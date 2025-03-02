package io.lettuce.test;

import io.lettuce.test.config.WorkloadRunnerConfig.WorkloadConfig;
import io.lettuce.test.workloads.BaseWorkload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ContinousWorkload implements Runnable {

    protected static final Logger log = LoggerFactory.getLogger(ContinousWorkload.class);

    protected final WorkloadConfig config;

    private final BaseWorkload workload;

    private final List<Exception> exceptions = new ArrayList<>();

    private final AtomicBoolean running = new AtomicBoolean(true);

    private final AtomicInteger iterationCount = new AtomicInteger(0);

    public ContinousWorkload(BaseWorkload workload, WorkloadConfig config) {
        this.config = config;
        this.workload = workload;
    }

    @Override
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
                // TODO : micrometer counter
                exceptions.add(e);
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
        running.getAndSet(false);
    }

    public List<Exception> capturedExceptions() {
        return exceptions;
    }

}
