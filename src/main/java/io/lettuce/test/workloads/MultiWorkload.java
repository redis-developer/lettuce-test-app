package io.lettuce.test.workloads;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;

public class MultiWorkload extends BaseWorkload {

    StatefulRedisConnection<String, String> conn;

    public MultiWorkload(StatefulRedisConnection<String, String> conn) {
        this.conn = conn;
    }

    @Override
    public void run() {
        RedisCommands<String, String> cmd = withMetrics(conn.sync());
        cmd.multi();
        for (int i = 0; i < 5; i++) {
            cmd.set("key" + i, "value" + i);
            cmd.get("key" + i);
        }

        cmd.exec();
    }

}
