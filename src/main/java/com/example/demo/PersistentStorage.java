package com.example.demo;

import com.google.common.collect.ImmutableList;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The storage system which stores the call requests.
 * 
 * It stores and fetches the call requests for both intermediate and final stages.
 */
public class PersistentStorage implements Storage {

    private String schedulePath = "";
    private AtomicInteger intermediateFileIndex = new AtomicInteger(0);
    private Map<Integer, List<String>> intermediateFileMap = new HashMap<>();
    private LocalDate date;
    private final String outputDir;

    public PersistentStorage(LocalDate date) {
        this(date, ".");
    }

    public PersistentStorage(LocalDate date, String outputDir) {
        this.date = date;
        this.outputDir = outputDir;
    }

    public void storeIntermediateData(int index, List<AllocationRequest> requests) {
        Path path = Paths.get(outputDir, getUniqueFileString(Constants.INTERMEDIATE_FILE_PREFIX));
        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(path)))) {
            for (AllocationRequest entry : requests) {
                out.writeUTF(entry.customer());
                out.writeInt(entry.agents());
                out.writeInt(entry.priority());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        intermediateFileMap.putIfAbsent(index, new LinkedList<>());
        intermediateFileMap.get(index).add(path.toString());
    }

    public List<AllocationRequest> fetchInterMediateData(int index) {
        List<AllocationRequest> request = new LinkedList<>();
        if (!intermediateFileMap.containsKey(index)) {
            return request;
        }
        for (String file : intermediateFileMap.get(index)) {
            Path path = Paths.get(file);
            try (DataInputStream in = new DataInputStream(new BufferedInputStream(Files.newInputStream(path)))) {
                while (in.available() > 0) {
                    String customer = in.readUTF();
                    int agents = in.readInt();
                    int priority = in.readInt();
                    request.add(new AllocationRequest(customer, agents, priority));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return request;
    }

    public void storeSchedule(List<ScheduleBucket> schedule) {
        Path path = Paths.get(outputDir, getUniqueFileString(Constants.SCHEDULE_FILE_PREFIX));
        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(path)))) {
            this.schedulePath = path.toString();
            out.writeInt(schedule.size());
            for (ScheduleBucket entry : schedule) {
                out.writeInt(entry.hour());
                out.writeInt(entry.totalAgents());
                out.writeInt(entry.allocations().size());
                for (Map.Entry<String, Integer> allocation : entry.allocations().entrySet()) {
                    out.writeUTF(allocation.getKey());
                    out.writeInt(allocation.getValue());
                }
                for (Map.Entry<String, Integer> demand : entry.demands().entrySet()) {
                    out.writeUTF(demand.getKey());
                    out.writeInt(demand.getValue());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<ScheduleBucket> fetchSchedule() {
        ImmutableList.Builder<ScheduleBucket> scheduleBuilder = ImmutableList.builder();
        if (schedulePath.isEmpty()) {
            return scheduleBuilder.build();
        }
        Path path = Paths.get(schedulePath);
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(Files.newInputStream(path)))) {
            int count = in.readInt();
            for (int i = 0; i < count; i++) {
                int hour = in.readInt();
                int totalAgents = in.readInt();
                int allocationCount = in.readInt();
                Map<String, Integer> allocations = new LinkedHashMap<>();
                for (int j = 0; j < allocationCount; j++) {
                    String customer = in.readUTF();
                    int agents = in.readInt();
                    allocations.put(customer, agents);
                }
                Map<String, Integer> demands = new LinkedHashMap<>();
                for (int k = 0; k < allocationCount; k++) {
                    String customer = in.readUTF();
                    int agents = in.readInt();
                    demands.put(customer, agents);
                }
                scheduleBuilder.add(new ScheduleBucket(hour, totalAgents, allocations, demands));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return scheduleBuilder.build();
    }

    public void cleanupIntermediateFiles() {
        for (List<String> files : intermediateFileMap.values()) {
            for (String file : files) {
                Path path = Paths.get(file);
                try {
                    Files.deleteIfExists(path);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private String getUniqueFileString(String prefix) {
        return
            prefix + "_"
            + date.toString() + "_"
            + Integer.toString(intermediateFileIndex.getAndIncrement());
    }
}
