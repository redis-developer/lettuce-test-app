package redis.clients.jedis.test.workloads;

import io.lettuce.test.CommonWorkloadOptions;
import io.lettuce.test.workloads.BaseWorkload;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.UnifiedJedis;

import java.util.function.Function;

import static io.lettuce.test.metrics.MetricsReporter.cmdKeyError;
import static io.lettuce.test.metrics.MetricsReporter.cmdKeyOk;

public abstract class JedisBaseWorkload extends BaseWorkload {

    protected static final Logger log = LoggerFactory.getLogger(JedisBaseWorkload.class);

    protected final UnifiedJedis client;

    public JedisBaseWorkload(UnifiedJedis jedis, CommonWorkloadOptions options) {
        super(options);
        this.client = jedis;
    }

    protected abstract void doRun();

    protected abstract String getType();

    @Override
    public void run() {
        Timer.Sample timer = metricsReporter.startTimer();

        try {
            doRun();
            metricsReporter.recordWorkloadExecutionDuration(timer, getType(), BaseWorkload.Status.SUCCESSFUL);
        } catch (Exception e) {
            metricsReporter.recordWorkloadExecutionDuration(timer, getType(), BaseWorkload.Status.COMPLETED_WITH_ERRORS);
            throw e;
        }
    }

    protected <R> R exec(String commandName, Function<UnifiedJedis, R> fn) {
        Timer.Sample sample = metricsReporter.startCommandTimer();
        try {
            R result = fn.apply(client);
            metricsReporter.recordCommandLatency(cmdKeyOk(commandName), sample);
            return result;
        } catch (RuntimeException ex) {
            metricsReporter.incrementCommandError(commandName);
            metricsReporter.recordCommandLatency(cmdKeyError(commandName, ex), sample);
        }
        return null;
    }

}
