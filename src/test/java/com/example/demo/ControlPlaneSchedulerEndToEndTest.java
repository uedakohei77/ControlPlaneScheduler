package com.example.demo;

import com.example.demo.Constants.OutputFormat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ControlPlaneSchedulerEndToEndTest {

    @TempDir
    Path tempDir;

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @BeforeEach
    void setUp() {
        System.setOut(new PrintStream(outContent));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    @Test
    void testEndToEnd_Scenario1_LowUtilization_InfiniteCapacity() throws IOException {
        // 1. Prepare Input Data
        Path inputCsv = tempDir.resolve("input_scenario1.csv");
        String csvContent = "CustomerName,NumberOfCalls,AverageCallDurationSeconds,Priority,StartTimePT,EndTimePT\n" +
                            "CustomerA,100,300,1,10:00 AM,11:00 AM\n" +
                            "CustomerB,200,300,2,10:00 AM,11:00 AM";
        Files.writeString(inputCsv, csvContent);

        // 2. Prepare Golden File (Expected Output)
        // Utilization 0.8, Capacity 0 (Infinite)
        // A: 100*300/3600 = 8.33 -> *0.8 = 6.66 -> 7 agents
        // B: 200*300/3600 = 16.66 -> *0.8 = 13.33 -> 14 agents
        Path goldenFile = tempDir.resolve("golden_scenario1.json");
        String activeBucket = "  {\n" +
                "    \"hour\": 10,\n" +
                "    \"totalAgents\": 21,\n" +
                "    \"allocations\": {\n" +
                "      \"CustomerA\": 7,\n" +
                "      \"CustomerB\": 14\n" +
                "    }\n" +
                "  }";
        String expectedJson = generateExpectedJson(10, activeBucket);
        Files.writeString(goldenFile, expectedJson);

        // 3. Run Scheduler
        PersistentStorage storage = new PersistentStorage(LocalDate.now(), tempDir.toString());
        ControlPlaneScheduler scheduler = new ControlPlaneScheduler(inputCsv.toString(), 0.8f, OutputFormat.JSON, 0, storage);
        scheduler.run();

        // 4. Verify Output
        String actualOutput = outContent.toString();
        String jsonOutput = extractJson(actualOutput);
        
        assertEquals(normalize(Files.readString(goldenFile)), normalize(jsonOutput));
    }

    @Test
    void testEndToEnd_Scenario2_FullUtilization_FiniteCapacity() throws IOException {
        // 1. Prepare Input Data
        Path inputCsv = tempDir.resolve("input_scenario2.csv");
        String csvContent = "CustomerName,NumberOfCalls,AverageCallDurationSeconds,Priority,StartTimePT,EndTimePT\n" +
                            "CustomerA,100,300,1,10:00 AM,11:00 AM\n" +
                            "CustomerB,200,300,2,10:00 AM,11:00 AM";
        Files.writeString(inputCsv, csvContent);

        // 2. Prepare Golden File (Expected Output)
        // Utilization 1.0, Capacity 15
        // A: 8.33 -> 9 agents.
        // B: 16.66 -> 17 agents.
        // Total Demand 26. Capacity 15.
        // P1 (A) gets 9. Remaining 6.
        // P2 (B) gets 6.
        Path goldenFile = tempDir.resolve("golden_scenario2.json");
        String activeBucket = "  {\n" +
                "    \"hour\": 10,\n" +
                "    \"totalAgents\": 15,\n" +
                "    \"allocations\": {\n" +
                "      \"CustomerA\": 9,\n" +
                "      \"CustomerB\": 6\n" +
                "    },\n" +
                "    \"demands\": {\n" +
                "      \"CustomerA\": 9,\n" +
                "      \"CustomerB\": 17\n" +
                "    }\n" +
                "  }";
        String expectedJson = generateExpectedJson(10, activeBucket);
        Files.writeString(goldenFile, expectedJson);

        // 3. Run Scheduler
        PersistentStorage storage = new PersistentStorage(LocalDate.now(), tempDir.toString());
        ControlPlaneScheduler scheduler = new ControlPlaneScheduler(inputCsv.toString(), 1.0f, OutputFormat.JSON, 15, storage);
        scheduler.run();

        // 4. Verify Output
        String actualOutput = outContent.toString();
        String jsonOutput = extractJson(actualOutput);

        assertEquals(normalize(Files.readString(goldenFile)), normalize(jsonOutput));
    }

    private String extractJson(String output) {
        int start = output.indexOf("[");
        int end = output.lastIndexOf("]");
        if (start >= 0 && end >= 0) {
            return output.substring(start, end + 1);
        }
        return "";
    }

    private String normalize(String s) {
        return s.replace("\r\n", "\n").trim();
    }

    private String generateExpectedJson(int activeHour, String activeBucketContent) {
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        for (int i = 0; i < 24; i++) {
            if (i == activeHour) {
                sb.append(activeBucketContent);
            } else {
                sb.append("  {\n");
                sb.append("    \"hour\": ").append(i).append(",\n");
                sb.append("    \"totalAgents\": 0,\n");
                sb.append("    \"allocations\": {}\n");
                sb.append("  }");
            }
            if (i < 23) {
                sb.append(",\n");
            } else {
                sb.append("\n");
            }
        }
        sb.append("]");
        return sb.toString();
    }
}