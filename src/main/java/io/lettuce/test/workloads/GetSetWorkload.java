package io.lettuce.test.workloads;

import io.lettuce.core.api.StatefulRedisConnection;

public class GetSetWorkload extends BaseWorkload {

    StatefulRedisConnection<String, String> conn;

    public GetSetWorkload(StatefulRedisConnection<String, String> conn) {
        this.conn = conn;
    }

    @Override
    public void run() {
        withLatency(()->conn.sync().set("key", "value"));
        withLatency(()->conn.sync().get("key"));
    }

}