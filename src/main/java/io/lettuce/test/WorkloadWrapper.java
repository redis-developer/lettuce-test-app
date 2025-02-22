package io.lettuce.test;

import io.lettuce.test.workloads.BaseWorkload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class WorkloadWrapper implements Runnable {

  protected static final Logger log = LoggerFactory.getLogger(WorkloadWrapper.class);
  private final BaseWorkload workload;

  private List<Exception> exceptions = new ArrayList<>();
  private final AtomicBoolean running = new AtomicBoolean(true);
  private final AtomicInteger iterationCount = new AtomicInteger(0);
  protected final Config.WorkloadConfig config;

  public WorkloadWrapper(BaseWorkload workload, Config.WorkloadConfig config) {
    this.config = config;
    this.workload = workload;
  }

  @Override
  public void run() {
    long startTime = System.currentTimeMillis();
    while (running.get() && !maxDurationReached(startTime)) {
      try {
        log.info("Iteration " + iterationCount.getAndIncrement());
        doRun();
        delay();
      } catch (Exception e) {
        log.error("Error executing action", e);
        exceptions.add(e);
      }
    }

    running.set(false);
    log.info( "Workload stopped");
  }

  /**
   * Override this method to implement the workload
   */
  private void doRun() {
    workload.run();
  }

  private boolean maxDurationReached(long startTime) {

    if (config.getMaxDuration() == null) {
      return false;
    }

    return System.currentTimeMillis() >= (startTime + config.getMaxDuration().toMillis());
  }

  public Integer getIterationCount() {
    return  iterationCount.get();
  }

  public Boolean isRunning() {
    return running.get();
  }

  public Boolean stop() {
    return running.getAndSet(false);
  }

  public List<Exception> capturedExceptions() {
    return exceptions;
  }

  private void delay() {
    long delay = config.getDelayBetweenIterationsMs();
    if (delay > 0){
      try {
        Thread.sleep(config.getDelayBetweenIterationsMs());
      } catch (InterruptedException e) {
        log.error("Error during delay", e);
      }
    }
  }

  public void awaitTermination() {
  }

}
