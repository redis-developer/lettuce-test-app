package io.lettuce.test.workloads;

import io.lettuce.core.RedisClient;
import io.lettuce.core.pubsub.RedisPubSubListener;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PubSubWorkload extends BaseWorkload {

    private static final Logger log = LoggerFactory.getLogger(PubSubWorkload.class);

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
                log.debug("Received message: " + message);
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

        pubSubConn.sync().subscribe("my_channel");

        while (!Thread.currentThread().isInterrupted()) {
            pubSubConn.sync().publish("my_channel", "Test Message");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

}