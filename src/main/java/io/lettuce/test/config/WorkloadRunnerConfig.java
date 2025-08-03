package io.lettuce.test.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Configuration
@ConfigurationProperties(prefix = "runner")
@PropertySource(value = "classpath:runner-config-defaults.yaml", factory = YamlPropertySourceFactory.class)
@PropertySource(value = "file:${runner.config:runner-config.yaml}", factory = YamlPropertySourceFactory.class, ignoreResourceNotFound = true)
public class WorkloadRunnerConfig {

    private RedisConfig redis;

    private TestConfig test;

    private ClientOptionsConfig clientOptions;

    private ClusterClientOptionsConfig clusterClientOptions;

    WorkloadRunnerConfig() {

    }

    public RedisConfig getRedis() {
        return redis;
    }

    public void setRedis(RedisConfig redis) {
        this.redis = redis;
    }

    public TestConfig getTest() {
        return test;
    }

    public void setTest(TestConfig test) {
        this.test = test;
    }

    public ClientOptionsConfig getClientOptions() {
        return clientOptions;
    }

    public void setClientOptions(ClientOptionsConfig clientOptions) {
        this.clientOptions = clientOptions;
    }

    public ClusterClientOptionsConfig getClusterClientOptions() {
        return clusterClientOptions;
    }

    public void setClusterClientOptions(ClusterClientOptionsConfig clusterClientOptions) {
        this.clusterClientOptions = clusterClientOptions;
    }

    @Override
    public String toString() {
        return "WorkloadRunnerConfig{" + "redis=" + redis + ", test=" + test + ", clientOptions=" + clientOptions
                + ", clusterClientOptions=" + clusterClientOptions + '}';
    }

    public static class RedisConfig {

        private String host;

        private int port;

        private int database;

        private String username;

        private String password;

        private boolean useTls;

        private boolean verifyPeer;

        private String clientName = "lettuce-test-app";

        private Duration timeout;

        // Getters and Setters
        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public int getDatabase() {
            return database;
        }

        public void setDatabase(int database) {
            this.database = database;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public boolean isUseTls() {
            return useTls;
        }

        public void setUseTls(boolean useTls) {
            this.useTls = useTls;
        }

        public boolean isVerifyPeer() {
            return verifyPeer;
        }

        public void setVerifyPeer(boolean verifyPeer) {
            this.verifyPeer = verifyPeer;
        }

        public String getClientName() {
            return clientName;
        }

        public void setClientName(String clientName) {
            this.clientName = clientName;
        }

        public Duration getTimeout() {
            return timeout;
        }

        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }

        @Override
        public String toString() {
            return "RedisConfig{" + "host='" + host + '\'' + ", port=" + port + ", database=" + database + ", username='"
                    + username + '\'' + ", password='"
                    + Optional.ofNullable(password).map(p -> "*".repeat(p.length())).orElse("") + '\'' + ", useTls=" + useTls
                    + ", verifyPeer=" + verifyPeer + ", clientName='" + clientName + '\'' + ", timeout=" + timeout + '}';
        }

    }

    public static class TestConfig {

        private String mode;

        private int clients;

        private int connectionsPerClient;

        private int threadsPerConnection;

        private WorkloadConfig workload;

        // Getters and Setters
        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public int getClients() {
            return clients;
        }

        public void setClients(int clients) {
            this.clients = clients;
        }

        public int getConnectionsPerClient() {
            return connectionsPerClient;
        }

        public void setConnectionsPerClient(int connectionsPerClient) {
            this.connectionsPerClient = connectionsPerClient;
        }

        public int getThreadsPerConnection() {
            return threadsPerConnection;
        }

        public void setThreadsPerConnection(int threadsPerConnection) {
            this.threadsPerConnection = threadsPerConnection;
        }

        public WorkloadConfig getWorkload() {
            return workload;
        }

        public void setWorkload(WorkloadConfig workload) {
            this.workload = workload;
        }

        @Override
        public String toString() {
            return "TestConfig{" + "mode='" + mode + '\'' + ", clients=" + clients + ", connectionsPerClient="
                    + connectionsPerClient + ", workload=" + workload + '}';
        }

    }

    public static class WorkloadConfig {

        private String type;

        private Duration maxDuration = Duration.ofSeconds(60);

        private Map<String, String> options;

        // Getters and Setters
        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Duration getMaxDuration() {
            return maxDuration;
        }

        public void setMaxDuration(Duration maxDuration) {
            this.maxDuration = maxDuration;
        }

        public Map<String, String> getOptions() {
            return options;
        }

        public void setOptions(Map<String, String> options) {
            this.options = options;
        }

        @Override
        public String toString() {
            return "WorkloadConfig{" + "type='" + type + '\'' + ", maxDuration=" + maxDuration + ", options=" + options + '}';
        }

    }

    public static class ClusterClientOptionsConfig {

        private ClusterTopologyRefreshOptionsConfig topologyRefreshOptions;

        private Boolean validateClusterNodeMembership;

        // Getters and Setters
        public ClusterTopologyRefreshOptionsConfig getTopologyRefreshOptions() {
            return topologyRefreshOptions;
        }

        public void setTopologyRefreshOptions(ClusterTopologyRefreshOptionsConfig topologyRefreshOptions) {
            this.topologyRefreshOptions = topologyRefreshOptions;
        }

        public Boolean isValidateClusterNodeMembership() {
            return validateClusterNodeMembership;
        }

        public void setValidateClusterNodeMembership(Boolean validateClusterNodeMembership) {
            this.validateClusterNodeMembership = validateClusterNodeMembership;
        }

        @Override
        public String toString() {
            return "ClusterClientOptionsConfig{" + "topologyRefreshOptions=" + topologyRefreshOptions + '}';
        }

        public static class ClusterTopologyRefreshOptionsConfig {

            private AdaptiveRefreshConfig adaptive;

            public AdaptiveRefreshConfig getAdaptive() {
                return adaptive;
            }

            public void setAdaptive(AdaptiveRefreshConfig adaptive) {
                this.adaptive = adaptive;
            }

            @Override
            public String toString() {
                return "ClusterTopologyRefreshOptionsConfig{" + "adaptive=" + adaptive + '}';
            }

            public static class AdaptiveRefreshConfig {

                private boolean enabled;

                private Set<String> refreshTriggers;

                private Duration triggersTimeout;

                // Getters and Setters
                public boolean isEnabled() {
                    return enabled;
                }

                public void setEnabled(boolean enabled) {
                    this.enabled = enabled;
                }

                public Set<String> getRefreshTriggers() {
                    return refreshTriggers;
                }

                public void setRefreshTriggers(Set<String> refreshTriggers) {
                    this.refreshTriggers = refreshTriggers;
                }

                public Duration getTriggersTimeout() {
                    return triggersTimeout;
                }

                public void setTriggersTimeout(Duration triggersTimeout) {
                    this.triggersTimeout = triggersTimeout;
                }

                @Override
                public String toString() {
                    return "AdaptiveRefreshConfig{" + "enabled=" + enabled + ", refreshTriggers='" + refreshTriggers + '\''
                            + ", triggersTimeout=" + triggersTimeout + '}';
                }

            }

        }

    }

    public static class ClientOptionsConfig {

        private Boolean autoReconnect;

        private MaintenanceEventsConfig maintenanceEventsConfig;

        private Boolean pingBeforeActivate;

        private Integer requestQueueSize;

        private TimeoutOptionsConfig timeoutOptions;

        private SocketOptionsConfig socketOptions;

        private String disconnectedBehavior;

        private ReconnectOptionsConfig reconnectOptions;

        private MetricsOptionsConfig metricsOptions;

        // Getters and Setters
        public Boolean getAutoReconnect() {
            return autoReconnect;
        }

        public void setAutoReconnect(Boolean autoReconnect) {
            this.autoReconnect = autoReconnect;
        }

        public MaintenanceEventsConfig getMaintenanceEventsConfig() {
            return maintenanceEventsConfig;
        }

        public void setMaintenanceEventsConfig(MaintenanceEventsConfig supportMaintenanceEvents) {
            this.maintenanceEventsConfig = supportMaintenanceEvents;
        }

        public Boolean getPingBeforeActivate() {
            return pingBeforeActivate;
        }

        public void setPingBeforeActivate(Boolean pingBeforeActivate) {
            this.pingBeforeActivate = pingBeforeActivate;
        }

        public Integer getRequestQueueSize() {
            return requestQueueSize;
        }

        public void setRequestQueueSize(Integer requestQueueSize) {
            this.requestQueueSize = requestQueueSize;
        }

        public TimeoutOptionsConfig getTimeoutOptions() {
            return timeoutOptions;
        }

        public void setTimeoutOptions(TimeoutOptionsConfig timeoutOptions) {
            this.timeoutOptions = timeoutOptions;
        }

        public SocketOptionsConfig getSocketOptions() {
            return socketOptions;
        }

        public void setSocketOptions(SocketOptionsConfig socketOptions) {
            this.socketOptions = socketOptions;
        }

        public String getDisconnectedBehavior() {
            return disconnectedBehavior;
        }

        public void setDisconnectedBehavior(String disconnectedBehavior) {
            this.disconnectedBehavior = disconnectedBehavior;
        }

        public ReconnectOptionsConfig getReconnectOptions() {
            return reconnectOptions;
        }

        public void setReconnectOptions(ReconnectOptionsConfig reconnectOptions) {
            this.reconnectOptions = reconnectOptions;
        }

        @Override
        public String toString() {
            return "ClientOptionsConfig{" + "autoReconnect=" + autoReconnect + ", proactiveRebind=" + maintenanceEventsConfig
                    + ", pingBeforeActivate=" + pingBeforeActivate + ", requestQueueSize=" + requestQueueSize
                    + ", timeoutOptions=" + timeoutOptions + ", socketOptions=" + socketOptions + ", disconnectedBehavior='"
                    + disconnectedBehavior + '\'' + ", reconnectOptions=" + reconnectOptions + '}';
        }

        public MetricsOptionsConfig getMetricsOptions() {
            return metricsOptions;
        }

        public void setMetricsOptions(MetricsOptionsConfig metricsOptions) {
            this.metricsOptions = metricsOptions;
        }

    }

    public static class MetricsOptionsConfig {

        private Boolean connectionMonitoring;

        private Boolean commandLatencyMonitoring;

        public Boolean getConnectionMonitoring() {
            return connectionMonitoring;
        }

        public void setConnectionMonitoring(Boolean connectionMonitoring) {
            this.connectionMonitoring = connectionMonitoring;
        }

        public Boolean getCommandLatencyMonitoring() {
            return commandLatencyMonitoring;
        }

        public void setCommandLatencyMonitoring(Boolean commandLatencyMonitoring) {
            this.commandLatencyMonitoring = commandLatencyMonitoring;
        }

    }

    public static class ReconnectOptionsConfig {

        private Duration fixedDelay;

        // Getters and Setters
        public Duration getFixedDelay() {
            return fixedDelay;
        }

        public void setFixedDelay(Duration fixedDelay) {
            this.fixedDelay = fixedDelay;
        }

        @Override
        public String toString() {
            return "ReconnectOptionsConfig{" + "fixedDelay=" + fixedDelay + '}';
        }

    }

    public static class TimeoutOptionsConfig {

        private Duration fixedTimeout;

        private Duration timeoutsRelaxingDuringMaintenance;

        // Getters and Setters
        public Duration getFixedTimeout() {
            return fixedTimeout;
        }

        public void setFixedTimeout(Duration fixedTimeout) {
            this.fixedTimeout = fixedTimeout;
        }

        public Duration getTimeoutsRelaxingDuringMaintenance() {
            return timeoutsRelaxingDuringMaintenance;
        }

        public void setTimeoutsRelaxingDuringMaintenance(Duration timeoutsRelaxingDuringMaintenance) {
            this.timeoutsRelaxingDuringMaintenance = timeoutsRelaxingDuringMaintenance;
        }

        @Override
        public String toString() {
            return "TimeoutOptionsConfig{" + "fixedTimeout=" + fixedTimeout + ",proactiveExpiryRelaxTime="
                    + timeoutsRelaxingDuringMaintenance + '}';
        }

    }

    public static class SocketOptionsConfig {

        private Duration connectTimeout;

        private TcpUserTimeoutOptionsConfig tcpUserTimeoutOptions;

        private KeepAliveOptionsConfig keepAliveOptions;

        // Getters and Setters
        public Duration getConnectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(Duration connectTimeout) {
            this.connectTimeout = connectTimeout;
        }

        public TcpUserTimeoutOptionsConfig getTcpUserTimeoutOptions() {
            return tcpUserTimeoutOptions;
        }

        public void setTcpUserTimeoutOptions(TcpUserTimeoutOptionsConfig tcpUserTimeoutOptions) {
            this.tcpUserTimeoutOptions = tcpUserTimeoutOptions;
        }

        public KeepAliveOptionsConfig getKeepAliveOptions() {
            return keepAliveOptions;
        }

        public void setKeepAliveOptions(KeepAliveOptionsConfig keepAliveOptions) {
            this.keepAliveOptions = keepAliveOptions;
        }

        @Override
        public String toString() {
            return "SocketOptionsConfig{" + "connectTimeout=" + connectTimeout + ", tcpUserTimeoutOptions="
                    + tcpUserTimeoutOptions + ", keepAliveOptions=" + keepAliveOptions + '}';
        }

    }

    public static class TcpUserTimeoutOptionsConfig {

        private Duration tcpUserTimeout;

        private Boolean enabled;

        // Getters and Setters
        public Duration getTcpUserTimeout() {
            return tcpUserTimeout;
        }

        public void setTcpUserTimeout(Duration tcpUserTimeout) {
            this.tcpUserTimeout = tcpUserTimeout;
        }

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        @Override
        public String toString() {
            return "TcpUserTimeoutOptionsConfig{" + "tcpUserTimeout=" + tcpUserTimeout + ", enabled=" + enabled + '}';
        }

    }

    public static class KeepAliveOptionsConfig {

        private Duration interval;

        private Duration idle;

        private Integer count;

        // Getters and Setters
        public Duration getInterval() {
            return interval;
        }

        public void setInterval(Duration interval) {
            this.interval = interval;
        }

        public Duration getIdle() {
            return idle;
        }

        public void setIdle(Duration idle) {
            this.idle = idle;
        }

        public Integer getCount() {
            return count;
        }

        public void setCount(Integer count) {
            this.count = count;
        }

        @Override
        public String toString() {
            return "KeepAliveOptionsConfig{" + "interval=" + interval + ", idle=" + idle + ", count=" + count + '}';
        }

    }

    public static class MaintenanceEventsConfig {

        boolean enabled;

        String movingEndpointAddressType;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getMovingEndpointAddressType() {
            return movingEndpointAddressType;
        }

        public void setMovingEndpointAddressType(String movingEndpointAddressType) {
            this.movingEndpointAddressType = movingEndpointAddressType;
        }

    }

}
