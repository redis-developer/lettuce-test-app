package io.lettuce.test.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;

import org.springframework.context.event.EventListener;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.StreamSupport;

@Component
public class ConfigDumper {

    private static final Logger logger = LoggerFactory.getLogger(ConfigDumper.class);

    @EventListener
    public void dumpConfiguration(ApplicationReadyEvent event) {
        final Environment env = event.getApplicationContext().getEnvironment();
        final MutablePropertySources sources = ((AbstractEnvironment) env).getPropertySources();

        StreamSupport.stream(sources.spliterator(), false).filter(ps -> ps instanceof EnumerablePropertySource<?>)
                .forEach(ps -> {
                    // Print the property source name
                    logger.info("\n-- Property Source: {}", ps.getName());

                    // Log each property from this property source
                    String[] propertyNames = ((EnumerablePropertySource<?>) ps).getPropertyNames();
                    Arrays.stream(propertyNames).sorted().forEach(prop -> {
                        String value = env.getProperty(prop);
                        // Hide sensitive properties
                        if (prop.contains("credentials") || prop.contains("password") || prop.contains("token")) {
                            value = "<hidden>";
                        }
                        logger.info("{}: {}", prop, value);
                    });
                });
    }

}
