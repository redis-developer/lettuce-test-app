package io.lettuce.test.workloads;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;

public class GetSetWorkload extends BaseWorkload {

    StatefulRedisConnection<String, String> conn;

    public GetSetWorkload(StatefulRedisConnection<String, String> conn) {
        this.conn = conn;
    }

    @Override
    public void run() {
        RedisCommands<String, String> cmd =  withMetrics(conn.sync());
        cmd.set("key", "value");
        cmd.get("key");
    }

}