package com.example.demo;

/** 
 * Represents a single allocation requirement for one customer in one specific hour.
 */
public record AllocationRequest (String customer, int agents, int priority) implements Comparable<AllocationRequest>{
    @Override
    public int compareTo(AllocationRequest other) {
        return Integer.compare(this.priority, other.priority);
    }
}
