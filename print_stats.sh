#!/bin/bash

# Check if the user provided a stat property name as an argument
if [ -z "$1" ]; then
    echo "Please provide the stat property name to query (e.g., total_connections_received, total_commands_processed, instantaneous_ops_per_sec)."
    exit 1
fi

# Set the Redis server host and port (change if necessary)
REDIS_HOST="localhost"
REDIS_PORT="6379"
# Get the property name passed as argument
PROPERTY_NAME=$1

# Loop indefinitely to get the stats periodically
while true; do
    # Get the stats from Redis
    stats=$(redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" INFO stats)

    # Extract the specific statistic based on the provided property name
    value=$(echo "$stats" | grep "$PROPERTY_NAME" | cut -d: -f2)

    # Check if the property was found and print the value
    if [ -z "$value" ]; then
        echo "Property '$PROPERTY_NAME' not found in Redis stats."
    else
        echo "$PROPERTY_NAME : $value"
    fi

    # Sleep for 1 second before running again
    sleep 1
done