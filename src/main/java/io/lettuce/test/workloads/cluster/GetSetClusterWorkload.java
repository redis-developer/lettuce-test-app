package io.lettuce.test.workloads.cluster;


import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.test.workloads.BaseWorkload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GetSetClusterWorkload extends BaseWorkload {
  private static final Logger logger = LoggerFactory.getLogger(GetSetClusterWorkload.class);

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