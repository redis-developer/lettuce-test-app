package io.lettuce.test.workloads;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.test.CommonWorkflowOptions;
import io.lettuce.test.util.PayloadUtils;

import java.util.Random;

public class MultiWorkload extends BaseWorkload {

    StatefulRedisConnection<String, String> conn;

    public MultiWorkload(StatefulRedisConnection<String, String> conn, CommonWorkflowOptions options) {
        super(options);
        this.conn = conn;
    }

    @Override
    public void run() {
        RedisCommands<String, String> cmd = withMetrics(conn.sync());
        Random random = new Random();
        String payload = PayloadUtils.randomString(options().valueSize());

        for (int i = 0; i < options().iterationCount(); i++) {
            cmd.multi();
            for (int j = 0; j < options().transactionSize(); j++) { // Use commandCount here
                if (random.nextDouble() < options().getSetRatio()) {
                    cmd.set("key" + j, payload);
                } else {
                    cmd.get("key" + j);
                }
            }
            cmd.exec();
        }
    }

}
