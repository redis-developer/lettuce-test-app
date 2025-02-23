package io.lettuce.test;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.test.workloads.BaseWorkload;
import io.lettuce.test.workloads.GetSetWorkload;
import io.lettuce.test.workloads.MultiWorkload;
import io.lettuce.test.workloads.PubSubWorkload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StandaloneWorkloadRunner extends BaseWorkloadRunner<RedisClient, StatefulRedisConnection<String, String>> {

    private static final Logger logger = LoggerFactory.getLogger(StandaloneWorkloadRunner.class);

    public StandaloneWorkloadRunner(Config config) {
        super(config);
    }

    @Override
    protected BaseWorkload createWorkload(RedisClient client, StatefulRedisConnection<String, String> connection,
            Config config) {
        return switch (config.test.workload.getType()) {
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
        client.setOptions(ClientOptions.builder().autoReconnect(config.clientOptions.autoReconnect)
                .pingBeforeActivateConnection(config.clientOptions.pingBeforeActivate)
                // TODO : Add important options
                .build());
        return client;
    }

    @Override
    protected StatefulRedisConnection<String, String> createConnection(RedisClient client, Config config) {
        return client.connect();
    }

}