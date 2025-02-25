package io.lettuce.test.workloads.async;

import io.lettuce.core.LettuceFutures;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.test.workloads.BaseWorkload;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class GetSetAsyncWorkload extends BaseWorkload {

    StatefulRedisConnection<String, String> conn;

    public GetSetAsyncWorkload(StatefulRedisConnection<String, String> conn) {
        this.conn = conn;
    }

    @Override
    public void run() {
        List<RedisFuture<String>> futures = new ArrayList<>();

        RedisAsyncCommands<String, String> cmd =  withMetrics(conn.async());
        for (int i = 0; i < 100; i++) {
            futures.add(cmd.set("key" + i, "value" + i));
            futures.add(cmd.get("key" + i));
        }

        LettuceFutures.awaitAll(1, TimeUnit.MINUTES, futures.toArray(new RedisFuture[futures.size()]));
    }

}