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

You can at any time add new workload (set of operations)

By: 
1. Choose a sample workload from the `workloads` directory
2. Ask AI to generate a workload for you or write one depending on your needs
3. You only need to change the run method in the `WorkloadRunner` class to use the new workload
4. Run the application

## Configuration
## Workloads

### Common Options

| Option          | Description                                                                 | Default Value |
|-----------------|-----------------------------------------------------------------------------|---------------|
| `valueSize`     | Size of the value in bytes.                                                 | 100           |
| `elementsCount` | Number of elements to process.                                              | 1             |
| `iterationCount`| Number of times to repeat the workload.                                     | 1000          |
| `getSetRatio`   | Ratio of GET to SET operations.                                             | 0.5           |
| `transactionSize`| Number of commands to execute per iteration.                               | 100           |

### RedisClient

| Alias          | Class                | Description                                                                          | Supported Options                                     |
|----------------|----------------------|--------------------------------------------------------------------------------------|-------------------------------------------------------|
| get_set        | `GetSetWorkload`     | Performs a mix of GET and SET operations with a specified ratio and value size.      | `getSetRatio`, `valueSize`, `iterationCount`          |
| get_set_async  | `GetSetAsyncWorkload`| Performs asynchronous GET and SET operations with a specified ratio and value size.  | `getSetRatio`, `valueSize`, `iterationCount`          |
| redis_commands | `RedisCommandsWorkload` | Executes a specified number of get/set/del/incr/lpush/lrange commands.               | `valueSize`, `elementsCount`, `iterationCount`        |
| multi          | `MultiWorkload`      | Executes get/set in MULTI/EXEC transactions with a specified size and command count. | `transactionSize`, `iterationCount`, `valueSize`, `getSetRatio` |
| pub_sub        | `PubSubWorkload`     | Publishes and subscribes to messages on a specified channel.                         |          |
### RedisClusterClient

| Alias          | Class                      | Description                                                                 | Supported Options                  |
|----------------|----------------------------|-----------------------------------------------------------------------------|-----------------------------------|
| get_set        | `GetSetClusterWorkload`    | Performs a mix of GET and SET operations on a Redis cluster with a specified ratio and value size. | `getSetRatio`, `valueSize`, `iterationCount` |

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
 