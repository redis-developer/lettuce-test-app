package io.lettuce.test;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.test.Config.WorkloadConfig;
import io.lettuce.test.metrics.MetricsReporter;
import io.lettuce.test.workloads.BaseWorkload;
import io.lettuce.test.workloads.cluster.GetSetClusterWorkload;

public class ClusterWorkloadRunner
        extends WorkloadRunnerBase<RedisClusterClient, StatefulRedisClusterConnection<String, String>> {

    public ClusterWorkloadRunner(Config config, MetricsReporter metricsReporter) {
        super(config, metricsReporter);
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
        WorkloadOptions options = WorkloadOptions.create(config.options);
        return switch (config.type) {
            case "get_set" -> new GetSetClusterWorkload(connection, options);
            default -> throw new IllegalArgumentException("Unsupported workload." + config.type);
        };
    }

}
