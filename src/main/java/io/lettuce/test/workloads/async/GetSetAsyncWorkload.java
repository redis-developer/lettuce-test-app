package io.lettuce.test.workloads.async;

import io.lettuce.core.LettuceFutures;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.test.WorkloadOptions;
import io.lettuce.test.workloads.BaseWorkload;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class GetSetAsyncWorkload extends BaseWorkload {

    private final StatefulRedisConnection<String, String> conn;

    public GetSetAsyncWorkload(StatefulRedisConnection<String, String> conn, WorkloadOptions options) {
        super(options);
        this.conn = conn;
    }

    @Override
    public void run() {
        List<RedisFuture<String>> futures = new ArrayList<>();

        RedisAsyncCommands<String, String> cmd = withMetrics(conn.async());
        Random random = new Random();

        String payload = generateRandomString(valueSize);

        for (int i = 0; i < iterationCount; i++) {
            if (random.nextDouble() < getSetRatio) {
                futures.add(cmd.set("key", payload));
            } else {
                futures.add(cmd.get("key"));
            }
        }

        LettuceFutures.awaitAll(1, TimeUnit.MINUTES, futures.toArray(new RedisFuture[futures.size()]));
    }

}
