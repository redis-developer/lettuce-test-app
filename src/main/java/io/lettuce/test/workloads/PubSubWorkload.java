package io.lettuce.test.workloads;

import io.lettuce.core.RedisClient;
import io.lettuce.core.pubsub.RedisPubSubListener;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;

public class PubSubWorkload extends BaseWorkload {
  RedisClient client;

  public PubSubWorkload(RedisClient client) {
    this.client = client;
  }

  @Override
  public void run() {

    StatefulRedisPubSubConnection<String, String> pubSubConn = client.connectPubSub();
    pubSubConn.addListener(new RedisPubSubListener<String, String>() {
      @Override
      public void message(String channel, String message) {
        // TODO : Handle incoming message
      }

      @Override
      public void message(String pattern, String channel, String message) {}

      @Override
      public void subscribed(String channel, long count) {}

      @Override
      public void psubscribed(String pattern, long count) {}

      @Override
      public void unsubscribed(String channel, long count) {}

      @Override
      public void punsubscribed(String pattern, long count) {}
    });

    pubSubConn.sync().subscribe("testChannel");

    while (!Thread.currentThread().isInterrupted()) {
      // Simulating Pub/Sub operations
      pubSubConn.sync().publish("testChannel", "message");
    }
  }

}