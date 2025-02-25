package io.lettuce.test;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.test.Config.WorkloadConfig;
import io.lettuce.test.workloads.BaseWorkload;
import io.lettuce.test.workloads.cluster.GetSetClusterWorkload;
import io.micrometer.core.instrument.MeterRegistry;

public class ClusterWorkloadRunner
        extends WorkloadRunnerBase<RedisClusterClient, StatefulRedisClusterConnection<String, String>> {

    public ClusterWorkloadRunner(Config config, MeterRegistry meterRegistry) {
        super(config, meterRegistry);
    }

    @Override
    protected RedisClusterClient createClient(RedisURI redisUri, Config config) {
        RedisClusterClient clusterClient = RedisClusterClient.create(redisUri);

        ClientOptions clientOptions = createClientOptions(config.clientOptions);
        ClusterClientOptions.Builder builder = ClusterClientOptions.builder(clientOptions);
        clusterClient.setOptions(builder.build());

        return clusterClient;
    }

    @Override
    protected StatefulRedisClusterConnection<String, String> createConnection(RedisClusterClient client, Config config) {
        return client.connect();
    }

    @Override
    protected BaseWorkload createWorkload(RedisClusterClient client, StatefulRedisClusterConnection<String, String> connection,
            WorkloadConfig config) {
        return switch (config.getType()) {
            case "get_set" -> new GetSetClusterWorkload(connection);
            default -> throw new IllegalArgumentException("Unsupported workload." + config.getType());
        };
    }

}