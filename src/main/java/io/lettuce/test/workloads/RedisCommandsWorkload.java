package io.lettuce.test.workloads;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.test.CommonWorkloadOptions;
import io.lettuce.test.util.PayloadUtils;

import java.util.ArrayList;
import java.util.List;

public class RedisCommandsWorkload extends BaseWorkload {

    private final StatefulRedisConnection<String, String> conn;

    public RedisCommandsWorkload(StatefulRedisConnection<String, String> conn, CommonWorkloadOptions options) {
        super(options);
        this.conn = conn;
    }

    @Override
    public void run() {
        RedisCommands<String, String> cmd = withMetrics(conn.sync());
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

            if (options().elementsCount() < 0) {
                cmd.lpush(key + "list", payloads.toArray(new String[0]));
                cmd.lrange(key + "list", 0, -1);
                cmd.ltrim(key + "list", 0, options().elementsCount());
            }

            delay(options().delayAfterIteration());
        }
    }

}
