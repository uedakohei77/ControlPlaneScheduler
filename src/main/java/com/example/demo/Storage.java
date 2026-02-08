package com.example.demo;

import java.util.List;

public interface Storage {
    void storeIntermediateData(int index, List<AllocationRequest> requests);
    List<AllocationRequest> fetchInterMediateData(int index);
    void storeSchedule(List<ScheduleBucket> schedule);
    List<ScheduleBucket> fetchSchedule();
    void cleanupIntermediateFiles();
}