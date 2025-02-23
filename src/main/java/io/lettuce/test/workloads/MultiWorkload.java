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
        RedisCommands<String, String> sync = conn.sync();
        sync.multi();
        for (int i = 0;i < 5; i++) {
            sync.set("key"+i, "value" + i);
            sync.get("key"+i);
        }

        sync.exec();
    }

}