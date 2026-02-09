package com.example.demo;

import static com.example.demo.Constants.AVG_CALL_DURATION_SEC;
import static com.example.demo.Constants.CUSTOMER_COLUMN;
import static com.example.demo.Constants.END_TIME;
import static com.example.demo.Constants.NUM_CALLS;
import static com.example.demo.Constants.PRIORITY;
import static com.example.demo.Constants.START_TIME;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.google.common.collect.ImmutableList;
import de.siegmar.fastcsv.reader.NamedCsvRecord;

public class RequestProcessor {
    Storage storage;
    float utilization;

    public RequestProcessor(Storage storage, float utilization) {
        this.storage = storage;
        this.utilization = utilization;
    }

    public void processRequest(List<NamedCsvRecord> request) {
        // Hour -> (CustomerName -> AgentCount)
        Map<Integer, ImmutableList.Builder<AllocationRequest>> batchResult = new HashMap<>();
        for (NamedCsvRecord record : request) {
            String customer = record.getField(CUSTOMER_COLUMN);
            int totalCalls = Integer.parseInt(record.getField(NUM_CALLS).trim());
            int avgDuration = Integer.parseInt(record.getField(AVG_CALL_DURATION_SEC).trim());
            int priority = Integer.parseInt(record.getField(PRIORITY).trim());
            String startTime = record.getField(START_TIME);
            String endTime = record.getField(END_TIME);
            int startHour = parseTime(startTime);
            int endHour = parseTime(endTime);

            int activHours = endHour - startHour;
            if (activHours <= 0) {
                continue;
            }
            double callsPerHour = (double) totalCalls / activHours;
            int agents = (int) Math.ceil((callsPerHour * avgDuration) / 3600.0 * utilization);
            for (int hour = startHour; hour < endHour; hour++) {
                batchResult.putIfAbsent(hour, ImmutableList.builder());
                batchResult.get(hour).add(new AllocationRequest(customer, agents, priority));
            }
        }
        if (!batchResult.isEmpty()) {
            for (int hour : batchResult.keySet()) {
                storage.storeIntermediateData(hour, batchResult.get(hour).build());
            }
        }
    }

    private int parseTime(String time) {
        time = time.trim().toUpperCase();
        int hour = Integer.parseInt(time.split(":")[0].replaceAll("[^0-9]", ""));
        if (time.contains("PM") && hour < 12) hour += 12;
        if (time.contains("AM") && hour == 12) hour = 0;
        return hour;
    }
}
