package io.lettuce.test;

import io.lettuce.test.config.WorkloadRunnerConfig.WorkloadConfig;
import io.lettuce.test.workloads.BaseWorkload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ContinuousWorkload {

    protected static final Logger log = LoggerFactory.getLogger(ContinuousWorkload.class);

    protected final WorkloadConfig config;

    private final BaseWorkload workload;

    private final AtomicBoolean running = new AtomicBoolean(true);

    private final AtomicInteger iterationCount = new AtomicInteger(0);

    public ContinuousWorkload(BaseWorkload workload, WorkloadConfig config) {
        this.config = config;
        this.workload = workload;
    }

    public void run() {
        long startTime = System.currentTimeMillis();
        log.info("Workload started. {} ", config);
        while (running.get() && !maxDurationReached(startTime)) {
            try {
                iterationCount.getAndIncrement();
                log.debug("Iteration {} started. Workload {}", iterationCount.get(), config);
                doRun();
            } catch (Exception e) {
                log.error("{} completed with errors", logPrefix(), e);
            }
        }

        running.set(false);
        log.info(logPrefix() + " completed.");
    }

    private String logPrefix() {
        return "Workload : " + config.getType();
    }

    /**
     * Override this method to implement the workload
     */
    private void doRun() {
        workload.run();
        delay(workload.options().delayAfterWorkload());
    }

    private boolean maxDurationReached(long startTime) {

        if (config.getMaxDuration() == null) {
            return false;
        }

        return System.currentTimeMillis() >= (startTime + config.getMaxDuration().toMillis());
    }

    public Integer getIterationCount() {
        return iterationCount.get();
    }

    public Boolean isRunning() {
        return running.get();
    }

    public void stop() {
        log.info("Stopping workload {}", config);
        running.getAndSet(false);
    }

    protected void delay(Duration delay) {
        if (Duration.ZERO.equals(delay)) {
            return;
        }

        try {
            Thread.sleep(delay.toMillis());
        } catch (InterruptedException e) {
            log.error("Delay interrupted", e);
        }
    }

}
