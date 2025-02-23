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
        RedisCommands<String, String> sync = conn.sync();
        sync.set("my_key", "Hello Redis!");
        sync.get("my_key");
        sync.del("my_key");
        sync.incr("counter");
        sync.lpush("my_list", "value1");
        sync.lrange("my_list", 0, -1);
    }

}