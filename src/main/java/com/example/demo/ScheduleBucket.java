package com.example.demo;

import com.google.common.collect.ImmutableMap;
import java.util.Map;

/**
 * Represents the final calculated schedule for a specific hour.
 */
public record ScheduleBucket(
    int hour,
    int totalAgents,
    Map<String, Integer> allocations, // Customer -> Number of agents allocated
    Map<String, Integer> demands,
    Map<String, Integer> priorityMap
) implements Comparable<ScheduleBucket> {
    public ScheduleBucket {
        allocations = ImmutableMap.copyOf(allocations);
        demands = ImmutableMap.copyOf(demands);
        priorityMap = ImmutableMap.copyOf(priorityMap);
    }

    public String getHourFormatted() {
        return String.format("%02d:00", hour);
    }

    public boolean isEmpty() {
        return totalAgents == 0;
    }

    public int totalAllocated() {
        return allocations.values().stream().mapToInt(Integer::intValue).sum();
    }

    public boolean isThrottled(String customer) {
        return demands.getOrDefault(customer, 0) > allocations.getOrDefault(customer, 0);
    }

    public boolean isAnyThrottled() {
        return demands.keySet().stream().anyMatch(this::isThrottled);
    }

    @Override
    public int compareTo(ScheduleBucket other) {
        return Integer.compare(this.hour, other.hour);
    }
}
