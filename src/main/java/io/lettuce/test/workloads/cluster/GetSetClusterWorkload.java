package io.lettuce.test.workloads.cluster;

import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.async.RedisAdvancedClusterAsyncCommands;
import io.lettuce.test.WorkloadOptions;
import io.lettuce.test.workloads.BaseWorkload;

import java.util.Random;

public class GetSetClusterWorkload extends BaseWorkload {

    StatefulRedisClusterConnection<String, String> conn;

    public GetSetClusterWorkload(StatefulRedisClusterConnection<String, String> conn, WorkloadOptions options) {
        super(options);
        this.conn = conn;
    }

    @Override
    public void run() {
        RedisAdvancedClusterAsyncCommands<String, String> cmd = withMetrics(conn.async());
        Random random = new Random();

        String payload = generateRandomString(valueSize);

        for (int i = 0; i < iterationCount; i++) {
            if (random.nextDouble() < getSetRatio) {
                cmd.set("key", payload);
            } else {
                cmd.get("key");
            }
        }
    }

}
