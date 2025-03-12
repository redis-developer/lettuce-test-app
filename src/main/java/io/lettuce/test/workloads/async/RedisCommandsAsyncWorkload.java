package io.lettuce.test.workloads.async;

import io.lettuce.core.LettuceFutures;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.test.CommonWorkloadOptions;
import io.lettuce.test.util.PayloadUtils;
import io.lettuce.test.workloads.BaseWorkload;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class RedisCommandsAsyncWorkload extends BaseWorkload {

    private final StatefulRedisConnection<String, String> conn;

    public RedisCommandsAsyncWorkload(StatefulRedisConnection<String, String> conn, CommonWorkloadOptions options) {
        super(options);
        this.conn = conn;
    }

    @Override
    public void run() {
        List<RedisFuture<?>> futures = new ArrayList<>();

        RedisAsyncCommands<String, String> cmd = withMetrics(conn.async());
        String payload = PayloadUtils.randomString(options().valueSize());

        for (int i = 0; i < options().iterationCount(); i++) {
            String key = keyGenerator().nextKey();
            futures.add(cmd.set(key, payload));
            futures.add(cmd.get(key));
            futures.add(cmd.del(key));
            futures.add(cmd.incr("counter"));

            List<String> payloads = new ArrayList<>();
            for (int j = 0; j < options().elementsCount(); j++) {
                payloads.add(payload);
            }
            futures.add(cmd.lpush(key + "list", payloads.toArray(new String[0])));
            futures.add(cmd.lrange(key + "list", 0, -1));

            delay(options().delayAfterIteration());
        }

        if (options().getBoolean("awaitAllResponses", true)) {
            LettuceFutures.awaitAll(1, TimeUnit.MINUTES, futures.toArray(new RedisFuture[futures.size()]));
            futures.clear();
        }
    }

}
