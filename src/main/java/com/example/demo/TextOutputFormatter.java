package com.example.demo;

import java.util.List;
import java.util.stream.Collectors;

public class TextOutputFormatter implements OutputFormatter{

    private int capacity = 0;

    @Override
    public void print(List<ScheduleBucket> bucket) {
        if (bucket.isEmpty()) {
            System.out.println("No schedule available.");
            return;
        }
        for (ScheduleBucket b : bucket) {
            printBucket(b);
        }
    }

    @Override
    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    private void printBucket(ScheduleBucket bucket) {
        String timeStr = String.format("%02d:00", bucket.hour());
        if (bucket.totalAgents() == 0) {
            System.out.println(timeStr + " : total=0; none");
        } else {
            String details = bucket.allocations().entrySet().stream()
                .map(e -> String.format("%s=%d", e.getKey(), e.getValue()))
                .collect(Collectors.joining(", "));
            System.out.println(timeStr + " : total=" + bucket.totalAgents() + "; " + details);
        }
    }
}
