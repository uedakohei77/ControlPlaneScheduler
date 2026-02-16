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

class TextOutputFormatterTest {

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private TextOutputFormatter formatter;

    @BeforeEach
    void setUp() {
        System.setOut(new PrintStream(outContent));
        formatter = new TextOutputFormatter();
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    @Test
    void testPrint_EmptySchedule() {
        formatter.print(Collections.emptyList());
        assertEquals("No schedule available." + System.lineSeparator(), outContent.toString());
    }

    @Test
    void testPrint_SingleBucket_NoAgents() {
        ScheduleBucket bucket =
            new ScheduleBucket(
                9, 0, Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
        formatter.print(List.of(bucket));
        assertEquals("09:00 : total=0; none" + System.lineSeparator(), outContent.toString());
    }

    @Test
    void testPrint_SingleBucket_WithAgents() {
        Map<String, Integer> allocations = new LinkedHashMap<>();
        allocations.put("A", 1);
        allocations.put("B", 2);
        Map<String, Integer> priorityMap = new LinkedHashMap<>();
        priorityMap.put("A", 1);
        priorityMap.put("B", 2);
        ScheduleBucket bucket = new ScheduleBucket(10, 3, allocations, allocations, priorityMap);

        formatter.print(List.of(bucket));
        assertEquals("10:00 : total=3; A=1, B=2" + System.lineSeparator(), outContent.toString());
    }

    @Test
    void testPrint_MultipleBuckets() {
        ScheduleBucket bucket1 =
            new ScheduleBucket(
                9, 0, Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
        
        Map<String, Integer> allocations = new LinkedHashMap<>();
        allocations.put("A", 5);
        Map<String, Integer> priorityMap = new LinkedHashMap<>();
        priorityMap.put("A", 1);
        ScheduleBucket bucket2 = new ScheduleBucket(10, 5, allocations, allocations, priorityMap);

        formatter.print(List.of(bucket1, bucket2));
        
        String expected = "09:00 : total=0; none" + System.lineSeparator() +
                          "10:00 : total=5; A=5" + System.lineSeparator();
        
        assertEquals(expected, outContent.toString());
    }
}
