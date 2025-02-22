package io.lettuce.test;

import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.*;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.test.workloads.cluster.GetSetClusterWorkload;

import java.util.ArrayList;
import java.util.List;

public class ClusterWorkloadRunner extends BaseWorkloadRunner{

  public ClusterWorkloadRunner(Config config) {
    super(config);
  }

  public void run() {
    RedisURI redisUri = buildRedisUri(config);

    List<RedisClusterClient> clients = new ArrayList<>();
    List<List<StatefulRedisClusterConnection<String, String>>> connections = new ArrayList<>();

    // Create the specified number of client instances
    for (int i = 0; i < config.test.clients; i++) {
      RedisClusterClient clusterClient = RedisClusterClient.create(redisUri);
      clusterClient.setOptions(ClusterClientOptions.builder()
          .autoReconnect(config.clientOptions.autoReconnect)
          .pingBeforeActivateConnection(config.clientOptions.pingBeforeActivate)
          // TODO : Add more cluster options
          .build());
      clients.add(clusterClient);

      List<StatefulRedisClusterConnection<String, String>> clientConnections = new ArrayList<>();
      for (int j = 0; j < config.test.connectionsPerClient; j++) {
        clientConnections.add(clients.get(j).connect());
      }
      connections.add(clientConnections);
    }

    for (int i = 0; i < config.test.clients; i++) {
      for (StatefulRedisClusterConnection<String, String> conn : connections.get(i)) {
        switch (config.test.workload.getType()) {
        case "get_set":
          submit(new GetSetClusterWorkload(conn));
          break;
        default:
          System.out.println("Unsupported workload for cluster mode.");
        }
      }
    }
  }
}