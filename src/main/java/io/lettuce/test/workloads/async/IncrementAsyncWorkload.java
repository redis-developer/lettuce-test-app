package io.lettuce.test.workloads.async;

import io.lettuce.core.LettuceFutures;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.test.CommonWorkloadOptions;
import io.lettuce.test.workloads.BaseWorkload;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class IncrementAsyncWorkload extends BaseWorkload {

    private final StatefulRedisConnection<String, String> conn;

    AtomicInteger incrKeyIdx = new AtomicInteger();

    public IncrementAsyncWorkload(StatefulRedisConnection<String, String> conn, CommonWorkloadOptions options) {
        super(options);
        this.conn = conn;
    }

    @Override
    public void run() {
        AtomicInteger requestedIncrements = new AtomicInteger();
        AtomicInteger successfulIncrements = new AtomicInteger();
        AtomicInteger failedIncrements = new AtomicInteger();
        List<RedisFuture<Long>> futures = new ArrayList<>();

        RedisAsyncCommands<String, String> cmd = withMetrics(conn.async());

        String key = IncrementAsyncWorkload.class.getSimpleName() + ":" + incrKeyIdx.getAndIncrement();

        cmd.set(key, "0");
        for (int i = 0; i < options().iterationCount(); i++) {

            RedisFuture<Long> incr = cmd.incr(key);
            requestedIncrements.incrementAndGet();
            incr.whenComplete((value, throwable) -> {
                if (throwable == null) {
                    successfulIncrements.incrementAndGet();
                } else {
                    failedIncrements.incrementAndGet();
                }
            });

            futures.add(incr);
            delay(options().delayAfterIteration());
        }

        if (options().getBoolean("awaitAllResponses", true)) {
            LettuceFutures.awaitAll(1, TimeUnit.MINUTES, futures.toArray(new RedisFuture[futures.size()]));
        }

        String counter = conn.sync().get(key);
        int finalCount = Integer.parseInt(counter);

        checkConsistency(key, finalCount, requestedIncrements.get(), successfulIncrements.get(), failedIncrements.get());
    }

    private void checkConsistency(String key, int actual, int requested, int successful, int failed) {
        if (actual != requested) {
            String msg = String.format(
                    "Consistency check failure. Key %s : expected %d,  actual %d. "
                            + " INCR operation's requested: %d, successful %d, failed %d",
                    key, requested, actual, requested, successful, failed);
            throw new IllegalStateException(msg);
        }
    }

}
