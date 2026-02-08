package com.example.demo;

import java.util.List;
import java.util.stream.Collectors;

public class JsonOutputFormatter implements OutputFormatter {

    @Override
    public void print(List<ScheduleBucket> schedule) {
        if (schedule.isEmpty()) {
            System.out.println("[]");
            return;
        }

        System.out.println("[");
        for (int i = 0; i < schedule.size(); i++) {
            System.out.print(formatBucket(schedule.get(i)));
            if (i < schedule.size() - 1) {
                System.out.println(",");
            } else {
                System.out.println();
            }
        }
        System.out.println("]");
    }

    private String formatBucket(ScheduleBucket bucket) {
        StringBuilder sb = new StringBuilder();
        sb.append("  {\n");
        sb.append("    \"hour\": ").append(bucket.hour()).append(",\n");
        sb.append("    \"totalAgents\": ").append(bucket.totalAgents()).append(",\n");
        sb.append("    \"allocations\": {");

        if (!bucket.allocations().isEmpty()) {
            sb.append("\n");
            String allocations = bucket.allocations().entrySet().stream()
                    .map(e -> String.format("      \"%s\": %d", escape(e.getKey()), e.getValue()))
                    .collect(Collectors.joining(",\n"));
            sb.append(allocations);
            sb.append("\n    ");
        }
        sb.append("}\n");
        sb.append("  }");
        return sb.toString();
    }

    private String escape(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}