package com.example.demo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryStorageTest {

    private InMemoryStorage storage;

    @BeforeEach
    void setUp() {
        storage = new InMemoryStorage();
    }

    @Test
    void testStoreAndFetchIntermediateData() {
        int index = 1;
        List<AllocationRequest> requests = List.of(
            new AllocationRequest("CustomerA", 5, 1),
            new AllocationRequest("CustomerB", 10, 2)
        );

        storage.storeIntermediateData(index, requests);

        List<AllocationRequest> fetchedRequests = storage.fetchInterMediateData(index);

        assertNotNull(fetchedRequests);
        assertEquals(2, fetchedRequests.size());
        assertTrue(fetchedRequests.containsAll(requests));
    }

    @Test
    void testStoreAndFetchIntermediateDataMultipleBatches() {
        int index = 2;
        List<AllocationRequest> batch1 = List.of(new AllocationRequest("A", 1, 1));
        List<AllocationRequest> batch2 = List.of(new AllocationRequest("B", 2, 1));

        // Store multiple batches for the same index
        storage.storeIntermediateData(index, batch1);
        storage.storeIntermediateData(index, batch2);

        List<AllocationRequest> result = storage.fetchInterMediateData(index);
        assertEquals(2, result.size());
        assertTrue(result.containsAll(batch1));
        assertTrue(result.containsAll(batch2));
    }

    @Test
    void testFetchIntermediateData_NonExistentKey() {
        List<AllocationRequest> result = storage.fetchInterMediateData(999);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testStoreAndFetchSchedule() {
        Map<String, Integer> allocations = new HashMap<>();
        allocations.put("CustomerA", 5);
        ScheduleBucket bucket = new ScheduleBucket(10, 5, allocations, allocations);
        List<ScheduleBucket> schedule = new ArrayList<>();
        schedule.add(bucket);

        storage.storeSchedule(schedule);
        List<ScheduleBucket> fetchedSchedule = storage.fetchSchedule();

        assertNotNull(fetchedSchedule);
        assertEquals(1, fetchedSchedule.size());
        assertEquals(bucket, fetchedSchedule.get(0));
    }

    @Test
    void testCleanupIntermediateFiles() {
        int index = 1;
        List<AllocationRequest> requests = List.of(new AllocationRequest("A", 1, 1));
        storage.storeIntermediateData(index, requests);

        assertFalse(storage.fetchInterMediateData(index).isEmpty());

        storage.cleanupIntermediateFiles();

        assertTrue(storage.fetchInterMediateData(index).isEmpty());
    }
}