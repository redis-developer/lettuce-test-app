# Lettuce Test App Runner

A workload runner for testing Lettuce client fault tolerance against Redis database upgrades.

## Build

```sh
mvn clean package
```
## Usage
Basic usage with specified runner configuration file and custom log directory:
```sh
java -jar target/lettuce-test-app-0.0.1-SNAPSHOT.jar --runner.config=runner-config.yaml --logging.file.path=logs
``` 
### Override Configuration Properties
Properties defined in `runner-config.yaml` can be overridden from the command line using the following syntax:
```sh
--<property-path>=<value>
```

#### Example
 ```sh
 java -jar target/lettuce-test-app-0.0.1-SNAPSHOT.jar --runner.config=runner-config.yaml --logging.file.path=logs --runner.test.workload.type=get_set --runner.test.workload.options.getSetRatio=0.3
 ```
 In this example:
 - `--runner.test.workload.type=get_set`: Overrides the workload type to `get_set`.
 - `--runner.test.workload.options.getSetRatio=0.3`: Overrides the `getSetRatio` option for the `get_set` workload.

## Adding new Workload
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

## Metrics
 
To enable metrics logging and configure the reporting step size, you can modify the configuration as follows:
### Logging Metrics to a File
Logging metrics to a file is enabled by default. To disable it, or to change the reporting step size, set the following properties in `application.properties`:
Metrics will be logged in the default log file (e.g., `${logdir:-logs}/lettuce-test-app-metrics.log`).
```properties
logging.metrics.enabled=true
logging.metrics.step=PT10S
``` 

### Logging Metrics to InfluxDB
Logging metrics to InfluxDB is disabled by default.To enable it, you need to set the following properties in `application.properties`:

```properties
# InfluxDB Configuration for Micrometer (Spring Boot 3.x)
management.influx.metrics.export.enabled=true
management.influx.metrics.export.uri=http://localhost:8086
management.influx.metrics.export.org=<your-organization>
management.influx.metrics.export.bucket=lettuce-test
management.influx.metrics.export.token=<your-token>
management.influx.metrics.export.auto-create-bucket=true
management.influx.metrics.export.consistency=one
management.influx.metrics.export.step=PT5S
```
### Example InfluxDB Query's
Example query for visualising throughput per second of get on 10s window
<details>
  <summary><strong>Throughput per second</strong></summary>

```sql
 from(bucket: "lettuce-test")
   |> range(start: v.timeRangeStart, stop: v.timeRangeStop)
   |> filter(fn: (r) => r["_measurement"] == "redis_command_latency")
   |> filter(fn: (r) => r["_field"] == "count")
   |> aggregateWindow(every: 10s, fn: sum, createEmpty: false)  // Sum the count of requests over 10s intervals
   |> map(fn: (r) => ({ r with _value: r._value / 10.0 }))  // Normalize to requests per second
   |> yield(name: "throughput")
```

</details>

### Setting Up InfluxDB
<details>
  <summary><strong>Step by step instructions</strong></summary>
 
 1. **Pull and Run the InfluxDB Docker container:**
   ```sh
   docker run -d --name influxdb -p 8086:8086 -v influxdb_data:/var/lib/influxdb influxdb:latest
   ```
 2. **Create Test Bucket:**
    - Access the InfluxDB UI at `http://localhost:8086`.
    - Follow the on-screen instructions to set up your initial user, organization, and bucket.
    - Create a bucket named `lettuce-test`.

3. **Generate Token:**
 - In the InfluxDB UI, go to the `Data` section.
 - Select `Tokens`.
 - Click `Generate Token` and choose `All-Access Token` or `Read/Write Token`.
 - Copy the generated token for later use.
 </details>

### Lettuce Test App Custom Metrics
 **Common Tags:**
   - runId: Unique identifier for the test run.

 | Metric Name                | Type    | Description                                                                                           | Tags                                                                                              |
 |----------------------------|---------|-------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------|
 | `lettuce.connect.success`   | Timer   | Measures the duration and count of successful Redis connections.                                       | N/A                                                                                               |
 | `lettuce.connect.failure`   | Timer   | Measures the duration and count of failed Redis connection attempts.                                   | N/A                                                                                               |
 | `lettuce.reconnect.attempts`| Counter | Counts the number of Redis reconnect attempts.                                                        | `epid`: Endpoint ID, `local`: Local address, `remote`: Remote address                             |
 | `lettuce.reconnect.failures`| Counter | Counts the number of failed Redis reconnect attempts.                                                 | `epid`: Endpoint ID, `local`: Local address, `remote`: Remote address                             |
 | `redis.command.latency`     | Timer   | Measures the execution time of Redis commands from API invocation until command completion.           | `command`: Redis command (e.g., `GET`, `SET`)                                                      |
 | `redis.command.errors`      | Counter | Counts the number of failed Redis command API calls that completed with an exception.                 | `command`: Redis command (e.g., `GET`, `SET`)                                                      |
