package io.lettuce.test;

import io.lettuce.test.metrics.Metrics;
import io.lettuce.test.metrics.MetricsReporter;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.assertj.core.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class LettuceWorkloadRunner {

    private static final Logger log = LoggerFactory.getLogger(LettuceWorkloadRunner.class);

    private WorkloadRunnerBase<?, ?> runner = null;

    public static void main(String[] args) {
        LettuceWorkloadRunner app = new LettuceWorkloadRunner();
        app.registerShutdownHook();
        app.run(args);
    }

    private void run(String[] args) {
        CommandLine cmd = parseArgs(args);

        if (cmd == null) {
            log.error("Invalid command-line arguments. Exiting...");
            System.exit(1);
        }

        // Load YAML config
        Config config = loadConfig(cmd.getOptionValue("config", "config.yaml"));

        // Create a Micrometer registry
        Metrics.initMeterRegistry(config.metrics);
        MetricsReporter metricsReporter = new MetricsReporter(Metrics.getMeterRegistry());

        boolean isCluster = config.test.mode.equalsIgnoreCase("cluster");
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

    private Config loadConfig(String configFilePath) {

        Config config = null;
        try {
            if (!Strings.isNullOrEmpty(configFilePath)) {
                config = Config.load(configFilePath);
            } else {
                config = Config.loadFromResources("config.yaml");
            }
        } catch (IOException e) {
            log.error("Error loading config file from path: " + configFilePath, e);
            System.exit(1);
        }

        log.info("Configuration loaded successfully from: " + configFilePath);
        return config;
    }

    private void shutdown() {
        log.info("Shutting down...");
        if (runner != null) {
            runner.close();
        }
    }

    private void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }

    private CommandLine parseArgs(String[] args) {
        Options options = new Options();

        Option configOption = new Option("c", "config", true, "Path to the configuration YAML file");
        configOption.setRequired(false);
        options.addOption(configOption);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            log.error("Parsing command-line arguments failed: " + e.getMessage());
            formatter.printHelp("lettuce-test-app", options);
            return null;
        }

        return cmd;
    }

}
