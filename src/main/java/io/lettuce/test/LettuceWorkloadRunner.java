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

        boolean isCluster = "cluster".equalsIgnoreCase(config.getTest().getMode());
        if (isCluster) {
            runner = new ClusterWorkloadRunner(config, metricsReporter);
        } else {
            runner = new StandaloneWorkloadRunner(config, metricsReporter);
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
