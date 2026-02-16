package com.example.demo;

import static com.example.demo.Constants.HTLM_FILE_NAME;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

public class UiOutputFormatter implements OutputFormatter {

    private static final String BASE_BLUE = "#40A8EA";
    private static final String BASE_GREEN = "#0F9268";

    private int capacityConstraint = 0;

    @Override
    public void print(List<ScheduleBucket> schedule) {
        if (schedule.isEmpty()) {
            System.out.println("No schedule available.");
            return;
        }
        String report = generateHtml(schedule);
        if (saveToFile(report, HTLM_FILE_NAME)) {
            File file = new File(HTLM_FILE_NAME);
            String fileUri = file.toURI().toString();

            System.out.println("\n" + "=".repeat(40));
            System.out.println("Schedule Generation Complete.");
            System.out.println("View dashboard: " + fileUri);
            System.out.println("=".repeat(40));
        }
    }

    @Override
    public void setCapacity(int capacity) {
        this.capacityConstraint = capacity;
    }


    String generateHtml(List<ScheduleBucket> schedule) {
        StringBuilder html = new StringBuilder();

        // 1. HTML Header & CSS 
        html.append("<!DOCTYPE html><html><head><title>Control Plane Scheduler</title>");
        html.append("<style>");
        html.append("body { font-family: 'Inter', sans-serif; background: #F8FAFC; color: #1E293B; padding: 40px; }");
        html.append(".container { max-width: 1000px; margin: 0 auto; }");
        html.append("h1 { color: " + BASE_BLUE + "; font-weight: 800; }");
        html.append(".card { background: white; border: 1px solid #E2E8F0; border-radius: 12px; margin-bottom: 12px; transition: 0.2s; }");
        html.append(".card:hover { box-shadow: 0 10px 15px -3px rgba(0,0,0,0.1); }");
        html.append("summary { padding: 20px; cursor: pointer; display: flex; justify-content: space-between; align-items: center; list-style: none; }");
        html.append(".badge { padding: 4px 12px; border-radius: 99px; font-size: 12px; font-weight: 600; }");
        html.append(".badge-ok { background: #DCFCE7; color: " + BASE_GREEN + "; }");
        html.append(".badge-warn { background: #FEE2E2; color: #EF4444; }");
        html.append(".table-container { padding: 0 20px 20px; border-top: 1px solid #F1F5F9; }");
        html.append("table { width: 100%; border-collapse: collapse; margin-top: 15px; }");
        html.append("th { text-align: left; font-size: 13px; color: #64748B; border-bottom: 2px solid #F1F5F9; padding-bottom: 8px; }");
        html.append("td { padding: 12px 0; border-bottom: 1px solid #F8FAFC; font-size: 14px; }");
        html.append(".unmet { color: #EF4444; font-weight: bold; }");
        html.append("</style></head><body><div class='container'>");

        // 2. Dashboard Header
        html.append("<h1>Agent Allocation Dashboard</h1>");
        html.append("<p>Capacity Constraint: <strong>" + capacityConstraint + " Agents</strong></p>");

        // 3. The 24-Hour Timeline
        for (ScheduleBucket bucket : schedule) {
            int totalAllocated = bucket.totalAllocated();
            boolean isThrottled = bucket.isAnyThrottled();
            String statusClass = isThrottled ? "badge-warn" : "badge-ok";
            String statusText = isThrottled ? "Demand Exceeded" : "Healthy";

            html.append("<details class='card'>");
            html.append("<summary>");
            html.append("<div><strong>" + bucket.getHourFormatted() + " PT</strong></div>");
            html.append("<div style='color: #64748B'>Total Allocation: " + totalAllocated + "</div>");
            html.append("<div class='badge " + statusClass + "'>" + statusText + "</div>");
            html.append("</summary>");

            // 4. Drill-down Table
            html.append("<div class='table-container'><table>");
            html.append("<tr><th>Customer</th><th>Priority</th><th>Required</th><th>Allocated</th><th>Gap</th></tr>");
            
            for (Map.Entry<String, Integer> entry : bucket.allocations().entrySet()) {
                int priority = bucket.priorityMap().getOrDefault(entry.getKey(), -1);
                int allocation = entry.getValue();
                int demand = bucket.demands().getOrDefault(entry.getKey(), 0);
                int gap = demand - allocation;
                String gapClass = gap > 0 ? "unmet" : "";

                html.append("<tr>");
                html.append("<td>" + entry.getKey() + "</td>");
                html.append("<td>" + priority + "</td>");
                html.append("<td>" + demand + "</td>");
                html.append("<td>" + allocation + "</td>");
                html.append("<td class='" + gapClass + "'>" + (gap > 0 ? "-" + gap : "0") + "</td>");
                html.append("</tr>");
            }
            html.append("</table></div></details>");
        }

        html.append("</div></body></html>");
        return html.toString();
    }

    private boolean saveToFile(String html, String filename) {
        try {
            Path path = Paths.get(filename);
            Files.deleteIfExists(path);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        try (FileWriter writer = new FileWriter(filename)) {
            writer.write(html);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
