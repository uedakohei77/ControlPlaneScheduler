# Control Plane Scheduler

A robust Java-based scheduling tool designed to calculate and allocate call center agent capacity based on customer demand, priority, and system constraints.

## Overview

The Control Plane Scheduler reads customer call data from a CSV file, processes the demand based on call volume and duration, and generates an hourly schedule of agent allocations. It supports:

*   **Utilization Adjustments**: Scale agent efficiency.
*   **Capacity Constraints**: Limit the total number of agents available per hour.
*   **Priority-Based Allocation**: Prioritize specific customers when capacity is limited.
*   **Flexible Output**: Generate schedules in Text, JSON, or UI format.
*   **Scalable Processing**: Uses an intermediate storage mechanism to handle large datasets efficiently.

## Prerequisites

*   Java 11 or higher
*   Maven

## Building the Project

To build the project and run tests:

```bash
mvn clean package
```

## Usage

Run the application using the generated JAR file (ensure dependencies are on the classpath):

```bash
java -cp target/classes:target/dependency/* com.example.demo.Main [OPTIONS]
```

### Command Line Options

| Option | Default | Description |
| :--- | :--- | :--- |
| `--input` | `test.csv` | Path to the input CSV file containing call data. |
| `--utilization` | `1.0` | Agent utilization factor (e.g., 0.8 for 80%). |
| `--format` | `TEXT` | Output format: `TEXT`, `JSON`, or `UI`. |
| `--capacity` | `0` | Maximum total agents allowed per hour. `0` indicates infinite capacity. |
| `--storage` | `MEMORY` | Storage type: `MEMORY` or `FILESYSTEM`. |

### Input CSV Format

The input file must contain the following headers:

```csv
CustomerName,NumberOfCalls,AverageCallDurationSeconds,Priority,StartTimePT,EndTimePT
```

**Example:**
```csv
CustomerA,100,300,1,10:00 AM,11:00 AM
CustomerB,200,300,2,10:00 AM,11:00 AM
```

### Examples

**1. Basic Run (Infinite Capacity)**
```bash
java -cp target/classes:target/dependency/* com.example.demo.Main --input data.csv
```

**2. JSON Output with 80% Utilization**
```bash
java -cp target/classes:target/dependency/* com.example.demo.Main --input data.csv --utilization 0.8 --format JSON
```

**3. Constrained Capacity (Max 50 agents)**
```bash
java -cp target/classes:target/dependency/* com.example.demo.Main --input data.csv --capacity 50
```

**4. Filesystem Storage (For larger datasets)**
```bash
java -cp target/classes:target/dependency/* com.example.demo.Main --input data.csv --storage FILESYSTEM
```

## Project Structure

*   `ControlPlaneScheduler`: Main orchestration logic.
*   `RequestProcessor`: Parses CSV and calculates raw demand.
*   `CallOrchestrator`: Applies capacity and priority logic.
*   `InMemoryStorage`: Default in-memory storage for fast processing of smaller datasets (<1M rows). 
*   `PersistentStorage`: Disk-based storage for handling large datasets efficiently.