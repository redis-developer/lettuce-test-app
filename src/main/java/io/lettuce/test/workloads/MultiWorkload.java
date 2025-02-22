package io.lettuce.test.workloads;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.test.Config;

public class MultiWorkload extends BaseWorkload {
  StatefulRedisConnection<String, String> conn;

  public MultiWorkload(StatefulRedisConnection<String, String> conn) {
    this.conn = conn;
  }

  @Override
  public void run() {

      conn.sync().multi();
      conn.sync().set("key", "value");
      conn.sync().get("key");
      conn.sync().exec();
  }
}