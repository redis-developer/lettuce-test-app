package redis.clients.jedis.test;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.util.HashMap;
import java.util.Map;

@SpringBootApplication(scanBasePackages = {  "io.lettuce.test.metrics", "io.lettuce.test.config", "redis.clients.jedis.test" })
public class JedisTestApplication implements ApplicationRunner {

    private final JedisWorkloadRunner runner;

    public JedisTestApplication(JedisWorkloadRunner runner) {
        this.runner = runner;
    }

    public static void main(String[] args) {
        // Ensure Jedis-specific app name overrides application.properties
        System.setProperty("spring.application.name", "jedis-test-app");
        // Ensure appName property (used in metrics tags) aligns with the app name
        System.setProperty("appName", "jedis-test-app");

        SpringApplication app = new SpringApplication(JedisTestApplication.class);
        Map<String, Object> defaults = new HashMap<>();
        defaults.put("runner.defaults", "runner-config-defaults-jedis.yaml");
        app.setDefaultProperties(defaults);
        app.run(args);
    }

    @Override
    public void run(ApplicationArguments args) {
        runner.run();
    }

}
