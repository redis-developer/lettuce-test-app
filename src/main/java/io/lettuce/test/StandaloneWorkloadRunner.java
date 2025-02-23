package io.lettuce.test;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.metrics.MicrometerCommandLatencyRecorder;
import io.lettuce.core.metrics.MicrometerOptions;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.test.workloads.BaseWorkload;
import io.lettuce.test.workloads.GetSetWorkload;
import io.lettuce.test.workloads.MultiWorkload;
import io.lettuce.test.workloads.PubSubWorkload;
import io.lettuce.test.workloads.RedisCommandsWorkload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StandaloneWorkloadRunner extends WorkloadRunnerBase<RedisClient, StatefulRedisConnection<String, String>> {

    private static final Logger logger = LoggerFactory.getLogger(StandaloneWorkloadRunner.class);

    public StandaloneWorkloadRunner(Config config) {
        super(config);
    }

    @Override
    protected BaseWorkload createWorkload(RedisClient client, StatefulRedisConnection<String, String> connection,
            Config config) {
        return switch (config.test.workload.getType()) {
            case "redis_commands" -> new RedisCommandsWorkload(connection);
            case "get_set" -> new GetSetWorkload(connection);
            case "multi" -> new MultiWorkload(connection);
            case "pub_sub" -> new PubSubWorkload(client);
            default -> throw new IllegalArgumentException(
                    "Invalid workload specified for standalone mode." + config.test.workload.getType());
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