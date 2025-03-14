package io.lettuce.test.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;

import org.springframework.context.event.EventListener;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.StreamSupport;

@Component
public class ConfigDumper {

    private static final Logger logger = LoggerFactory.getLogger(ConfigDumper.class);

    private final Environment environment;

    @Autowired
    public ConfigDumper(Environment environment) {
        this.environment = environment;
    }

    @EventListener
    public void dumpConfiguration(ApplicationReadyEvent event) {
        final MutablePropertySources sources = ((AbstractEnvironment) environment).getPropertySources();

        // TreeSet to store distinct property names in a sorted order
        Set<String> distinctProperties = new TreeSet<>();

        // Iterate over all property sources and collect distinct properties
        logger.info("\n-- Configuration Properties --");
        StreamSupport.stream(sources.spliterator(), false).filter(ps -> ps instanceof EnumerablePropertySource<?>) // Only
                                                                                                                   // process
                                                                                                                   // EnumerablePropertySources
                .forEach(ps -> {
                    String[] propertyNames = ((EnumerablePropertySource<?>) ps).getPropertyNames();
                    Arrays.stream(propertyNames).forEach(prop -> {
                        // Only add to distinct set if it is not already present
                        if (distinctProperties.add(prop)) {
                            String resolvedValue = environment.getProperty(prop); // Get resolved value
                            if (resolvedValue != null) {
                                // Hide sensitive properties
                                if (prop.contains("credentials") || prop.contains("password") || prop.contains("token")) {
                                    resolvedValue = "<hidden>";
                                }

                                // Print in command-line argument format (--key=value)
                                logger.info("{}={}", prop, resolvedValue);
                            }
                        }
                    });
                });
    }

}
