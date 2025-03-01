package io.lettuce.test.workloads;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.test.WorkloadOptions;

import java.util.Random;

public class GetSetWorkload extends BaseWorkload {

    private final StatefulRedisConnection<String, String> conn;

    public GetSetWorkload(StatefulRedisConnection<String, String> conn, WorkloadOptions options) {
        super(options);
        this.conn = conn;
    }

    @Override
    public void run() {
        RedisCommands<String, String> cmd = withMetrics(conn.sync());
        Random random = new Random();

        String payload = generateRandomString(valueSize);

        for (int i = 0; i < iterationCount; i++) {
            if (random.nextDouble() < getSetRatio) {
                cmd.set("key", payload + ":" + i);
            } else {
                cmd.get("key");
            }
        }
    }

}
