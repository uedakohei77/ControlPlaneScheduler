package com.example.demo;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class PersistentStorageTest {

    private PersistentStorage storage;

    @BeforeEach
    void setUp() {
        storage = new PersistentStorage(LocalDate.now());
    }

    @AfterEach
    void tearDown() throws Exception {
        // Clean up intermediate files created during tests
        storage.cleanupIntermediateFiles();

        // Clean up the schedule file using reflection since the field is private
        Field schedulePathField = PersistentStorage.class.getDeclaredField("schedulePath");
        schedulePathField.setAccessible(true);
        String schedulePath = (String) schedulePathField.get(storage);

        if (schedulePath != null && !schedulePath.isEmpty()) {
            Path path = Paths.get(schedulePath);
            Files.deleteIfExists(path);
        }
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

        // Store multiple batches for the same index (simulating multiple files)
        storage.storeIntermediateData(index, batch1);
        storage.storeIntermediateData(index, batch2);

        List<AllocationRequest> result = storage.fetchInterMediateData(index);
        assertEquals(2, result.size());
        assertTrue(result.containsAll(batch1));
        assertTrue(result.containsAll(batch2));
    }

    @Test
    void testStoreAndFetchSchedule() {
        // Create a sample schedule
        Map<String, Integer> allocations = new HashMap<>();
        allocations.put("CustomerA", 5);
        allocations.put("CustomerB", 10);

        ScheduleBucket bucket = new ScheduleBucket(10, 15, allocations, allocations);
        List<ScheduleBucket> schedule = new ArrayList<>();
        schedule.add(bucket);

        // Store and Fetch
        storage.storeSchedule(schedule);
        List<ScheduleBucket> fetchedSchedule = storage.fetchSchedule();

        // Verify
        assertNotNull(fetchedSchedule);
        assertEquals(1, fetchedSchedule.size());
        assertEquals(bucket, fetchedSchedule.get(0));
    }

    @Test
    void testFetchScheduleWithCorruptedFile() throws Exception {
        // 1. Store a valid schedule to set the internal schedulePath
        Map<String, Integer> allocations = new HashMap<>();
        allocations.put("CustomerA", 5);
        ScheduleBucket bucket = new ScheduleBucket(10, 5, allocations, allocations);
        List<ScheduleBucket> schedule = new ArrayList<>();
        schedule.add(bucket);

        storage.storeSchedule(schedule);

        // 2. Corrupt the file by writing non-integer data
        Field schedulePathField = PersistentStorage.class.getDeclaredField("schedulePath");
        schedulePathField.setAccessible(true);
        String pathStr = (String) schedulePathField.get(storage);
        Path path = Paths.get(pathStr);

        Files.write(path, "NotAnInteger".getBytes());

        // 3. Fetch - should handle exception internally and return empty map
        List<ScheduleBucket> result = storage.fetchSchedule();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testFetchIntermediateDataWithMissingFile() throws Exception {
        // 1. Inject a non-existent file path into the map using reflection
        Field mapField = PersistentStorage.class.getDeclaredField("intermediateFileMap");
        mapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<Integer, List<String>> map = (Map<Integer, List<String>>) mapField.get(storage);

        int index = 99;
        List<String> files = new ArrayList<>();
        files.add("non_existent_file.dat");
        map.put(index, files);

        // 2. Fetch - should handle exception internally (NoSuchFileException) and return empty list
        List<AllocationRequest> result = storage.fetchInterMediateData(index);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testFetchIntermediateData_NonExistentKey() {
        List<AllocationRequest> result = storage.fetchInterMediateData(999);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}