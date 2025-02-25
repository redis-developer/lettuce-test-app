package io.lettuce.test.workloads.cluster;

import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.test.workloads.BaseWorkload;

public class GetSetClusterWorkload extends BaseWorkload {

    StatefulRedisClusterConnection<String, String> conn;

    public GetSetClusterWorkload(StatefulRedisClusterConnection<String, String> conn) {
        this.conn = conn;
    }

    @Override
    public void run() {
        conn.sync().set("key", "value");
        conn.sync().get("key");
    }

}
