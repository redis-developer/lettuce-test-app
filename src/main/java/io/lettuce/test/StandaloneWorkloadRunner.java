package io.lettuce.test;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.test.Config.WorkloadConfig;
import io.lettuce.test.metrics.MetricsReporter;
import io.lettuce.test.workloads.BaseWorkload;
import io.lettuce.test.workloads.GetSetWorkload;
import io.lettuce.test.workloads.MultiWorkload;
import io.lettuce.test.workloads.PubSubWorkload;
import io.lettuce.test.workloads.RedisCommandsWorkload;
import io.lettuce.test.workloads.async.GetSetAsyncWorkload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StandaloneWorkloadRunner extends WorkloadRunnerBase<RedisClient, StatefulRedisConnection<String, String>> {

    private static final Logger logger = LoggerFactory.getLogger(StandaloneWorkloadRunner.class);

    public StandaloneWorkloadRunner(Config config, MetricsReporter metricsReporter) {
        super(config, metricsReporter);
    }

    @Override
    protected BaseWorkload createWorkload(RedisClient client, StatefulRedisConnection<String, String> connection,
            WorkloadConfig config) {
        WorkloadOptions options = WorkloadOptions.create(config.options);
        return switch (config.type) {
            case "redis_commands" -> new RedisCommandsWorkload(connection, options);
            case "get_set" -> new GetSetWorkload(connection,options);
            case "multi" -> new MultiWorkload(connection, options);
            case "pub_sub" -> new PubSubWorkload(client, options);
            // async
            case "get_set_async" -> new GetSetAsyncWorkload(connection, options);
            default -> throw new IllegalArgumentException("Invalid workload specified for standalone mode." + config.type);
        };
    }

    @Override
    protected RedisClient createClient(RedisURI redisUri, Config config) {
        ClientResources.Builder resourceBuilder = ClientResources.builder();
        applyConfig(resourceBuilder, config);
        ClientResources resources = resourceBuilder.build();

        RedisClient client = RedisClient.create(resources, redisUri);

        ClientOptions clientOptions = createClientOptions(config.clientOptions);
        client.setOptions(clientOptions);

        return client;
    }

    @Override
    protected StatefulRedisConnection<String, String> createConnection(RedisClient client, Config config) {
        return client.connect();
    }

}
