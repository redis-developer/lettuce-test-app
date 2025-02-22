package io.lettuce.test;

import io.lettuce.test.workloads.GetSetWorkload;
import io.lettuce.test.workloads.MultiWorkload;
import io.lettuce.test.workloads.PubSubWorkload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.lettuce.core.*;
import io.lettuce.core.api.StatefulRedisConnection;

import java.util.ArrayList;
import java.util.List;

public class StandaloneWorkloadRunner extends BaseWorkloadRunner {

  private static final Logger logger = LoggerFactory.getLogger(StandaloneWorkloadRunner.class);

  public StandaloneWorkloadRunner(Config config) {
    super(config);
  }

  public void run() {
    RedisURI redisUri = buildRedisUri(config);

    List<RedisClient> clients = new ArrayList<>();
    List<List<StatefulRedisConnection<String, String>>> connections = new ArrayList<>();

    // Create the specified number of client instances
    for (int i = 0; i < config.test.clients; i++) {
      RedisClient client = RedisClient.create(redisUri);
      client.setOptions(ClientOptions.builder()
          .autoReconnect(config.clientOptions.autoReconnect)
          .pingBeforeActivateConnection(config.clientOptions.pingBeforeActivate)
          // TODO : Add important options
          .build());
      clients.add(client);

      List<StatefulRedisConnection<String, String>> clientConnections = new ArrayList<>();
      for (int j = 0; j < config.test.connectionsPerClient; j++) {
        clientConnections.add(clients.get(j).connect());
      }
      connections.add(clientConnections);
    }

    for (int i = 0; i < config.test.clients; i++) {
      for (StatefulRedisConnection<String, String> conn : connections.get(i)) {
        switch (config.test.workload.getType()) {
        case "get_set":
          submit(new GetSetWorkload(conn));
          break;
        case "multi":
          submit(new MultiWorkload(conn));
          break;
        case "pub_sub":
          RedisClient client = clients.get(i);
          submit(new PubSubWorkload(client));
          break;
        default:
          logger.error("Invalid workload specified.");
        }
      }
    }
  }
}