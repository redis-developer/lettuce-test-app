package io.lettuce.test;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.push.PushListener;
import io.lettuce.core.api.push.PushMessage;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.event.EventBus;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.sync.RedisPubSubCommands;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.test.config.WorkloadRunnerConfig;
import io.lettuce.test.config.WorkloadRunnerConfig.WorkloadConfig;
import io.lettuce.test.metrics.MetricsReporter;
import io.lettuce.test.workloads.BaseWorkload;
import io.lettuce.test.workloads.GetSetWorkload;
import io.lettuce.test.workloads.MultiWorkload;
import io.lettuce.test.workloads.PubSubWorkload;
import io.lettuce.test.workloads.RedisCommandsWorkload;
import io.lettuce.test.workloads.async.GetSetAsyncWorkload;
import io.lettuce.test.workloads.async.IncrementAsyncWorkload;
import io.lettuce.test.workloads.async.RedisCommandsAsyncWorkload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.stream.Collectors;

public class ProactiveUpgradeWorkloadRunner
        extends WorkloadRunnerBase<RedisClient, StatefulRedisPubSubConnection<String, String>> {

    private static final Logger logger = LoggerFactory.getLogger(ProactiveUpgradeWorkloadRunner.class);

    public ProactiveUpgradeWorkloadRunner(WorkloadRunnerConfig config, MetricsReporter metricsReporter) {
        super(config, metricsReporter);

    }

    @Override
    protected BaseWorkload createWorkload(RedisClient client, StatefulRedisPubSubConnection<String, String> connection,
            WorkloadConfig config) {
        CommonWorkloadOptions options = DefaultWorkloadOptions.create(config.getOptions());
        return switch (config.getType()) {
            case "redis_commands" -> new RedisCommandsWorkload(connection, options);
            case "get_set" -> new GetSetWorkload(connection, options);
            case "multi" -> new MultiWorkload(connection, options);
            case "pub_sub" -> new PubSubWorkload(client, options);
            // async
            case "get_set_async" -> new GetSetAsyncWorkload(connection, options);
            case "redis_commands_async" -> new RedisCommandsAsyncWorkload(connection, options);
            case "increment_async" -> new IncrementAsyncWorkload(connection, options);
            default -> throw new IllegalArgumentException("Invalid workload specified for standalone mode." + config.getType());
        };
    }

    @Override
    protected RedisClient createClient(RedisURI redisUri, WorkloadRunnerConfig config) {
        ClientResources.Builder resourceBuilder = ClientResources.builder();
        applyConfig(resourceBuilder, config.getClientOptions());

        ClientResources resources = resourceBuilder.build();

        RedisClient client = RedisClient.create(resources, redisUri);

        EventBus eventBus = client.getResources().eventBus();
        eventBus.get().subscribe(e -> {
            logger.info(">>> Connection event: " + e);
        });

        ClientOptions clientOptions = createClientOptions(config.getClientOptions());
        client.setOptions(clientOptions);

        return client;
    }

    @Override
    protected StatefulRedisPubSubConnection<String, String> createConnection(RedisClient client, WorkloadRunnerConfig config) {

        // Subscribe to __rebind channel
        StatefulRedisPubSubConnection<String, String> connection = client.connectPubSub();
        RedisPubSubCommands<String, String> commands = connection.sync();
        commands.subscribe("__rebind");

        return connection;
    }

    static class Control implements PushListener {

        public boolean shouldContinue = true;

        @Override
        public void onPushMessage(PushMessage message) {
            List<String> content = message.getContent().stream().map(ez -> StringCodec.UTF8.decodeKey((ByteBuffer) ez))
                    .collect(Collectors.toList());

            if (content.stream().anyMatch(c -> c.equals("type=stop_demo"))) {
                logger.info("Control received message to stop the demo");
                shouldContinue = false;
            }
        }

    }

}
