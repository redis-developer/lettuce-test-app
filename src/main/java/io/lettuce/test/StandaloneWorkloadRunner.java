package io.lettuce.test;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.test.Config.WorkloadConfig;
import io.lettuce.test.workloads.BaseWorkload;
import io.lettuce.test.workloads.GetSetWorkload;
import io.lettuce.test.workloads.MultiWorkload;
import io.lettuce.test.workloads.PubSubWorkload;
import io.lettuce.test.workloads.RedisCommandsWorkload;
import io.lettuce.test.workloads.async.GetSetAsyncWorkload;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StandaloneWorkloadRunner extends WorkloadRunnerBase<RedisClient, StatefulRedisConnection<String, String>> {

    private static final Logger logger = LoggerFactory.getLogger(StandaloneWorkloadRunner.class);

    public StandaloneWorkloadRunner(Config config, MeterRegistry meterRegistry) {
        super(config, meterRegistry);
    }

    @Override
    protected BaseWorkload createWorkload(RedisClient client, StatefulRedisConnection<String, String> connection,
            WorkloadConfig config) {
        return switch (config.type) {
            case "redis_commands" -> new RedisCommandsWorkload(connection);
            case "get_set" -> new GetSetWorkload(connection, WorkloadOptions.create(config.options));
            case "multi" -> new MultiWorkload(connection);
            case "pub_sub" -> new PubSubWorkload(client);
            // async
            case "get_set_async" -> new GetSetAsyncWorkload(connection, WorkloadOptions.create(config.options));
            default -> throw new IllegalArgumentException("Invalid workload specified for standalone mode." + config.type);
        };
    }

    @Override
    protected RedisClient createClient(RedisURI redisUri, Config config) {

        RedisClient client = RedisClient.create(redisUri);

        ClientOptions clientOptions = createClientOptions(config.clientOptions);
        client.setOptions(clientOptions);

        return client;
    }

    @Override
    protected StatefulRedisConnection<String, String> createConnection(RedisClient client, Config config) {
        return client.connect();
    }

}
