package io.lettuce.test.workloads;

import io.lettuce.core.api.StatefulRedisConnection;

public class GetSetWorkload extends BaseWorkload {
  StatefulRedisConnection<String, String> conn;

  public GetSetWorkload(StatefulRedisConnection<String, String> conn) {
    this.conn = conn;
  }

  @Override
  public void run() {
      conn.sync().set("key", "value");
      conn.sync().get("key");
  }

}