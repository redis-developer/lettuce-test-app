package io.lettuce.test;

import io.lettuce.core.AbstractRedisClient;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisURI;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.event.Event;
import io.lettuce.core.event.EventBus;
import io.lettuce.core.event.connection.ReconnectAttemptEvent;
import io.lettuce.core.event.connection.ReconnectEventHelper;
import io.lettuce.core.event.connection.ReconnectFailedEvent;
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
import io.micrometer.core.instrument.Timer;
import io.netty.channel.local.LocalAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.net.SocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public abstract class WorkloadRunnerBase<C extends AbstractRedisClient, Conn extends StatefulConnection<?, ?>>
        implements AutoCloseable {

    private static final long SHUTDOWN_DELAY = Duration.ofSeconds(1).toSeconds();

    private static final Logger log = LoggerFactory.getLogger(WorkloadRunnerBase.class);

    private final MetricsReporter metricsReporter;

    WorkloadRunnerConfig config;

    ExecutorService executor = Executors.newCachedThreadPool();

    Workloads submittedWorkloads = new Workloads();

    public WorkloadRunnerBase(WorkloadRunnerConfig config, MetricsReporter metricsReporter) {
        this.config = config;
        this.metricsReporter = metricsReporter;
    }

    public final void run() {
        RedisURI redisUri = buildRedisUri(config.getRedis());

        List<C> clients = new ArrayList<>();
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

        executeWorkloads(clients, connections);
    }

    private void executeWorkloads(List<C> clients, List<List<Conn>> connections) {
        for (int i = 0; i < clients.size(); i++) {
            for (Conn conn : connections.get(i)) {
                for (int j = 0; j < config.getTest().getThreadsPerConnection(); j++) {
                    C client = clients.get(i);
                    BaseWorkload workload = createWorkload(client, conn, config.getTest().getWorkload());
                    workload.metricsReporter(metricsReporter);
                    BaseWorkload withErrorHandler = withErrorHandler(workload, client, conn);

                    submit(withErrorHandler, config.getTest().getWorkload());
                }
            }
        }
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
        Timer.Sample sample = metricsReporter.startConnectionTimer();
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

    protected CompletableFuture<?> submit(BaseWorkload task, WorkloadConfig config) {
        ContinousWorkload workload = new ContinousWorkload(task, config);
        CompletableFuture<Void> future = CompletableFuture.runAsync(workload, executor).whenComplete((result, throwable) -> {
            submittedWorkloads.remove(workload);
            if (throwable != null) {
                log.error("Workload failed: ", throwable);
            }
        });
        submittedWorkloads.add(workload);
        return future;
    }

    private BaseWorkload withErrorHandler(BaseWorkload task, C client, Conn conn) {
        return new BaseWorkload(task.options()) {

            @Override
            public void run() {
                try {
                    task.run();
                } catch (Exception e) {
                    // Note: Use client and conn reference to track, which client and connection caused the error
                    // could not find other means to identify the client and connection
                    log.error("Error client: {} conn: {}", client, conn, e);
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
        submittedWorkloads.stop();

        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
            }
        }
    }

    public void awaitTermination() {

        long maxTime = config.getTest().getWorkload().getMaxDuration().toSeconds();
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(maxTime + SHUTDOWN_DELAY);

        // Wait until the queue is empty or timeout is reached
        while (!submittedWorkloads.isEmpty() && System.currentTimeMillis() < deadline) {
            log.debug("Waiting for tasks to finish and queue to be empty...");
            try {
                Thread.sleep(2000); // Check every 500 ms
            } catch (InterruptedException e) {
                throw new RuntimeException("Interrupted while waiting for tasks to complete", e);
            }
        }

        executor.shutdownNow();
    }

    private void subscribeToReconnectEvents(EventBus eventBus) {
        Flux<Event> events = eventBus.get();

        events.filter(event -> event instanceof ReconnectAttemptEvent).subscribe(event -> {
            ReconnectAttemptEvent reconnectEvent = (ReconnectAttemptEvent) event;
            ConnectionKey connectionKey = ReconnectEventHelper.connectionKey(reconnectEvent);

            metricsReporter.incrementReconnectAttempt(connectionKey);
        });

        events.filter(event -> event instanceof ReconnectFailedEvent).subscribe(event -> {
            ReconnectFailedEvent reconnectEvent = (ReconnectFailedEvent) event;
            ConnectionKey connectionKey = ReconnectEventHelper.connectionKey(reconnectEvent);

            metricsReporter.incrementReconnectFailure(connectionKey);
        });
    }

    protected void applyConfig(ClientResources.Builder resourceBuilder, ClientOptionsConfig config) {
        if (config == null) {
            return;
        }

        if (config.getReconnectOptions() != null) {
            applyReconnectOptionsConfig(resourceBuilder, config.getReconnectOptions());
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

}
