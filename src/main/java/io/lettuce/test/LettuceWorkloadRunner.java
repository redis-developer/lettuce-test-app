package io.lettuce.test;

import io.lettuce.test.config.WorkloadRunnerConfig;
import io.lettuce.test.metrics.MetricsReporter;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LettuceWorkloadRunner {

    private static final Logger log = LoggerFactory.getLogger(LettuceWorkloadRunner.class);

    private final MetricsReporter metricsReporter;

    private final WorkloadRunnerConfig config;

    private WorkloadRunnerBase<?, ?> runner = null;

    public LettuceWorkloadRunner(MetricsReporter metricsReporter, WorkloadRunnerConfig config) {
        this.metricsReporter = metricsReporter;
        this.config = config;
    }

    public void run() {

        switch (config.getTest().getMode().toUpperCase()) {
            case "STANDALONE":
                runner = new StandaloneWorkloadRunner(config, metricsReporter);
                log.info("Running standalone workload");
                break;
            case "CLUSTER":
                runner = new ClusterWorkloadRunner(config, metricsReporter);
                log.info("Running cluster workload");
                break;
            case "PROACTIVE_UPGRADE":
                runner = new ProactiveUpgradeWorkloadRunner(config, metricsReporter);
                log.info("Running proactive_upgrade workload");
                break;
            default:
                throw new IllegalArgumentException("Invalid mode specified: " + config.getTest().getMode());
        }

        runner.run();

        // Wait for runner to finish
        runner.awaitTermination();

        try {
            runner.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @PreDestroy
    private void shutdown() {
        log.info("Shutting down...");
        if (runner != null) {
            runner.close();
        }
    }

}
