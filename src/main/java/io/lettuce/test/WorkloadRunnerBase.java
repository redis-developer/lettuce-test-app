package io.lettuce.test;

import io.lettuce.core.AbstractRedisClient;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisURI;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.cluster.event.ClusterTopologyChangedEvent;
import io.lettuce.core.event.Event;
import io.lettuce.core.event.EventBus;
import io.lettuce.core.event.connection.ReconnectAttemptEvent;
import io.lettuce.core.event.connection.ReconnectEventHelper;
import io.lettuce.core.event.connection.ReconnectFailedEvent;
import io.lettuce.core.metrics.MicrometerCommandLatencyRecorder;
import io.lettuce.core.metrics.MicrometerOptions;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.Delay;
import io.lettuce.test.config.WorkloadRunnerConfig;
import io.lettuce.test.config.WorkloadRunnerConfig.ClientOptionsConfig;
import io.lettuce.test.config.WorkloadRunnerConfig.SocketOptionsConfig;
import io.lettuce.test.config.WorkloadRunnerConfig.TcpUserTimeoutOptionsConfig;
import io.lettuce.test.config.WorkloadRunnerConfig.TimeoutOptionsConfig;
import io.lettuce.test.config.WorkloadRunnerConfig.WorkloadConfig;
import io.lettuce.test.metrics.ConnectionKey;
import io.lettuce.test.metrics.MetricsReporter;
import io.lettuce.test.workloads.BaseWorkload;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public abstract class WorkloadRunnerBase<C extends AbstractRedisClient, Conn extends StatefulConnection<?, ?>>
        implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(WorkloadRunnerBase.class);

    private final MetricsReporter metricsReporter;

    WorkloadRunnerConfig config;

    ExecutorService executor = Executors.newCachedThreadPool();

    List<C> clients = new ArrayList<>();

    Workloads submittedWorkloads = new Workloads();

    public WorkloadRunnerBase(WorkloadRunnerConfig config, MetricsReporter metricsReporter) {
        this.config = config;
        this.metricsReporter = metricsReporter;
    }

    public final void run() {
        RedisURI redisUri = buildRedisUri(config.getRedis());

        List<List<Conn>> connections = new ArrayList<>();

        // Create the specified number of client instances
        for (int i = 0; i < config.getTest().getClients(); i++) {
            C client = tryCreateClient(redisUri, config);
            if (client != null) {
                clients.add(client);

                List<Conn> clientConnections = new ArrayList<>();
                for (int j = 0; j < config.getTest().getConnectionsPerClient(); j++) {
                    Conn conn = tryCreateConnection(client, config);
                    if (conn != null) {
                        clientConnections.add(conn);
                    }
                }
                connections.add(clientConnections);
            }
        }

        try {
            CompletableFuture<Void> workloadsFuture = executeWorkloads(clients, connections);
            workloadsFuture.thenRun(() -> log.info("All tasks completed. Exiting..."));
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            executor.shutdown();
        }
    }

    private CompletableFuture<Void> executeWorkloads(List<C> clients, List<List<Conn>> connections) {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        WorkloadConfig workloadConfig = config.getTest().getWorkload();
        for (int i = 0; i < clients.size(); i++) {
            for (Conn conn : connections.get(i)) {
                for (int j = 0; j < config.getTest().getThreadsPerConnection(); j++) {
                    C client = clients.get(i);
                    BaseWorkload workload = createWorkload(client, conn, workloadConfig);
                    workload.metricsReporter(metricsReporter);
                    BaseWorkload withErrorHandler = withErrorHandler(workload, client, conn, workloadConfig);

                    futures.add(submit(withErrorHandler, config.getTest().getWorkload()));
                }
            }
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    private C tryCreateClient(RedisURI redisUri, WorkloadRunnerConfig config) {
        try {
            C redisClient = createClient(redisUri, config);
            subscribeToReconnectEvents(redisClient.getResources().eventBus());
            return redisClient;
        } catch (Exception e) {
            log.error("Failed to create client: " + e.getMessage());
            return null;
        }
    }

    private Conn tryCreateConnection(C client, WorkloadRunnerConfig config) {
        Timer.Sample sample = metricsReporter.startTimer();
        try {
            Conn connection = createConnection(client, config);
            metricsReporter.recordSuccessfulConnection(sample);
            return connection;
        } catch (Exception e) {
            log.error("Failed to create connection! ", e);
            metricsReporter.recordFailedConnection(sample);
            return null;
        }
    }

    /**
     * Creates a workload and initializes it with the given client, connection and configuration.
     *
     * @param client the Redis client, supports {@link io.lettuce.core.RedisClient} or
     *        {@link io.lettuce.core.cluster.RedisClusterClient}
     * @param connection the Redis connection, supports {@link io.lettuce.core.api.StatefulRedisConnection} and
     *        {@link io.lettuce.core.cluster.api.StatefulRedisClusterConnection}
     * @param config the workload configuration
     * @return the created workload instance
     * @throws IllegalArgumentException if the workload type is not supported
     */
    protected abstract BaseWorkload createWorkload(C client, Conn connection, WorkloadConfig config);

    protected abstract C createClient(RedisURI redisUri, WorkloadRunnerConfig config);

    protected abstract Conn createConnection(C client, WorkloadRunnerConfig config);

    protected CompletableFuture<ContinuousWorkload> submit(BaseWorkload task, WorkloadConfig config) {
        ContinuousWorkload workload = new ContinuousWorkload(task, config);
        CompletableFuture<ContinuousWorkload> future = CompletableFuture.runAsync(workload::run, executor)
                .whenComplete((result, throwable) -> {
                    submittedWorkloads.remove(workload);
                    if (throwable != null) {
                        log.error("Workload failed: ", throwable);
                    }
                }).thenApply(v -> workload);
        submittedWorkloads.add(workload);
        return future;
    }

    private BaseWorkload withErrorHandler(BaseWorkload task, C client, Conn conn, WorkloadConfig config) {
        return new BaseWorkload(task.options()) {

            @Override
            public void run() {
                Timer.Sample timer = metricsReporter.startTimer();
                try {
                    task.run();
                    metricsReporter.recordWorkloadExecutionDuration(timer, config.getType(), BaseWorkload.Status.SUCCESSFUL);
                } catch (Exception e) {
                    // Note: Use client and conn reference to track, which client and connection caused the error
                    // could not find other means to identify the client and connection
                    log.error("Error client: {} conn: {}", client, conn, e);
                    metricsReporter.recordWorkloadExecutionDuration(timer, config.getType(),
                            BaseWorkload.Status.COMPLETED_WITH_ERRORS);
                    throw e;
                }
            }

        };
    }

    protected RedisURI buildRedisUri(WorkloadRunnerConfig.RedisConfig config) {
        RedisURI.Builder builder = RedisURI.builder();
        builder.withHost(config.getHost()).withPort(config.getPort()).withDatabase(config.getDatabase())
                .withSsl(config.isUseTls()).withVerifyPeer(config.isVerifyPeer());

        if (config.getUsername() != null) {
            builder.withAuthentication(config.getUsername(), config.getPassword().toCharArray());
        } else if (config.getPassword() != null) {
            builder.withPassword(config.getPassword().toCharArray());
        }

        if (config.getClientName() != null) {
            builder.withClientName(config.getClientName());
        }

        if (config.getTimeout() != null) {
            builder.withTimeout(config.getTimeout());
        }

        return builder.build();
    }

    public void close() {
        log.info("Workload Runner stopping...");
        submittedWorkloads.stop();

        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
            }
        }

        for (C client : clients) {
            client.shutdown();
        }

        log.info("Workload Runner stopped.");
    }

    private void subscribeToReconnectEvents(EventBus eventBus) {
        Flux<Event> events = eventBus.get();

        events.subscribe(event -> {
            if (event instanceof ReconnectAttemptEvent) {
                ReconnectAttemptEvent reconnectEvent = (ReconnectAttemptEvent) event;
                ConnectionKey connectionKey = ReconnectEventHelper.connectionKey(reconnectEvent);
                metricsReporter.incrementReconnectAttempt(connectionKey);
            } else if (event instanceof ReconnectFailedEvent) {
                ReconnectFailedEvent reconnectEvent = (ReconnectFailedEvent) event;
                ConnectionKey connectionKey = ReconnectEventHelper.connectionKey(reconnectEvent);
                metricsReporter.incrementReconnectFailure(connectionKey);
            } else if (event instanceof ClusterTopologyChangedEvent) {
                ClusterTopologyChangedEvent ctcEvent = (ClusterTopologyChangedEvent) event;
                log.info("ClusterTopologyChangedEvent: before={}, after={}", ctcEvent.before(), ctcEvent.after());
            }
        });
    }

    protected void applyConfig(ClientResources.Builder resourceBuilder, ClientOptionsConfig config) {
        if (config == null) {
            return;
        }

        if (config.getReconnectOptions() != null) {
            applyReconnectOptionsConfig(resourceBuilder, config.getReconnectOptions());
        }

        if (config.getMetricsOptions() != null) {
            applyMetricsOptionsConfig(resourceBuilder, config.getMetricsOptions());
        }
    }

    private void applyMetricsOptionsConfig(ClientResources.Builder resourceBuilder,
            WorkloadRunnerConfig.MetricsOptionsConfig config) {
        MicrometerOptions options = MicrometerOptions.create();
        if (config.getConnectionMonitoring() != null && config.getConnectionMonitoring()) {
            // MicrometerConnectionMonitor monitor = new MicrometerConnectionMonitor(metricsReporter.getMeterRegistry(),
            // options);
            // resourceBuilder.connectionMonitor(monitor).build();
            OptionalMicrometerConnectionMonitor.applyMetricsOptionsConfig(resourceBuilder, metricsReporter.getMeterRegistry());
        }

        if (config.getCommandLatencyMonitoring() != null && config.getCommandLatencyMonitoring()) {
            MicrometerCommandLatencyRecorder monitor = new MicrometerCommandLatencyRecorder(metricsReporter.getMeterRegistry(),
                    options);
            resourceBuilder.commandLatencyRecorder(monitor).build();
        }
    }

    private void applyReconnectOptionsConfig(ClientResources.Builder resourceBuilder,
            WorkloadRunnerConfig.ReconnectOptionsConfig config) {
        if (config.getFixedDelay() != null) {
            resourceBuilder.reconnectDelay(Delay.constant(config.getFixedDelay()));
        }
    }

    protected ClientOptions createClientOptions(ClientOptionsConfig config) {
        ClientOptions.Builder builder = ClientOptions.builder();
        if (config != null) {
            if (config.getAutoReconnect() != null) {
                builder.autoReconnect(config.getAutoReconnect());
            }

            if (config.getSupportMaintenanceEvents() != null) {
                OptionalClientOptions.applySupportMaintenanceEventsOption(builder, config.getSupportMaintenanceEvents());
            }

            if (config.getPingBeforeActivate() != null) {
                builder.pingBeforeActivateConnection(config.getPingBeforeActivate());
            }

            if (config.getTimeoutOptions() != null) {
                TimeoutOptions.Builder timeoutOptions = TimeoutOptions.builder();
                applyTimeoutOptions(timeoutOptions, config.getTimeoutOptions());
                builder.timeoutOptions(timeoutOptions.build());
            }

            if (config.getSocketOptions() != null) {
                SocketOptions.Builder socketOptions = SocketOptions.builder();
                applySocketOptions(socketOptions, config.getSocketOptions());
                builder.socketOptions(socketOptions.build());
            }

            if (config.getDisconnectedBehavior() != null) {
                builder.disconnectedBehavior(ClientOptions.DisconnectedBehavior.valueOf(config.getDisconnectedBehavior()));
            }
        }

        return builder.build();
    }

    private void applyTimeoutOptions(TimeoutOptions.Builder builder, TimeoutOptionsConfig config) {
        if (config.getFixedTimeout() != null) {
            builder.fixedTimeout(config.getFixedTimeout());
        }

        if (config.getTimeoutsRelaxingDuringMaintenance() != null) {
            try {
                Method timeoutsRelaxingDuringMaintenance = builder.getClass().getMethod("timeoutsRelaxingDuringMaintenance",
                        Duration.class);
                timeoutsRelaxingDuringMaintenance.invoke(builder, config.getTimeoutsRelaxingDuringMaintenance());
                log.info("ProactiveTimeoutsRelaxingMethod enabled successfully.");
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException("The method 'timeoutsRelaxingDuringMaintenance' is not available in this build.", e);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException("Failed to invoke 'timeoutsRelaxingDuringMaintenance' method.", e);
            }
        }
    }

    private void applySocketOptions(SocketOptions.Builder builder, SocketOptionsConfig config) {
        if (config.getConnectTimeout() != null) {
            builder.connectTimeout(config.getConnectTimeout());
        }
        if (config.getKeepAliveOptions() != null) {
            SocketOptions.KeepAliveOptions.Builder keepAliveOptions = SocketOptions.KeepAliveOptions.builder();
            applyKeepAliveOptions(keepAliveOptions, config.getKeepAliveOptions());
            builder.keepAlive(keepAliveOptions.build());
        }
        if (config.getTcpUserTimeoutOptions() != null) {
            SocketOptions.TcpUserTimeoutOptions.Builder tcpUserTimeoutOptions = SocketOptions.TcpUserTimeoutOptions.builder();
            applyTcpUserTimeoutOptions(tcpUserTimeoutOptions, config.getTcpUserTimeoutOptions());
            builder.tcpUserTimeout(tcpUserTimeoutOptions.build());
        }
    }

    private void applyTcpUserTimeoutOptions(SocketOptions.TcpUserTimeoutOptions.Builder builder,
            TcpUserTimeoutOptionsConfig config) {
        if (config.getTcpUserTimeout() != null) {
            builder.tcpUserTimeout(config.getTcpUserTimeout());
        }
        if (config.getEnabled() != null) {
            builder.enable();
        }
    }

    private void applyKeepAliveOptions(SocketOptions.KeepAliveOptions.Builder builder,
            WorkloadRunnerConfig.KeepAliveOptionsConfig config) {
        if (config.getInterval() != null) {
            builder.interval(config.getInterval());
        }

        if (config.getIdle() != null) {
            builder.idle(config.getIdle());
        }

        if (config.getCount() != null) {
            builder.count(config.getCount());
        }
    }

    static class OptionalClientOptions {

        static public void applySupportMaintenanceEventsOption(ClientOptions.Builder builder, boolean value) {
            try {
                Method method = builder.getClass().getMethod("supportMaintenanceEvents", boolean.class);
                method.invoke(builder, value);
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException(
                        "supportMaintenanceEvents configuration option can't be applied. Not available in this build.", e);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException("Failed to configure proactiveRebind option.", e);
            }
        }

    }

    // This class is a workaround to avoid a compile-time dependency on Lettuce's MicrometerConnectionMonitor class
    // which is not available in the Lettuce core module yet
    // Once the MicrometerConnectionMonitor class is available in the core module, this class can be removed
    static class OptionalMicrometerConnectionMonitor {

        private static final Logger log = LoggerFactory.getLogger(OptionalMicrometerConnectionMonitor.class);

        public static void applyMetricsOptionsConfig(ClientResources.Builder resourceBuilder, MeterRegistry meterRegistry) {
            MicrometerOptions options = MicrometerOptions.create();

            configureConnectionMonitor(resourceBuilder, meterRegistry, options);

            configureQueueMonitor(resourceBuilder, meterRegistry, options);

        }

        private static void configureConnectionMonitor(ClientResources.Builder resourceBuilder, MeterRegistry meterRegistry,
                MicrometerOptions options) {
            try {
                Class<?> connectionMonitorClass = Class.forName("io.lettuce.core.metrics.ConnectionMonitor");

                Class<?> micrometerConnectionMonitorClass = Class
                        .forName("io.lettuce.core.metrics.MicrometerConnectionMonitor");

                Constructor<?> constructor = micrometerConnectionMonitorClass.getConstructor(MeterRegistry.class,
                        MicrometerOptions.class);

                Object monitor = constructor.newInstance(meterRegistry, options);

                Method connectionMonitorMethod = resourceBuilder.getClass().getMethod("connectionMonitor",
                        connectionMonitorClass);

                connectionMonitorMethod.invoke(resourceBuilder, monitor);
                log.info("MicrometerConnectionMonitor configured successfully.");
            } catch (ClassNotFoundException e) {
                log.warn("MicrometerConnectionMonitor or ConnectionMonitor class not found. Skipping connection monitoring.");
            } catch (NoSuchMethodException e) {
                log.warn("connectionMonitor method not found in ClientResources.Builder. Skipping connection monitoring.");
            } catch (InvocationTargetException | IllegalAccessException | InstantiationException e) {
                log.error("Failed to invoke connectionMonitor method", e);
            }
        }

        private static void configureQueueMonitor(ClientResources.Builder resourceBuilder, MeterRegistry meterRegistry,
                MicrometerOptions options) {
            try {
                Class<?> queueMonitorClass = Class.forName("io.lettuce.core.metrics.EndpointQueueMonitor");

                Class<?> micrometerQueueMonitor = Class.forName("io.lettuce.core.metrics.MicrometerQueueMonitor");

                Constructor<?> queueMonitorConstructor = micrometerQueueMonitor.getConstructor(MeterRegistry.class,
                        MicrometerOptions.class);

                Object queueMonitor = queueMonitorConstructor.newInstance(meterRegistry, options);

                Method queueMonitorMethod = resourceBuilder.getClass().getMethod("endpointQueueMonitor", queueMonitorClass);

                queueMonitorMethod.invoke(resourceBuilder, queueMonitor);
                log.info("MicrometerQueueMonitor configured successfully.");
            } catch (ClassNotFoundException e) {
                log.warn("MicrometerQueueMonitor or QueueMonitor class not found. Skipping connection monitoring.");
            } catch (NoSuchMethodException e) {
                log.warn("endpointQueueMonitor method not found in ClientResources.Builder. Skipping connection monitoring.");
            } catch (InvocationTargetException | IllegalAccessException | InstantiationException e) {
                log.error("Failed to invoke endpointQueueMonitor method", e);
            }
        }

    }

}
