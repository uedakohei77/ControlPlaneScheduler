package com.example.demo;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonOutputFormatterTest {

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private JsonOutputFormatter formatter;

    @BeforeEach
    void setUp() {
        System.setOut(new PrintStream(outContent));
        formatter = new JsonOutputFormatter();
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    @Test
    void testPrint_EmptySchedule() {
        formatter.print(Collections.emptyList());
        assertEquals("[]" + System.lineSeparator(), outContent.toString());
    }

    @Test
    void testPrint_SingleBucket() {
        Map<String, Integer> allocations = new LinkedHashMap<>();
        allocations.put("CustomerA", 5);
        allocations.put("CustomerB", 10);
        ScheduleBucket bucket = new ScheduleBucket(10, 15, allocations, allocations);

        formatter.print(List.of(bucket));

        String expected = "[" + System.lineSeparator() +
                "  {\n" +
                "    \"hour\": 10,\n" +
                "    \"totalAgents\": 15,\n" +
                "    \"allocations\": {\n" +
                "      \"CustomerA\": 5,\n" +
                "      \"CustomerB\": 10\n" +
                "    }\n" +
                "  }" + System.lineSeparator() +
                "]" + System.lineSeparator();

        assertEquals(expected, outContent.toString());
    }

    @Test
    void testPrint_MultipleBuckets() {
        ScheduleBucket bucket1 = new ScheduleBucket(10, 0, Collections.emptyMap(), Collections.emptyMap());
        
        Map<String, Integer> allocations2 = new LinkedHashMap<>();
        allocations2.put("CustomerC", 2);
        ScheduleBucket bucket2 = new ScheduleBucket(11, 2, allocations2, allocations2);

        formatter.print(List.of(bucket1, bucket2));

        String expected = "[" + System.lineSeparator() +
                "  {\n" +
                "    \"hour\": 10,\n" +
                "    \"totalAgents\": 0,\n" +
                "    \"allocations\": {}\n" +
                "  }," + System.lineSeparator() +
                "  {\n" +
                "    \"hour\": 11,\n" +
                "    \"totalAgents\": 2,\n" +
                "    \"allocations\": {\n" +
                "      \"CustomerC\": 2\n" +
                "    }\n" +
                "  }" + System.lineSeparator() +
                "]" + System.lineSeparator();

        assertEquals(expected, outContent.toString());
    }

    @Test
    void testPrint_Escaping() {
        Map<String, Integer> allocations = new LinkedHashMap<>();
        allocations.put("Cust\"omer\\X", 1);
        ScheduleBucket bucket = new ScheduleBucket(12, 1, allocations, allocations);

        formatter.print(List.of(bucket));

        String expected = "[" + System.lineSeparator() +
                "  {\n" +
                "    \"hour\": 12,\n" +
                "    \"totalAgents\": 1,\n" +
                "    \"allocations\": {\n" +
                "      \"Cust\\\"omer\\\\X\": 1\n" +
                "    }\n" +
                "  }" + System.lineSeparator() +
                "]" + System.lineSeparator();

        assertEquals(expected, outContent.toString());
    }

    @Test
    void testPrint_ThrottledBucket() {
        Map<String, Integer> allocations = new LinkedHashMap<>();
        allocations.put("CustomerA", 5);
        Map<String, Integer> demands = new LinkedHashMap<>();
        demands.put("CustomerA", 10);
        ScheduleBucket bucket = new ScheduleBucket(10, 5, allocations, demands);

        formatter.print(List.of(bucket));

        String expected = "[" + System.lineSeparator() +
                "  {\n" +
                "    \"hour\": 10,\n" +
                "    \"totalAgents\": 5,\n" +
                "    \"allocations\": {\n" +
                "      \"CustomerA\": 5\n" +
                "    },\n" +
                "    \"demands\": {\n" +
                "      \"CustomerA\": 10\n" +
                "    }\n" +
                "  }" + System.lineSeparator() +
                "]" + System.lineSeparator();

        assertEquals(expected, outContent.toString());
    }
}
