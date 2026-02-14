package com.example.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.NamedCsvRecord;
import java.io.StringReader;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RequestProcessorTest {

    private Storage storage;
    private RequestProcessor processor;

    @BeforeEach
    void setUp() {
        storage = mock(Storage.class);
        processor = new RequestProcessor(storage, 1.0f);
    }

    @Test
    void testProcessRequest_SingleHour() {
        // 10 calls, 1 hour (10-11), 360s duration -> 10 * 360 / 3600 = 1 agent
        NamedCsvRecord record = createRecord("CustomerA", "10", "360", "1", "10 AM", "11 AM");

        processor.processRequest(Collections.singletonList(record));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<AllocationRequest>> captor = ArgumentCaptor.forClass(List.class);
        verify(storage).storeIntermediateData(eq(10), captor.capture());

        List<AllocationRequest> requests = captor.getValue();
        assertEquals(1, requests.size());
        assertEquals("CustomerA", requests.get(0).customer());
        assertEquals(1, requests.get(0).agents());
        assertEquals(1, requests.get(0).priority());
    }

    @Test
    void testProcessRequest_MultipleHours() {
        // 20 calls, 2 hours (10-12), 360s duration -> 10 calls/hr -> 1 agent
        NamedCsvRecord record = createRecord("CustomerB", "20", "360", "2", "10 AM", "12 PM");

        processor.processRequest(Collections.singletonList(record));

        // Should be called for 10 and 11 (exclusive of 12)
        verify(storage).storeIntermediateData(eq(10), anyList());
        verify(storage).storeIntermediateData(eq(11), anyList());
        verify(storage, times(2)).storeIntermediateData(anyInt(), anyList());
    }

    @Test
    void testProcessRequest_TimeParsingEdgeCases() {
        // 13 calls, 13 hours (0-13), 3600s duration -> 1 call/hr -> 1 agent
        NamedCsvRecord record = createRecord("CustomerC", "13", "3600", "1", "12 AM", "1 PM");

        processor.processRequest(Collections.singletonList(record));

        // Should cover 0 (12 AM) to 12 (1 PM exclusive)
        verify(storage).storeIntermediateData(eq(0), anyList());
        verify(storage).storeIntermediateData(eq(12), anyList());
    }

    @Test
    void testProcessRequest_MultipleRecords_MixedTimes() {
        // Rec1: 10 calls, 1 hr, 360s -> 1 agent
        NamedCsvRecord record1 = createRecord("CustomerA", "10", "360", "1", "10 AM", "11 AM");
        // Rec2: 20 calls, 1 hr, 360s -> 2 agents
        NamedCsvRecord record2 = createRecord("CustomerB", "20", "360", "2", "11 AM", "12 PM");
        // Rec3: 30 calls, 2 hrs, 360s -> 15 calls/hr -> ceil(1.5) = 2 agents
        NamedCsvRecord record3 = createRecord("CustomerC", "30", "360", "3", "10 AM", "12 PM");

        processor.processRequest(List.of(record1, record2, record3));

        // Hour 10: CustomerA (10-11), CustomerC (10-12)
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<AllocationRequest>> captor10 = ArgumentCaptor.forClass(List.class);
        verify(storage).storeIntermediateData(eq(10), captor10.capture());
        List<AllocationRequest> requests10 = captor10.getValue();
        assertEquals(2, requests10.size());
        assertEquals("CustomerA", requests10.get(0).customer());
        assertEquals(1, requests10.get(0).agents());
        assertEquals("CustomerC", requests10.get(1).customer());
        assertEquals(2, requests10.get(1).agents());

        // Hour 11: CustomerB (11-12), CustomerC (10-12)
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<AllocationRequest>> captor11 = ArgumentCaptor.forClass(List.class);
        verify(storage).storeIntermediateData(eq(11), captor11.capture());
        List<AllocationRequest> requests11 = captor11.getValue();
        assertEquals(2, requests11.size());
        assertEquals("CustomerB", requests11.get(0).customer());
        assertEquals(2, requests11.get(0).agents());
        assertEquals("CustomerC", requests11.get(1).customer());
        assertEquals(2, requests11.get(1).agents());
    }

    @Test
    void testProcessRequest_InvalidRecord() {
        // Valid record
        NamedCsvRecord validRecord = createRecord("CustomerA", "10", "360", "1", "10 AM", "11 AM");
        // Invalid record (NumberFormatException for calls)
        NamedCsvRecord invalidRecord = createRecord("CustomerB", "invalid", "360", "1", "10 AM", "11 AM");

        processor.processRequest(List.of(validRecord, invalidRecord));

        // Should process the valid one
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<AllocationRequest>> captor = ArgumentCaptor.forClass(List.class);
        verify(storage).storeIntermediateData(eq(10), captor.capture());

        List<AllocationRequest> requests = captor.getValue();
        assertEquals(1, requests.size());
        assertEquals("CustomerA", requests.get(0).customer());
    }

    private NamedCsvRecord createRecord(String customer, String numCalls, String avgDuration, String priority, String startTime, String endTime) {
        String headers = String.join(",", Constants.CUSTOMER_COLUMN, Constants.NUM_CALLS, Constants.AVG_CALL_DURATION_SEC, Constants.PRIORITY, Constants.START_TIME, Constants.END_TIME);
        String values = String.join(",", customer, numCalls, avgDuration, priority, startTime, endTime);
        
        try (CsvReader<NamedCsvRecord> reader = CsvReader.builder().ofNamedCsvRecord(new StringReader(headers + "\n" + values))) {
            return reader.stream().findFirst().orElseThrow();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
