package com.example.demo;

public class Constants {

    public static final String CUSTOMER_COLUMN = "CustomerName";
    public static final String AVG_CALL_DURATION_SEC = "AverageCallDurationSeconds";
    public static final String START_TIME = "StartTimePT";
    public static final String END_TIME = "EndTimePT";
    public static final String NUM_CALLS = "NumberOfCalls";
    public static final String PRIORITY = "Priority";
    public static final String SCHEDULE_FILE_PREFIX = "schedule";
    public static final String INTERMEDIATE_FILE_PREFIX = "intermediate";
    public static final String HTLM_FILE_NAME = "scheduler_report.html";

    /** The output format. */
    enum OutputFormat {
        TEXT,
        JSON,
        UI
    }

}
