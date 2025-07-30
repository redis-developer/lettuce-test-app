package io.lettuce.test.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for test run identification. Contains shared properties like runId and instanceId that can be used
 * across multiple beans. This class is configured via TestRunPropertiesConfiguration to maintain backward compatibility with
 * existing property names (runId, instanceId, appName).
 */
@Component
public class TestRunProperties {

    /**
     * Unique identifier for the test run. Defaults to workload type + random alphanumeric string.
     */
    @Value("${runId:${runner.test.workload.type}-#{T(org.apache.commons.lang3.RandomStringUtils).randomAlphanumeric(8)}}")
    private String runId;

    /**
     * Unique identifier for the application instance. Defaults to application name + random alphanumeric string.
     */
    @Value("${instanceId:${spring.application.name}-#{T(org.apache.commons.lang3.RandomStringUtils).randomAlphanumeric(8)}}")
    private String instanceId;

    /**
     * Application name for identification purposes. Defaults to "lettuce-test-app".
     */
    @Value("${appName:lettuce-test-app}")
    private String appName;

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    @Override
    public String toString() {
        return "TestRunProperties{" + "runId='" + runId + '\'' + ", instanceId='" + instanceId + '\'' + ", appName='" + appName
                + '\'' + '}';
    }

}
