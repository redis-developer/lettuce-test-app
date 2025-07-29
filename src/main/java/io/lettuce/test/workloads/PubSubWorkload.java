package io.lettuce.test.workloads;

import io.lettuce.core.RedisClient;
import io.lettuce.core.pubsub.RedisPubSubListener;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.sync.RedisPubSubCommands;
import io.lettuce.test.CommonWorkloadOptions;
import io.lettuce.test.metrics.MetricsReporter.PubSubOpKey;
import io.lettuce.test.metrics.MetricsReporter.PubSubOperation;
import io.lettuce.test.metrics.OperationStatus;

import java.util.UUID;

public class PubSubWorkload extends BaseWorkload {

    RedisClient client;

    public PubSubWorkload(RedisClient client, CommonWorkloadOptions options) {
        super(options);
        this.client = client;
    }

    @Override
    public void run() {

        StatefulRedisPubSubConnection<String, String> pubSubConn = client.connectPubSub();
        pubSubConn.addListener(new RedisPubSubListener<>() {

            final String subscriberId = UUID.randomUUID().toString();

            @Override
            public void message(String channel, String message) {
                PubSubOpKey pubSubOpKey = new PubSubOpKey(channel, PubSubOperation.RECEIVE, subscriberId,
                        OperationStatus.SUCCESS);
                metricsReporter.incrementPubSubOperation(pubSubOpKey);
            }

            @Override
            public void message(String pattern, String channel, String message) {
                PubSubOpKey pubSubOpKey = new PubSubOpKey(channel, PubSubOperation.RECEIVE, subscriberId,
                        OperationStatus.SUCCESS);
                metricsReporter.incrementPubSubOperation(pubSubOpKey);
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
        String myChannel = "my_channel";
        try {
            cmd.subscribe(myChannel);
            PubSubOpKey pubSubOpKey = new PubSubOpKey(myChannel, PubSubOperation.SUBSCRIBE, pubSubConn.toString(),
                    OperationStatus.SUCCESS);
            metricsReporter.incrementPubSubOperation(pubSubOpKey);
        } catch (Exception e) {
            PubSubOpKey pubSubOpKey = new PubSubOpKey(myChannel, PubSubOperation.SUBSCRIBE, pubSubConn.toString(),
                    OperationStatus.ERROR);
            metricsReporter.incrementPubSubOperation(pubSubOpKey);
            throw e;
        }

        while (!Thread.currentThread().isInterrupted()) {
            RedisPubSubCommands<String, String> pubSubCommands = withMetrics(pubSubConn.sync());
            try {
                pubSubCommands.publish(myChannel, "Test Message");
                PubSubOpKey pubSubOpKey = new PubSubOpKey(myChannel, PubSubOperation.PUBLISH, pubSubConn.toString(),
                        OperationStatus.SUCCESS);
                metricsReporter.incrementPubSubOperation(pubSubOpKey);
            } catch (Exception e) {
                PubSubOpKey pubSubOpKey = new PubSubOpKey(myChannel, PubSubOperation.PUBLISH, pubSubConn.toString(),
                        OperationStatus.ERROR);
                metricsReporter.incrementPubSubOperation(pubSubOpKey);
                throw e;
            }

            delay(options().delayAfterIteration());
        }
    }

}
