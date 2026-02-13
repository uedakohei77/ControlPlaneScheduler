package com.example.demo;

import com.example.demo.Constants.OutputFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Random;

@EnabledIfSystemProperty(named = "benchmark", matches = "true")
class ControlPlaneSchedulerBenchmarkTest {

    @TempDir
    Path tempDir;

    @Test
    void testBenchmark_InMemory() throws IOException {
        System.out.println("Generating CSV for In-Memory Benchmark...");
        Path inputCsv = generateLargeCsv("benchmark_memory.csv", 10_000_000);
        
        System.out.println("=== Benchmark: In-Memory Storage ===");
        Storage storage = new InMemoryStorage();
        // verbose = true to show timing logs
        ControlPlaneScheduler scheduler = new ControlPlaneScheduler(
            inputCsv.toString(), 1.0f, OutputFormat.TEXT, 0, storage, true, true
        );
        scheduler.run();
        System.out.println("====================================");
    }

    @Test
    void testBenchmark_FileSystem() throws IOException {
        System.out.println("Generating CSV for Filesystem Benchmark...");
        Path inputCsv = generateLargeCsv("benchmark_fs.csv", 10_000_000);
        
        System.out.println("=== Benchmark: Filesystem Storage ===");
        // Use PersistentStorage with temp dir to avoid polluting project root
        Storage storage = new PersistentStorage(LocalDate.now(), tempDir.toString());
        ControlPlaneScheduler scheduler = new ControlPlaneScheduler(
            inputCsv.toString(), 1.0f, OutputFormat.TEXT, 0, storage, true, true
        );
        scheduler.run();
        System.out.println("=====================================");
    }

    private Path generateLargeCsv(String filename, int rows) throws IOException {
        Path file = tempDir.resolve(filename);
        try (BufferedWriter writer = Files.newBufferedWriter(file)) {
            writer.write("CustomerName,NumberOfCalls,AverageCallDurationSeconds,Priority,StartTimePT,EndTimePT\n");
            Random rand = new Random();
            for (int i = 0; i < rows; i++) {
                String customer = "Customer" + (i % 100); 
                int calls = rand.nextInt(100) + 1;
                int duration = 300;
                int priority = (i % 3) + 1;
                int startHour = rand.nextInt(12); 
                int endHour = rand.nextInt(23);
                while (endHour <= startHour) {
                    endHour = rand.nextInt(23);
                }
                String start = String.format("%02d:00", startHour);
                String end = String.format("%02d:00", endHour);
                
                writer.write(String.format("%s,%d,%d,%d,%s,%s\n", 
                    customer, calls, duration, priority, start, end));
            }
        }
        return file;
    }
}