package io.lettuce.test;

import io.lettuce.core.RedisURI;
import io.lettuce.test.workloads.BaseWorkload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public abstract class BaseWorkloadRunner implements AutoCloseable{
  private static final long SHUTDOWN_DELAY = Duration.ofSeconds(10).toMillis();
  private static final Logger log = LoggerFactory.getLogger(BaseWorkloadRunner.class);
  Config config;
  ExecutorService executor;

  public void awaitTermination() {
    // Now, wait for the tasks to complete (plus the additional delay after last task)
    try {
      long maxTime = config.test.workload.getMaxDuration().toSeconds();
      boolean completed = executor.awaitTermination(maxTime + SHUTDOWN_DELAY, TimeUnit.SECONDS);
      if (!completed) {
        log.error("Tasks did not complete within the expected time. Forcing shutdown.");
        executor.shutdownNow();
      }
    } catch (InterruptedException e) {
      log.error("Waiting for tasks to complete was interrupted");
      Thread.currentThread().interrupt();
    } finally {
      executor.shutdown();
    }
  }

  static class Workloads extends ArrayList<WorkloadWrapper> {

    public void stop() {
      for (WorkloadWrapper workload : this) {
        workload.stop();
      }
    }
  }

  Workloads workloads = new Workloads();

  public BaseWorkloadRunner(Config config) {
    this.config = config;
    executor = Executors.newFixedThreadPool(config.test.threads);
  }

  protected Future<?> submit(BaseWorkload task) {
    WorkloadWrapper workload = new WorkloadWrapper(task, config.test.workload);

    Future<?> future = executor.submit(workload);
    workloads.add(workload);
    return future;
  }

  protected RedisURI buildRedisUri(Config config) {
    RedisURI.Builder builder = RedisURI.builder();
    builder.withHost(config.redis.host)
        .withPort(config.redis.port)
        .withDatabase(config.redis.database)
        .withSsl(config.redis.useTls)
        .withVerifyPeer(config.redis.verifyPeer);

    if ( config.redis.password != null) {
      builder.withPassword(config.redis.password.toCharArray());
    }

    if ( config.redis.clientName != null) {
      builder.withClientName(config.redis.clientName);
    }

    if ( config.redis.timeout != null){
      builder.withTimeout(Duration.ofMillis(config.redis.timeout));
    }

    return builder.build();
  }

  public abstract void run();

  public void close() {
    workloads.stop();

    if (executor != null) {
      executor.shutdown();
      try {
        if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
          executor.shutdownNow();
        }
      } catch (InterruptedException e) {
        executor.shutdownNow();
      }
    }
  }
}