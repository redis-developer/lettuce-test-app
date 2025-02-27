package io.lettuce.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Map;

public class Config {

    private static final ObjectMapper configMapper = new ObjectMapper(new YAMLFactory());
    static {
        configMapper.registerModule(new JavaTimeModule());
    }

    public RedisConfig redis;

    public TestConfig test;

    public ClientOptionsConfig clientOptions;

    public MetricsConfig metrics;

    public static Config load(String path) throws IOException {
        return configMapper.readValue(new File(path), Config.class);
    }

    public static Config loadFromResources(String fileName) throws IOException {
        try (InputStream inputStream = Config.class.getClassLoader().getResourceAsStream(fileName)) {
            if (inputStream == null) {
                throw new IOException("Config file not found in resources: " + fileName);
            }
            return configMapper.readValue(inputStream, Config.class);
        }
    }

    public static class RedisConfig {

        public String host;

        public int port;

        public int database;

        public String username;

        public String password;

        public boolean useTls;

        public boolean verifyPeer;

        public String clientName = "lettuce-test-app";

        public Duration timeout;

    }

    public static class TestConfig {

        public String mode; // standalone or cluster

        public int clients; // Number of client instances

        public int connectionsPerClient; // Number of connections per client

        public WorkloadConfig workload;

    }

    public static class WorkloadConfig {

        public String type; // Options: get_set, multi, pub_sub

        public Duration maxDuration = Duration.ofSeconds(60); // Maximum duration of the workload

        public Map<String, Object> options;

    }

    public static class ClientOptionsConfig {

        public Boolean autoReconnect;

        public Boolean pingBeforeActivate;

        public TimeoutOptionsConfig timeoutOptions;

        public SocketOptionsConfig socketOptions;

        public String disconnectedBehavior; // Options: DEFAULT, ACCEPT_COMMANDS, REJECT_COMMANDS

    }

    public static class TimeoutOptionsConfig {

        public Duration fixedTimeout;

    }

    public static class SocketOptionsConfig {

        public TcpUserTimeoutOptionsConfig tcpUserTimeoutOptions;

        public KeepAliveOptionsConfig keepAliveOptions;

    }

    public static class TcpUserTimeoutOptionsConfig {

        public Duration tcpUserTimeout;

        public Boolean enabled;

    }

    public static class KeepAliveOptionsConfig {

        public Duration interval;

        public Duration idle;

        public Integer count;

    }

    public static class MetricsConfig {

        public InfluxConfig influx;

        public LoggingConfig logging;

    }

    public static class InfluxConfig {

        public boolean enable;

        public String uri; // InfluxDB URL

        public int db; // Number of client instances

        public Boolean autoCreateDb = true; // Auto-create DB if missing

        public String step = "5s"; // # Step size (i.e. reporting frequency)

    }

    public static class LoggingConfig {

        public boolean enable;

        public String step = "5s"; // # Step size (i.e. reporting frequency)

    }

}
