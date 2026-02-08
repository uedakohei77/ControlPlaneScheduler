package com.example.demo;

import com.google.common.collect.ImmutableMap;
import java.util.Map;

/**
 * Represents the final calculated schedule for a specific hour.
 */
public record ScheduleBucket(
    int hour,
    int totalAgents,
    Map<String, Integer> allocations // Customer -> Number of agents
) implements Comparable<ScheduleBucket> {
    public ScheduleBucket {
        allocations = ImmutableMap.copyOf(allocations);
    }

    public boolean isEmpty() {
        return totalAgents == 0;
    }

    @Override
    public int compareTo(ScheduleBucket other) {
        return Integer.compare(this.hour, other.hour);
    }
}
