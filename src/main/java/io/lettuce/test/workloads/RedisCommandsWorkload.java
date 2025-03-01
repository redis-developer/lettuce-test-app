package io.lettuce.test.workloads;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.test.WorkloadOptions;
import io.lettuce.test.util.TestPayloadUtils;

import java.util.ArrayList;
import java.util.List;

public class RedisCommandsWorkload extends BaseWorkload {

    private final StatefulRedisConnection<String, String> conn;

    public RedisCommandsWorkload(StatefulRedisConnection<String, String> conn, WorkloadOptions options) {
        super(options);
        this.conn = conn;
    }

    @Override
    public void run() {
        RedisCommands<String, String> cmd = withMetrics(conn.sync());
        String payload = TestPayloadUtils.randomString(valueSize);
        for (int j = 0; j < iterationCount; j++) {
            cmd.set("my_key", payload);
            cmd.get("my_key");
            cmd.del("my_key");
            cmd.incr("counter");

            List<String> payloads = new ArrayList<>();
            for (int i = 0; i < elementsCount; i++) {
                payloads.add(payload);
            }
            cmd.lpush("my_list", payloads.toArray(new String[0]));
            cmd.lrange("my_list", 0, -1);
        }
    }

}
