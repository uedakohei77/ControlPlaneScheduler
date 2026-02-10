package com.example.demo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryStorage implements Storage {

    private final Map<Integer, List<AllocationRequest>> intermediateData = new ConcurrentHashMap<>();
    private List<ScheduleBucket> schedule = Collections.emptyList();

    @Override
    public void storeIntermediateData(int index, List<AllocationRequest> requests) {
        intermediateData.computeIfAbsent(index, k -> Collections.synchronizedList(new ArrayList<>()))
                .addAll(requests);
    }

    @Override
    public List<AllocationRequest> fetchInterMediateData(int index) {
        List<AllocationRequest> requests = intermediateData.get(index);
        if (requests == null) {
            return Collections.emptyList();
        }
        synchronized (requests) {
            return new ArrayList<>(requests);
        }
    }

    @Override
    public void storeSchedule(List<ScheduleBucket> schedule) {
        this.schedule = new ArrayList<>(schedule);
    }

    @Override
    public List<ScheduleBucket> fetchSchedule() {
        return new ArrayList<>(schedule);
    }

    @Override
    public void cleanupIntermediateFiles() {
        intermediateData.clear();
    }
}