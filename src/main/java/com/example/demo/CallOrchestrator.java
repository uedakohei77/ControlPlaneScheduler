package com.example.demo;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;

/** 
 * Creates the call agent assignment per hour based on the capacity and customer priority.
 */
public class CallOrchestrator {

    private Storage storage;
    private int capacity;

    public CallOrchestrator(Storage storage, int capacity) {
        this.storage = storage;
        this.capacity = capacity;
    }

    public ScheduleBucket calculateSchedule(int hour) {
        List<AllocationRequest> requests = storage.fetchInterMediateData(hour);
        if (requests.isEmpty()) {
            return new ScheduleBucket(
                hour,
                0,
                java.util.Collections.emptyMap(),
                java.util.Collections.emptyMap(),
                java.util.Collections.emptyMap());
        }

        Map<String, Integer> allocations = new LinkedHashMap<>();
        Map<String, Integer> priorityMap = new LinkedHashMap<>();
        if (capacity <= 0) {
            // Infinite capacity
            int totalAgents = 0;
            requests.sort(AllocationRequest::compareTo);
            for (AllocationRequest request : requests) {
                allocations.merge(request.customer(), request.agents(), Integer::sum);
                priorityMap.put(request.customer(), request.priority());
                totalAgents += request.agents();
            }
            return new ScheduleBucket(hour, totalAgents, allocations, allocations, priorityMap);
        }
        // 1. Group all requests for this hour by priority
        Map<Integer, List<AllocationRequest>> priorityGroups = new TreeMap<>();
        Map<String, Integer> demands = new LinkedHashMap<>();
        for (AllocationRequest request : requests) {
            priorityGroups.computeIfAbsent(request.priority(), k -> new ArrayList<>()).add(request);
            demands.merge(request.customer(), request.agents(), Integer::sum);
            priorityMap.put(request.customer(), request.priority());
        }

        int remainingCapacty = capacity;
        int totalAllocated = 0;


        // 2. Iterate through priorities in order. 
        for (int priority : priorityGroups.keySet()) {
            List<AllocationRequest> tierRequests = priorityGroups.get(priority);
            int tierDemand = tierRequests.stream().mapToInt(AllocationRequest::agents).sum();

            if (tierDemand <= remainingCapacty) {
                // Full allocation for this tier.
                for (AllocationRequest request : tierRequests) {
                    allocations.merge(request.customer(), request.agents(), Integer::sum);
                }
                remainingCapacty -= tierDemand;
                totalAllocated += tierDemand;
            } else {
                // Return if there is no capacity left.
                if (remainingCapacty <= 0) {
                    break;
                }
                double ratio = (double) remainingCapacty / tierDemand;
                int allocatedInTier = 0;
                Map<AllocationRequest, Double> roundingLoss = new LinkedHashMap<>();
                // Pass 1: Floor distribution
                for (AllocationRequest request : tierRequests) {
                    double exactShare = request.agents() * ratio;
                    int flooredShare = (int) Math.floor(exactShare);
                    allocations.merge(request.customer(), flooredShare, Integer::sum);
                    allocatedInTier += flooredShare;
                    roundingLoss.put(request, exactShare - flooredShare);
                }
                // Pass 2: Distribute leftovers based on who lost the most value.
                // Allocate for largest remaining first.
                int leftovers = remainingCapacty - allocatedInTier;
                tierRequests.sort((a, b) -> Double.compare(roundingLoss.get(b), roundingLoss.get(a)));

                for (int i = 0; i < leftovers; i++) {
                    // Using modulo is safe if leftovers > tierRequests.size()
                    AllocationRequest request = tierRequests.get(i % tierRequests.size());
                    allocations.merge(request.customer(), 1, Integer::sum);
                }

                remainingCapacty = 0;
                totalAllocated = capacity;
                break;
            }
        }
        return new ScheduleBucket(hour, totalAllocated, allocations, demands, priorityMap);
    }
}
