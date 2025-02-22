package io.lettuce.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.format.DateTimeParseException;

public class Config {

  public RedisConfig redis;
  public TestConfig test;
  public ClientOptionsConfig clientOptions;

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
    public int threads;
    public WorkloadConfig workload;
  }

  public static class WorkloadConfig {
    private String type;  // Options: get_set, multi, pub_sub
    private String maxDuration = "60s"; // Maximum duration of the workload
    private int delayBetweenIterationsMs = 0; // Delay between iterations

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

    public int getDelayBetweenIterationsMs() {
      return delayBetweenIterationsMs;
    }
  }

  public static class ClientOptionsConfig {
    public boolean autoReconnect;
    public boolean pingBeforeActivate;
  }

  private static final ObjectMapper configMapper = new ObjectMapper(new YAMLFactory());
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
}