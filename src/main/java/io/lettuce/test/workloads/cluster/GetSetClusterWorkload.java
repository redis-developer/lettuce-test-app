package io.lettuce.test.workloads.cluster;

import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.async.RedisAdvancedClusterAsyncCommands;
import io.lettuce.test.CommonWorkloadOptions;
import io.lettuce.test.util.PayloadUtils;
import io.lettuce.test.workloads.BaseWorkload;

import java.util.Random;

public class GetSetClusterWorkload extends BaseWorkload {

    StatefulRedisClusterConnection<String, String> conn;

    public GetSetClusterWorkload(StatefulRedisClusterConnection<String, String> conn, CommonWorkloadOptions options) {
        super(options);
        this.conn = conn;
    }

    @Override
    public void run() {
        RedisAdvancedClusterAsyncCommands<String, String> cmd = withMetrics(conn.async());
        Random random = new Random();

        String payload = PayloadUtils.randomString(options().valueSize());

        for (int i = 0; i < options().iterationCount(); i++) {
            if (random.nextDouble() < options().getSetRatio()) {
                cmd.set("key", payload);
            } else {
                cmd.get("key");
            }
        }
    }

}
