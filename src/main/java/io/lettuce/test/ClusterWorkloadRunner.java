package io.lettuce.test;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.test.config.WorkloadRunnerConfig;
import io.lettuce.test.config.WorkloadRunnerConfig.WorkloadConfig;
import io.lettuce.test.metrics.MetricsReporter;
import io.lettuce.test.workloads.BaseWorkload;
import io.lettuce.test.workloads.cluster.GetSetClusterWorkload;

public class ClusterWorkloadRunner
        extends WorkloadRunnerBase<RedisClusterClient, StatefulRedisClusterConnection<String, String>> {

    public ClusterWorkloadRunner(WorkloadRunnerConfig config, MetricsReporter metricsReporter) {
        super(config, metricsReporter);
    }

    @Override
    protected RedisClusterClient createClient(RedisURI redisUri, WorkloadRunnerConfig config) {
        ClientResources.Builder resourceBuilder = ClientResources.builder();
        applyConfig(resourceBuilder, config.getClientOptions());
        ClientResources resources = resourceBuilder.build();

        RedisClusterClient clusterClient = RedisClusterClient.create(resources, redisUri);

        ClientOptions clientOptions = createClientOptions(config.getClientOptions());
        ClusterClientOptions.Builder builder = ClusterClientOptions.builder(clientOptions);
        clusterClient.setOptions(builder.build());

        return clusterClient;
    }

    @Override
    protected StatefulRedisClusterConnection<String, String> createConnection(RedisClusterClient client,
            WorkloadRunnerConfig config) {
        return client.connect();
    }

    @Override
    protected BaseWorkload createWorkload(RedisClusterClient client, StatefulRedisClusterConnection<String, String> connection,
            WorkloadConfig config) {
        CommonWorkloadOptions options = DefaultWorkloadOptions.create(config.getOptions());
        return switch (config.getType()) {
            case "get_set" -> new GetSetClusterWorkload(connection, options);
            default -> throw new IllegalArgumentException("Unsupported workload." + config.getType());
        };
    }

}
