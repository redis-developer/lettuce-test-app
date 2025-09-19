package redis.clients.jedis.test;

import io.lettuce.test.config.WorkloadRunnerConfig;
import io.lettuce.test.metrics.MetricsReporter;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class JedisWorkloadRunner {

    private static final Logger log = LoggerFactory.getLogger(JedisWorkloadRunner.class);

    private final MetricsReporter metricsReporter;

    private final WorkloadRunnerConfig config;

    private JedisRunner runner;

    public JedisWorkloadRunner(MetricsReporter metricsReporter, WorkloadRunnerConfig config) {
        this.metricsReporter = metricsReporter;
        this.config = config;
    }

    public void run() {
        switch (config.getTest().getMode().toUpperCase()) {
            case "STANDALONE" -> {
                runner = new StandaloneJedisWorkloadRunner(config, metricsReporter);
                log.info("Running standalone workload (Jedis/UnifiedJedis)");
            }
            default -> throw new IllegalArgumentException("Unsupported mode for Jedis app: " + config.getTest().getMode()
                    + ". Only STANDALONE is supported for Jedis right now.");
        }
        runner.run();
    }

    @PreDestroy
    private void shutdown() {
        log.info("Shutting down (Jedis app)...");
        if (runner != null) {
            try {
                runner.close();
            } catch (Exception e) {
                log.warn("Error closing Jedis runner", e);
            }
        }
    }

}
