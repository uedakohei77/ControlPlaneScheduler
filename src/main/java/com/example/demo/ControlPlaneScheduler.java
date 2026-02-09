package com.example.demo;

import com.example.demo.Constants.OutputFormat;
import com.google.common.collect.ImmutableList;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.NamedCsvRecord;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ControlPlaneScheduler {
    private static final int BATCH_SIZE = 100;
    private static final int INITIAL_THREAD_POOL_SIZE = 4;

    private final String inputFile;
    private final float utilization;
    private final OutputFormat outputFormat;
    private final int capacity;
    private final Storage storage;

    public ControlPlaneScheduler(String inputFile, float utilization, OutputFormat outputFormat, int capacity) {
        this(inputFile, utilization, outputFormat, capacity, new PersistentStorage(LocalDate.now()));
    }

    public ControlPlaneScheduler(String inputFile, float utilization, OutputFormat outputFormat, int capacity, Storage storage) {
        this.inputFile = inputFile;
        this.utilization = utilization;
        this.outputFormat = outputFormat;
        this.capacity = capacity;
        this.storage = storage;
    }

    public void run() {
        // Check the previous calculation.
        List<ScheduleBucket> schedule = storage.fetchSchedule();
        if (schedule.isEmpty()) {
            Path path = Paths.get(inputFile);
            if (!Files.exists(path)) {
                System.err.println("Input file does not exist: " + inputFile);
                return;
            }

            schedule = new ArrayList<>();
            ExecutorService mapExecutor = Executors.newFixedThreadPool(INITIAL_THREAD_POOL_SIZE);

            // Step 1: Read CSV data from the input. And pass it to RequestProcessor.
            System.out.println("Processing file: " + inputFile);
            try (CsvReader<NamedCsvRecord> reader = CsvReader.builder().ofNamedCsvRecord(path)) {
                int count = 0;
                ImmutableList.Builder<NamedCsvRecord> builder = ImmutableList.builderWithExpectedSize(BATCH_SIZE);
                List<CompletableFuture<Void>> futures = new ArrayList<>();

                for (NamedCsvRecord record : reader) {
                    builder.add(record);
                    if (++count >= BATCH_SIZE) {
                        ImmutableList<NamedCsvRecord> batch = builder.build();
                        futures.add(CompletableFuture.runAsync(() -> { 
                            RequestProcessor processor = new RequestProcessor(storage, utilization);
                            processor.processRequest(batch);
                        }, mapExecutor));
                        builder = ImmutableList.builderWithExpectedSize(BATCH_SIZE);
                        count = 0;
                    }
                }
                if (count > 0) {
                    ImmutableList<NamedCsvRecord> batch = builder.build();
                        futures.add(CompletableFuture.runAsync(() -> { 
                            RequestProcessor processor = new RequestProcessor(storage, utilization);
                            processor.processRequest(batch);
                        }, mapExecutor));
                }

                // Wait for all async tasks to complete
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            } catch (Exception e) {
                System.out.println("Filed to read data.");
                e.printStackTrace();
            }
            mapExecutor.shutdown();

            // Step 2: Aggregate intermediate requests into ScheduleBucket per hour.
            ExecutorService reduceExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
            List<CompletableFuture<ScheduleBucket>> reduceFutures = new ArrayList<>();
            for (int h = 0; h < 24; h++) {
                final int hour = h;
                reduceFutures.add(CompletableFuture.supplyAsync(() -> {
                    CallOrchestrator callOrchestrator = new CallOrchestrator(storage, capacity);
                    return callOrchestrator.calculateSchedule(hour);
                }, reduceExecutor));
            }
            CompletableFuture.allOf(reduceFutures.toArray(new CompletableFuture[0])).join();
            for (CompletableFuture<ScheduleBucket> future : reduceFutures) {
                ScheduleBucket bucket = future.join();
                schedule.add(bucket);
            }
            Collections.sort(schedule);

            storage.storeSchedule(schedule);
            storage.cleanupIntermediateFiles();
            reduceExecutor.shutdown();
        }
        // Step 3: Show output 
        OutputFormatter formatter;
        switch (outputFormat) {
            case JSON:
                formatter = new JsonOutputFormatter();
                break;
            case UI:
                formatter = new UiOutputFormatter();
                break;
            default:
                formatter = new TextOutputFormatter();
                break;
        }
        formatter.setCapacity(capacity);
        formatter.print(schedule);
    }
}
