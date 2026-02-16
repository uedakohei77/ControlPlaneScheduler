package com.example.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UiOutputFormatterTest {

    private UiOutputFormatter formatter;

    @BeforeEach
    void setUp() {
        formatter = new UiOutputFormatter();
    }

    @Test
    void testGenerateHtml_EmptySchedule() {
        String html = formatter.generateHtml(Collections.emptyList());
        // Verify basic structure exists even if empty
        assertEquals(expectedHtmlHeader(0) + "</div></body></html>", html);
    }

    @Test
    void testGenerateHtml_ComplexSchedule() {
        // Setup Data
        // Hour 10: Healthy (Demand 10, Alloc 10)
        Map<String, Integer> alloc10 = new LinkedHashMap<>();
        alloc10.put("CustA", 10);
        Map<String, Integer> demand10 = new LinkedHashMap<>();
        demand10.put("CustA", 10);
        Map<String, Integer> priorityMap = new LinkedHashMap<>();
        priorityMap.put("CustA", 1);
        ScheduleBucket bucket10 = new ScheduleBucket(10, 10, alloc10, demand10, priorityMap);

        // Hour 11: Throttled (Demand 20, Alloc 10)
        Map<String, Integer> alloc11 = new LinkedHashMap<>();
        alloc11.put("CustB", 10);
        Map<String, Integer> demand11 = new LinkedHashMap<>();
        demand11.put("CustB", 20);
        Map<String, Integer> priorityMap11 = new LinkedHashMap<>();
        priorityMap11.put("CustB", 2);
        ScheduleBucket bucket11 = new ScheduleBucket(11, 10, alloc11, demand11, priorityMap11);

        formatter.setCapacity(50);
        String html = formatter.generateHtml(List.of(bucket10, bucket11));

        // Construct Expected Golden String
        StringBuilder expected = new StringBuilder();
        expected.append(expectedHtmlHeader(50));

        // Bucket 10
        expected.append("<details class='card'><summary>");
        expected.append("<div><strong>10:00 PT</strong></div>");
        expected.append("<div style='color: #64748B'>Total Allocation: 10</div>");
        expected.append("<div class='badge badge-ok'>Healthy</div>");
        expected.append("</summary><div class='table-container'><table>");
        expected.append("<tr><th>Customer</th><th>Priority</th><th>Required</th><th>Allocated</th><th>Gap</th></tr>");
        expected.append("<tr><td>CustA</td><td>1</td><td>10</td><td>10</td><td class=''>0</td></tr>");
        expected.append("</table></div></details>");

        // Bucket 11
        expected.append("<details class='card'><summary>");
        expected.append("<div><strong>11:00 PT</strong></div>");
        expected.append("<div style='color: #64748B'>Total Allocation: 10</div>");
        expected.append("<div class='badge badge-warn'>Demand Exceeded</div>");
        expected.append("</summary><div class='table-container'><table>");
        expected.append("<tr><th>Customer</th><th>Priority</th><th>Required</th><th>Allocated</th><th>Gap</th></tr>");
        expected.append("<tr><td>CustB</td><td>2</td><td>20</td><td>10</td><td class='unmet'>-10</td></tr>");
        expected.append("</table></div></details>");

        expected.append("</div></body></html>");

        assertEquals(expected.toString(), html);
    }

    private String expectedHtmlHeader(int capacity) {
        return "<!DOCTYPE html><html><head><title>Control Plane Scheduler</title>" +
               "<style>body { font-family: 'Inter', sans-serif; background: #F8FAFC; color: #1E293B; padding: 40px; }" +
               ".container { max-width: 1000px; margin: 0 auto; }h1 { color: #40A8EA; font-weight: 800; }" +
               ".card { background: white; border: 1px solid #E2E8F0; border-radius: 12px; margin-bottom: 12px; transition: 0.2s; }" +
               ".card:hover { box-shadow: 0 10px 15px -3px rgba(0,0,0,0.1); }summary { padding: 20px; cursor: pointer; display: flex; justify-content: space-between; align-items: center; list-style: none; }" +
               ".badge { padding: 4px 12px; border-radius: 99px; font-size: 12px; font-weight: 600; }.badge-ok { background: #DCFCE7; color: #0F9268; }" +
               ".badge-warn { background: #FEE2E2; color: #EF4444; }.table-container { padding: 0 20px 20px; border-top: 1px solid #F1F5F9; }" +
               "table { width: 100%; border-collapse: collapse; margin-top: 15px; }th { text-align: left; font-size: 13px; color: #64748B; border-bottom: 2px solid #F1F5F9; padding-bottom: 8px; }" +
               "td { padding: 12px 0; border-bottom: 1px solid #F8FAFC; font-size: 14px; }.unmet { color: #EF4444; font-weight: bold; }</style></head><body><div class='container'>" +
               "<h1>Agent Allocation Dashboard</h1><p>Capacity Constraint: <strong>" + capacity + " Agents</strong></p>";
    }
}
