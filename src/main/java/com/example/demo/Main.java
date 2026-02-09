package com.example.demo;

import com.example.demo.Constants.OutputFormat;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "main", description = "Main command", mixinStandardHelpOptions = true)
public class Main implements Callable<Integer> {

    @Option(names = {"--input"}, defaultValue = "test.csv", description = "The filename.")
    private String inputFile;

    @Option(names = {"--utilization"}, defaultValue = "1.0", description = "The utilization of the agent.")
    private float utilization;

    @Option(names = {"--format"}, defaultValue = "TEXT", description = "The output format.")
    private OutputFormat outputFormat;

    @Option(names = {"--capacity"}, defaultValue = "0", description = "The capacity of the agent. Zero means unlimited capacity.")
    private int capacity;

    @Override
    public Integer call() throws Exception {
        if (utilization <= 0 || utilization > 1.0f) {
            System.err.println("Error: Utilization must be between 0 (exclusive) and 1 (inclusive).");
            return 1;
        }
        ControlPlaneScheduler scheduler = new ControlPlaneScheduler(inputFile, utilization, outputFormat, capacity);
        scheduler.run();

        return 0;
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }
}