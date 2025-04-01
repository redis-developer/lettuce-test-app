package io.lettuce.test.workloads.cluster;

import io.lettuce.core.LettuceFutures;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.async.RedisAdvancedClusterAsyncCommands;
import io.lettuce.test.CommonWorkloadOptions;
import io.lettuce.test.util.PayloadUtils;
import io.lettuce.test.workloads.BaseWorkload;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class GetSetAsyncClusterWorkload extends BaseWorkload {

    StatefulRedisClusterConnection<String, String> conn;

    public GetSetAsyncClusterWorkload(StatefulRedisClusterConnection<String, String> conn, CommonWorkloadOptions options) {
        super(options);
        this.conn = conn;
    }

    @Override
    public void run() {
        List<RedisFuture<String>> futures = new ArrayList<>();

        RedisAdvancedClusterAsyncCommands<String, String> cmd = withMetrics(conn.async());
        Random random = new Random();

        String payload = PayloadUtils.randomString(options().valueSize());

        for (int i = 0; i < options().iterationCount(); i++) {
            String key = keyGenerator().nextKey();
            if (random.nextDouble() < options().getSetRatio()) {
                futures.add(cmd.set(key, payload));
            } else {
                futures.add(cmd.get(key));
            }

            delay(options().delayAfterIteration());
        }

        if (options().getBoolean("awaitAllResponses", true)) {
            LettuceFutures.awaitAll(1, TimeUnit.MINUTES, futures.toArray(new RedisFuture[futures.size()]));
        }
    }

}
