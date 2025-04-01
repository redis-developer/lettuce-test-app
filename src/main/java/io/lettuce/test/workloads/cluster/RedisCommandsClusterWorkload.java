package io.lettuce.test.workloads.cluster;

import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.sync.RedisClusterCommands;
import io.lettuce.test.CommonWorkloadOptions;
import io.lettuce.test.util.PayloadUtils;
import io.lettuce.test.workloads.BaseWorkload;

import java.util.ArrayList;
import java.util.List;

public class RedisCommandsClusterWorkload extends BaseWorkload {

    private final StatefulRedisClusterConnection<String, String> conn;

    public RedisCommandsClusterWorkload(StatefulRedisClusterConnection<String, String> conn, CommonWorkloadOptions options) {
        super(options);
        this.conn = conn;
    }

    @Override
    public void run() {
        RedisClusterCommands<String, String> cmd = withMetrics(conn.sync());
        String payload = PayloadUtils.randomString(options().valueSize());

        for (int i = 0; i < options().iterationCount(); i++) {
            String key = keyGenerator().nextKey();
            cmd.set(key, payload);
            cmd.get(key);
            cmd.del(key);
            cmd.incr("counter");

            List<String> payloads = new ArrayList<>();
            for (int j = 0; j < options().elementsCount(); j++) {
                payloads.add(payload);
            }

            if (options().elementsCount() > 0) {
                cmd.lpush(key + "list", payloads.toArray(new String[0]));
                cmd.lrange(key + "list", 0, -1);
                cmd.ltrim(key + "list", 0, options().elementsCount());
            }

            delay(options().delayAfterIteration());
        }
    }

}
