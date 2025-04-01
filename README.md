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
   - runId: Unique identifier for the test run.

 | Metric Name                | Type    | Description                                                                                           | Tags                                                                                              |
 |----------------------------|---------|-------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------|
 | `lettuce.connect.success`   | Timer   | Measures the duration and count of successful Redis connections.                                       | N/A                                                                                               |
 | `lettuce.connect.failure`   | Timer   | Measures the duration and count of failed Redis connection attempts.                                   | N/A                                                                                               |
 | `lettuce.reconnect.attempts`| Counter | Counts the number of Redis reconnect attempts.                                                        | `epid`: Endpoint ID, `local`: Local address, `remote`: Remote address                             |
 | `lettuce.reconnect.failures`| Counter | Counts the number of failed Redis reconnect attempts.                                                 | `epid`: Endpoint ID, `local`: Local address, `remote`: Remote address                             |
 | `redis.command.latency`     | Timer   | Measures the execution time of Redis commands from API invocation until command completion.           | `command`: Redis command (e.g., `GET`, `SET`)                                                      |
 | `redis.command.errors`      | Counter | Counts the number of failed Redis command API calls that completed with an exception.                 | `command`: Redis command (e.g., `GET`, `SET`)                                                      |

### Lettuce App Custom Metrics
Modified version of lettuce with additional metrics is available at [lettuce-metrics](https://github.com/ggivo/lettuce/tree/lettuce-observability)
To use this version, you need to build the project and replace the lettuce version in the `pom.xml` file with the built jar file.

**Steps to build and replace the lettuce jar file:**
```shell
git clone -b lettuce-observability https://github.com/ggivo/lettuce/ lettuce-observability
cd lettuce-observability
mvn clean install -DskipTests
```
Write down the version of the built jar file. It will be used in the next step.

**Make sure lettuce version in the `pom.xml` file is updated to the built jar file.**
```xml
        <dependency>
            <groupId>io.lettuce</groupId>
            <artifactId>lettuce-core</artifactId>
            <version>{PROVIDE_CUSTOM_LETTUCE_VERSION}</version>
        </dependency>
```
**Last step is to build the lettuce-test-app with the updated lettuce jar file.**
```shell
mvn clean package
```

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
| `lettuce.reconnection.attempts.count`   | Timer   | Measures the duration connection was in inactive state during reconnection. | epid                                                                                                                                                                                                  |
| `lettuce.reconnection.attempts.count`   | Counter | Number of reconnection attempts.                                            | epid. <br> Note: this metric is similar to `lettuce.reconnect.attempts`  but is reported directly by Lettuce client itself  and is counted per `epid` (e.g not taged with `local` and `remote` tags). |

Example : 
```   
 Counter: MeterId{name='lettuce.reconnection.attempts.count', tags=[tag(epid=0x1),tag(runId=${runId:get_set_async-8szygeLU)]} value: 11.0
 Timer: MeterId{name='lettuce.reconnection.inactive.duration', tags=[tag(epid=0x1),tag(runId=${runId:get_set_async-8szygeLU)]} count: 1 total time: 45587.98
```
