# Lettuce Test App Runner

A workload runner for testing Lettuce client fault tolerance against Redis database upgrades.

## Build

```sh
mvn clean package
```
## Usage

```sh
java -Dlogdir=./get_set/logs -jar target/lettuce-test-app-1.0-SNAPSHOT.jar --config <config.yaml>
```
## Enabling Metrics Logging
 
To enable metrics logging and configure the reporting step size, you can modify the configuration as follows:
 
```yaml
metrics:
  logging:
    enable: true  # Enable logging of metrics.
    step: 1s      # Set the step size (i.e., reporting frequency). This can be adjusted based on how often you want to report metrics.
  influx:
    enable: false  # InfluxDB integration can be enabled if required (set to `true`).
    uri: http://localhost:8086  # URI for InfluxDB.
    db: 1  # Database to write metrics to.
    autoCreateDb: true  # Auto-create the InfluxDB database if it doesn't exist.
    step: 1s  # Step size for InfluxDB metrics reporting.
```
 
## Default Metrics Logging Location

Metrics will be logged in the default log file (e.g., `${logdir:-logs}/lettuce-test-app-metrics.log`).

## Metrics

 The following metrics are exposed to track the health and performance of Redis connections and commands using Micrometer:
 
 | Metric Name                | Type    | Description                                                                                           | Tags                                                                                              |
 |----------------------------|---------|-------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------|
 | `lettuce.connect.success`   | Timer   | Measures the duration and count of successful Redis connections.                                       | N/A                                                                                               |
 | `lettuce.connect.failure`   | Timer   | Measures the duration and count of failed Redis connection attempts.                                   | N/A                                                                                               |
 | `lettuce.reconnect.attempts`| Counter | Counts the number of Redis reconnect attempts.                                                        | `epid`: Endpoint ID, `local`: Local address, `remote`: Remote address                             |
 | `lettuce.reconnect.failures`| Counter | Counts the number of failed Redis reconnect attempts.                                                 | `epid`: Endpoint ID, `local`: Local address, `remote`: Remote address                             |
 | `redis.command.latency`     | Timer   | Measures the execution time of Redis commands from API invocation until command completion.           | `command`: Redis command (e.g., `GET`, `SET`)                                                      |
 | `redis.command.errors`      | Counter | Counts the number of failed Redis command API calls that completed with an exception.                 | `command`: Redis command (e.g., `GET`, `SET`)                                                      |
 