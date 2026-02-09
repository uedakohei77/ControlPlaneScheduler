package com.example.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class CallOrchestratorTest {

    @Test
    void testCalculateSchedule_NoRequests() {
        Storage storage = mock(Storage.class);
        when(storage.fetchInterMediateData(10)).thenReturn(Collections.emptyList());
        
        CallOrchestrator orchestrator = new CallOrchestrator(storage, 100);
        ScheduleBucket bucket = orchestrator.calculateSchedule(10);
        
        assertTrue(bucket.isEmpty());
        assertEquals(0, bucket.totalAgents());
    }

    @Test
    void testCalculateSchedule_InfiniteCapacity() {
        Storage storage = mock(Storage.class);
        List<AllocationRequest> requests = new ArrayList<>(List.of(
            new AllocationRequest("A", 10, 1),
            new AllocationRequest("B", 20, 2)
        ));
        when(storage.fetchInterMediateData(10)).thenReturn(requests);

        CallOrchestrator orchestrator = new CallOrchestrator(storage, 0); // 0 = infinite
        ScheduleBucket bucket = orchestrator.calculateSchedule(10);

        assertEquals(30, bucket.totalAgents());
        assertEquals(10, bucket.allocations().get("A"));
        assertEquals(20, bucket.allocations().get("B"));
    }

    @Test
    void testCalculateSchedule_SufficientCapacity() {
        Storage storage = mock(Storage.class);
        List<AllocationRequest> requests = new ArrayList<>(List.of(
            new AllocationRequest("A", 10, 1),
            new AllocationRequest("B", 20, 2)
        ));
        when(storage.fetchInterMediateData(10)).thenReturn(requests);

        CallOrchestrator orchestrator = new CallOrchestrator(storage, 50);
        ScheduleBucket bucket = orchestrator.calculateSchedule(10);

        assertEquals(30, bucket.totalAgents());
        assertEquals(10, bucket.allocations().get("A"));
        assertEquals(20, bucket.allocations().get("B"));
    }

    @Test
    void testCalculateSchedule_PriorityEnforcement() {
        Storage storage = mock(Storage.class);
        // Priority 1 (High), Priority 2 (Low)
        // Total demand: 10 (P1) + 20 (P2) = 30. Capacity = 20.
        // P1 should get 10. Remaining 10. P2 needs 20, gets 10.
        List<AllocationRequest> requests = new ArrayList<>(List.of(
            new AllocationRequest("HighPrio", 10, 1),
            new AllocationRequest("LowPrio", 20, 2)
        ));
        when(storage.fetchInterMediateData(10)).thenReturn(requests);

        CallOrchestrator orchestrator = new CallOrchestrator(storage, 20);
        ScheduleBucket bucket = orchestrator.calculateSchedule(10);

        assertEquals(20, bucket.totalAgents());
        assertEquals(10, bucket.allocations().get("HighPrio"));
        assertEquals(10, bucket.allocations().get("LowPrio"));

        // Verify demands are tracked correctly
        assertEquals(10, bucket.demands().get("HighPrio"));
        assertEquals(20, bucket.demands().get("LowPrio"));
    }

    @Test
    void testCalculateSchedule_FairShareWithRounding() {
        Storage storage = mock(Storage.class);
        // Capacity 10.
        // A: 10, B: 11, C: 12. Total 33.
        // Ratio 10/33 ~= 0.303.
        // A: 3, B: 3, C: 3. Total 9. Leftover 1.
        // C is largest (12), so C gets +1 -> 4.
        List<AllocationRequest> requests = new ArrayList<>(List.of(
            new AllocationRequest("A", 10, 1),
            new AllocationRequest("B", 11, 1),
            new AllocationRequest("C", 12, 1)
        ));
        when(storage.fetchInterMediateData(10)).thenReturn(requests);

        CallOrchestrator orchestrator = new CallOrchestrator(storage, 10);
        ScheduleBucket bucket = orchestrator.calculateSchedule(10);

        assertEquals(10, bucket.totalAgents());
        assertEquals(3, bucket.allocations().get("A"));
        assertEquals(3, bucket.allocations().get("B"));
        assertEquals(4, bucket.allocations().get("C"));

        // Verify demands are tracked correctly
        assertEquals(10, bucket.demands().get("A"));
        assertEquals(11, bucket.demands().get("B"));
        assertEquals(12, bucket.demands().get("C"));
    }

    @Test
    void testCalculateSchedule_DuplicateCustomers() {
        Storage storage = mock(Storage.class);
        // Customer A has two requests: 10 and 20. Total 30.
        List<AllocationRequest> requests = new ArrayList<>(List.of(
            new AllocationRequest("A", 10, 1),
            new AllocationRequest("A", 20, 1)
        ));
        when(storage.fetchInterMediateData(10)).thenReturn(requests);

        CallOrchestrator orchestrator = new CallOrchestrator(storage, 0); // Infinite
        ScheduleBucket bucket = orchestrator.calculateSchedule(10);

        assertEquals(30, bucket.totalAgents());
        assertEquals(30, bucket.allocations().get("A"));
    }
}
