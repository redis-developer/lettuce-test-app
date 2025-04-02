package io.lettuce.test;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions.RefreshTrigger;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.test.config.WorkloadRunnerConfig;
import io.lettuce.test.config.WorkloadRunnerConfig.ClusterClientOptionsConfig;
import io.lettuce.test.config.WorkloadRunnerConfig.ClusterClientOptionsConfig.ClusterTopologyRefreshOptionsConfig;
import io.lettuce.test.config.WorkloadRunnerConfig.ClusterClientOptionsConfig.ClusterTopologyRefreshOptionsConfig.AdaptiveRefreshConfig;
import io.lettuce.test.config.WorkloadRunnerConfig.WorkloadConfig;
import io.lettuce.test.metrics.MetricsReporter;
import io.lettuce.test.workloads.BaseWorkload;
import io.lettuce.test.workloads.cluster.GetSetAsyncClusterWorkload;
import io.lettuce.test.workloads.cluster.GetSetClusterWorkload;
import io.lettuce.test.workloads.cluster.PubSubClusterWorkload;
import io.lettuce.test.workloads.cluster.RedisCommandsClusterAsyncWorkload;
import io.lettuce.test.workloads.cluster.RedisCommandsClusterWorkload;
import io.lettuce.test.workloads.cluster.SetInSlotClusterWorkload;

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
        applyConfig(builder, config.getClusterClientOptions());

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
            case "get_set_async" -> new GetSetAsyncClusterWorkload(connection, options);
            case "pub_sub" -> new PubSubClusterWorkload(client, options);
            case "redis_commands" -> new RedisCommandsClusterWorkload(connection, options);
            case "redis_commands_async" -> new RedisCommandsClusterAsyncWorkload(connection, options);
            case "set_in_slot" -> new SetInSlotClusterWorkload(connection, options);
            default -> throw new IllegalArgumentException("Unsupported workload." + config.getType());
        };
    }

    private void applyConfig(ClusterClientOptions.Builder builder, ClusterClientOptionsConfig config) {
        if (config != null) {
            if (config.getTopologyRefreshOptions() != null) {
                applyTopologyRefreshOptions(builder, config.getTopologyRefreshOptions());
            }
        }
    }

    private void applyTopologyRefreshOptions(ClusterClientOptions.Builder builder, ClusterTopologyRefreshOptionsConfig config) {
        ClusterTopologyRefreshOptions.Builder topologyRefreshOptions = ClusterTopologyRefreshOptions.builder();

        if (config.getAdaptive() != null) {
            AdaptiveRefreshConfig adaptive = config.getAdaptive();
            if (adaptive.isEnabled()) {
                if (adaptive.getRefreshTriggers().stream().anyMatch(e -> e.equalsIgnoreCase("ALL"))) {
                    topologyRefreshOptions.enableAllAdaptiveRefreshTriggers();
                } else {
                    RefreshTrigger[] triggers = adaptive.getRefreshTriggers().stream().map(RefreshTrigger::valueOf)
                            .toArray(RefreshTrigger[]::new);
                    topologyRefreshOptions.enableAdaptiveRefreshTrigger(triggers);
                }
                if (adaptive.getTriggersTimeout() != null) {
                    topologyRefreshOptions.adaptiveRefreshTriggersTimeout(adaptive.getTriggersTimeout());
                }
            }
        }
        builder.topologyRefreshOptions(topologyRefreshOptions.build());
    }

}
