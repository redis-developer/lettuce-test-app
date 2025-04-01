package io.lettuce.test.workloads.cluster;

import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.pubsub.StatefulRedisClusterPubSubConnection;
import io.lettuce.core.pubsub.RedisPubSubListener;
import io.lettuce.core.pubsub.api.sync.RedisPubSubCommands;
import io.lettuce.test.CommonWorkloadOptions;
import io.lettuce.test.workloads.BaseWorkload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PubSubClusterWorkload extends BaseWorkload {

    private static final Logger log = LoggerFactory.getLogger(PubSubClusterWorkload.class);

    RedisClusterClient client;

    public PubSubClusterWorkload(RedisClusterClient client, CommonWorkloadOptions options) {
        super(options);
        this.client = client;
    }

    @Override
    public void run() {

        StatefulRedisClusterPubSubConnection<String, String> pubSubConn = client.connectPubSub();
        pubSubConn.addListener(new RedisPubSubListener<>() {

            @Override
            public void message(String channel, String message) {
                log.trace("Received message: " + message);
            }

            @Override
            public void message(String pattern, String channel, String message) {
            }

            @Override
            public void subscribed(String channel, long count) {
            }

            @Override
            public void psubscribed(String pattern, long count) {
            }

            @Override
            public void unsubscribed(String channel, long count) {
            }

            @Override
            public void punsubscribed(String pattern, long count) {
            }

        });

        RedisPubSubCommands<String, String> cmd = withMetrics(pubSubConn.sync());
        cmd.subscribe("my_channel");

        while (!Thread.currentThread().isInterrupted()) {
            RedisPubSubCommands<String, String> pubSubCommands = withMetrics(pubSubConn.sync());
            pubSubCommands.publish("my_channel", "Test Message");

            delay(options().delayAfterIteration());
        }
    }

}
