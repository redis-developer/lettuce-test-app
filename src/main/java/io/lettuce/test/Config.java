package io.lettuce.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.format.DateTimeParseException;

public class Config {

    private static final ObjectMapper configMapper = new ObjectMapper(new YAMLFactory());

    public RedisConfig redis;

    public TestConfig test;

    public ClientOptionsConfig clientOptions;

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

        public String password;

        public boolean useTls;

        public boolean verifyPeer;

        public String clientName = "lettuce-test-app";

        public Long timeout;

    }

    public static class TestConfig {

        public String mode;  // standalone or cluster

        public int clients;  // Number of client instances

        public int connectionsPerClient;  // Number of connections per client

        public WorkloadConfig workload;

    }

    public static class WorkloadConfig {

        private String type;  // Options: get_set, multi, pub_sub

        private String maxDuration = "60s"; // Maximum duration of the workload

        public String getType() {
            return type;
        }

        public Duration getMaxDuration() {
            if (maxDuration == null || maxDuration.isEmpty()) {
                return Duration.ofSeconds(60);
            }
            try {
                return Duration.parse("PT" + maxDuration.replace("s", "S").replace("m", "M").replace("h", "H"));
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Invalid duration format: " + maxDuration, e);
            }
        }

    }

    public static class ClientOptionsConfig {

        public Boolean autoReconnect;

        public Boolean pingBeforeActivate;

        public SocketOptionsConfig socketOptions;
    }


    public static class SocketOptionsConfig {

        public TcpUserTimeoutOptionsConfig tcpUserTimeoutOptions;

        public KeepAliveOptionsConfig keepAliveOptions;

    }

    public static class TcpUserTimeoutOptionsConfig {

        public Long tcpUserTimeoutMs;

        public Boolean enabled;
    }

    public static class KeepAliveOptionsConfig {

        public Long intervalMs;

        public Long idleMs;

        public Integer count;

    }

}