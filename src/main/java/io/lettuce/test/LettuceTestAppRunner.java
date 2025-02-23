package io.lettuce.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class LettuceTestAppRunner {

    private static final Logger log = LoggerFactory.getLogger(LettuceTestAppRunner.class);

    private BaseWorkloadRunner runner = null;

    public static void main(String[] args) throws IOException {
        LettuceTestAppRunner app = new LettuceTestAppRunner();
        app.registerShutdownHook();
        app.run(args);
    }

    private void run(String[] args) {
        // Load YAML config
        Config config = loadConfig(args);

        boolean isCluster = config.test.mode.equalsIgnoreCase("cluster");
        if (isCluster) {
            runner = new ClusterWorkloadRunner(config);
        } else {
            runner = new StandaloneWorkloadRunner(config);
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

    private Config loadConfig(String[] args) {
        String configFilePath = "config.yaml";

        if (args.length > 0) {
            configFilePath = args[0];
        }

        Config config = null;
        try {
            if (args.length > 0) {
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

}