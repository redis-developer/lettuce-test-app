package io.lettuce.test.workloads;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;

public class RedisCommandsWorkload extends BaseWorkload {

    StatefulRedisConnection<String, String> conn;

    public RedisCommandsWorkload(StatefulRedisConnection<String, String> conn) {
        this.conn = conn;
    }

    @Override
    public void run() {
        RedisCommands<String, String> cmd =  withMetrics(conn.sync());
        cmd.set("my_key", "Hello Redis!");
        cmd.get("my_key");
        cmd.del("my_key");
        cmd.incr("counter");
        cmd.lpush("my_list", "value1");
        cmd.lrange("my_list", 0, -1);
    }

}