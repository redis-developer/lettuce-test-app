package io.lettuce.test.workloads;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;

public abstract class BaseWorkload implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(BaseWorkload.class);

    protected <T> T withLatency(Callable<T> callable){
        long start = System.nanoTime();
        try {
            return callable.call();
         } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            // TODO : micrometer
            long end = System.nanoTime();
            log.debug("Latency: " + (end - start) + " ns");
        }
    }
}
