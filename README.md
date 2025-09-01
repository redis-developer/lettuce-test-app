# Lettuce Test App Runner

A workload runner for testing Lettuce client fault tolerance against Redis database upgrades.

> **ℹ️ NOTE**: This project uses lettuce-core 7.0.0-SNAPSHOT which includes the maintenance events features. The snapshot version is automatically downloaded from Maven repositories.

## Build

### Building the Project

Use the provided build script to build and test the project:

```sh
./scripts/build.sh
```

The build script will:
1. Check code formatting
2. Build the lettuce-test-app using the lettuce-core version specified in pom.xml
3. Run tests to verify everything works correctly

### Manual Build

You can also build manually using Maven:

```sh
# Check formatting
mvn formatter:validate

# Build and test
mvn clean verify

# Run tests
mvn test
```

### CI/CD Integration
The GitHub Actions workflow automatically builds and tests the project. See `.github/workflows/integration.yaml` for details.
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

| Alias                | Class                        | Description                                                                          | Supported Options                                     |
|----------------------|------------------------------|--------------------------------------------------------------------------------------|-------------------------------------------------------|
| get_set              | `GetSetWorkload`             | Performs a mix of GET and SET operations with a specified ratio and value size.      | `getSetRatio`, `valueSize`, `iterationCount`          |
| get_set_async        | `GetSetAsyncWorkload`        | Performs asynchronous GET and SET operations with a specified ratio and value size.  | `getSetRatio`, `valueSize`, `iterationCount`          |
| redis_commands       | `RedisCommandsWorkload`      | Executes a specified number of get/set/del/incr/lpush/lrange commands.               | `valueSize`, `elementsCount`, `iterationCount`        |
| redis_commands_async | `RedisCommandsAsyncWorkload` | Executes a specified number of get/set/del/incr/lpush/lrange commands.               | `valueSize`, `elementsCount`, `iterationCount`        |
| multi                | `MultiWorkload`              | Executes get/set in MULTI/EXEC transactions with a specified size and command count. | `transactionSize`, `iterationCount`, `valueSize`, `getSetRatio` |
| pub_sub              | `PubSubWorkload`             | Publishes and subscribes to messages on a specified channel.                         |          |
### RedisClusterClient

| Alias          | Class                               | Description                                                                 | Supported Options                  |
|----------------|-------------------------------------|-----------------------------------------------------------------------------|-----------------------------------|
| get_set        | `GetSetClusterWorkload`             | Performs a mix of GET and SET operations on a Redis cluster with a specified ratio and value size. | `getSetRatio`, `valueSize`, `iterationCount` |
| get_set_async  | `GetSetAsyncClusterWorkload`        | Performs asynchronous GET and SET operations with a specified ratio and value size.  | `getSetRatio`, `valueSize`, `iterationCount`          |
| redis_commands_async | `RedisCommandsAsyncClusterWorkload` | Executes a specified number of get/set/del/incr/lpush/lrange commands.               | `valueSize`, `elementsCount`, `iterationCount`        |
| redis_commands | `RedisCommandsClusterWorkload`      | Executes a specified number of get/set/del/incr/lpush/lrange commands.               | `valueSize`, `elementsCount`, `iterationCount`        |
| pub_sub        | `PubSubClusterWorkload`             | Publishes and subscribes to messages on a specified channel.                         |          |   

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
   - run_id: Unique identifier for the test run.

 | Metric Name                | Type    | Description                                                                                                                                                                                      | Tags                                                                                        |
 |----------------------------|---------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------|
 | `lettuce.connect.success`   | Timer   | Measures the duration and count of successful initial Redis connections.                                                                                                                         | N/A                                                                                         |
 | `lettuce.connect.failure`   | Timer   | Measures the duration and count of failed initial Redis connection attempts.                                                                                                                     | N/A                                                                                         |
 | `lettuce.reconnect.attempts`| Counter | Counts the number of Redis reconnect attempts per connection (localAddr, remoteAddr, epid). Corresponds to ReconnectAttemptEvent                                                                 | `epid`: Endpoint ID, `local`: Local address, `remote`: Remote address                       |
 | `lettuce.reconnect.failures`| Counter | Counts the number of failed Redis reconnect attempts.Corresponds to ReconnectFailedEvent                                                                                                         | `epid`: Endpoint ID, `local`: Local address, `remote`: Remote address                       |
 | `lettuce.reconnect.total.attempts `| Counter | Counts the number of Redis reconnect attempts(ReconnectAttemptEvent across all connections)                                                                                                      | N/A                                                                                        |
 | `lettuce.reconnect.total.failures `| Counter | Counts the number of failed Redis reconnect attempts(ReconnectFailedEvent across all connections)                                                                                                | N/A                                                                                        |
 | `redis.connections.total`| Counter | Counts the number of Redis reconnect attempts per connection (INITIATED and completed with ERROR)  . (as reported by lettuce core library via ReconnectAttemptEvent- INITIATED, ReconnectFailedEvent-ERROR | 'status':(INITIATED, ERROR), `epid`: Endpoint ID, `local`: Local address, `remote`: Remote address |
 | `redis.connection.drops.total`| Counter | Counts the number of disconnects per connection (localAddr, remoteAddr, epid). (as reported by lettuce core library via DisconnectedEvents)                                                      | `epid`: Endpoint ID, `local`: Local address, `remote`: Remote address                       |
 | `redis.command.errors`      | Counter | Counts the number of failed Redis command API calls that completed with an exception. (per command type)                                                                                         | `command`: Redis command (e.g., `GET`, `SET`)                                               |
 | `redis.operation.duration.total`     | TIMER   | Measures the execution time of Redis commands from API invocation until command completion. Percentiles (0.5, 0.95, 0.99)                                                                        | Agregated across connections/command types                                                  |
 | `redis.operation.duration`      | TIMER   | Measures the execution time of Redis commands from API invocation until command completion per command.                                                                                          | `command`: Redis command (e.g., `GET`, `SET`), status: (SUCCESS, ERROR, INITIATED)          |
 | `redis.operations.total`      | Counter   | Counts the number of total Redis command API calls completed successfully or with an error.                                                                                                      | `command`: Redis command (e.g., `GET`, `SET`), status: (SUCCESS, ERROR)                 |

### Lettuce App Custom Metrics
This project uses lettuce-core 7.0.0-SNAPSHOT which includes additional metrics from the maintenance events features that were merged into the main branch.

**Building the Project:**
Use the build script to build and test the project:

```shell
./scripts/build.sh
```

The lettuce-core snapshot version with maintenance events features is automatically downloaded from Maven repositories.

Additional metrics aer enabled/disabled via configuration property in the `runner-config.yaml` file:
```yaml
runner:
  clientOptions:
    metricsOptions:
      commandLatencyMonitoring: true
      connectionMonitoring: true
```
Following additional metrics are available in the modified lettuce version:


| Metric Name                | Type    | Description                                                                 | Tags                                                                                                                                                                                                  |
|----------------------------|---------|-----------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `redis.reconnection.duration`   | Timer   | Measures the duration connection was in inactive state during reconnection. | epid                                                                                                                                                                                                  |
| `redis.reconnection.attempts`   | Counter | Number of reconnection attempts.                                            | epid. <br> Note: this metric is similar to `lettuce.reconnect.attempts`  but is reported directly by Lettuce client itself  and is counted per `epid` (e.g not taged with `local` and `remote` tags). |

Example : 
```   
 Counter: MeterId{name='lettuce.reconnection.attempts.count', tags=[tag(epid=0x1),tag(runId=${runId:get_set_async-8szygeLU)]} value: 11.0
 Timer: MeterId{name='lettuce.reconnection.inactive.duration', tags=[tag(epid=0x1),tag(runId=${runId:get_set_async-8szygeLU)]} count: 1 total time: 45587.98
```
