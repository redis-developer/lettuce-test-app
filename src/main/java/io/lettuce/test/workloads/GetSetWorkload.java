package io.lettuce.test.workloads;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.test.WorkloadOptions;

import java.util.Random;

public class GetSetWorkload extends BaseWorkload {

    private final StatefulRedisConnection<String, String> conn;

    private final double getSetRatio;

    private final int valueSize;

    private final int operationCount;

    public GetSetWorkload(StatefulRedisConnection<String, String> conn, WorkloadOptions options) {
        super(options);
        this.conn = conn;

        this.getSetRatio = options().getDouble("getSetRatio");
        this.valueSize = options().getInteger("valueSize");
        this.operationCount = options().getInteger("operationCount");
    }

    @Override
    public void run() {
        RedisCommands<String, String> cmd = withMetrics(conn.sync());
        Random random = new Random();

        String payload = generateRandomString(valueSize);

        for (int i = 0; i < operationCount; i++) {
            if (random.nextDouble() < getSetRatio) {
                cmd.set("key", payload + ":" + i);
            } else {
                cmd.get("key");
            }
        }
    }

}
