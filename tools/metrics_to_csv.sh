#!/bin/bash

# Check if input file is provided
if [ "$#" -ne 1 ]; then
    echo "Usage: $0 <metrics.log>"
    exit 1
fi

INPUT_FILE="$1"
OUTPUT_FILE="metrics.csv"

# Process the metrics file and generate CSV
awk -F '[{},= ]+' '
BEGIN { print "timestamp,metric_name,command,run_id,throughput (ops/sec),mean_latency (s),max_latency (s)" }
{
    # Correctly capture the timestamp (first field)
    timestamp=$1

    # Extract the metric name (it is always at the 3rd field)
    metric_name=$9;

    # Initialize other variables
    command=""; run_id=""; throughput=""; mean=""; max="";

    # Loop over the fields to extract the relevant data
    for (i=4; i<=NF; i+=2) {
        if ($i == "command") command=$(i+1);
        else if ($i == "runId") run_id=$(i+1);
        else if ($i == "throughput") throughput=$(i+1);
        else if ($i == "mean") mean=$(i+1);
        else if ($i == "max") max=$(i+1);
    }

    # Print the final CSV format with the timestamp and extracted values
    print timestamp "," metric_name "," command "," run_id "," throughput "," mean "," max;
}' "$INPUT_FILE" > "$OUTPUT_FILE"

echo "Metrics exported to $OUTPUT_FILE"