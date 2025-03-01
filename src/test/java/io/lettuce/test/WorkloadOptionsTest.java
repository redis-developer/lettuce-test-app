package io.lettuce.test;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WorkloadOptionsTest {

    @Test
    void testGetIntegerWithDefault() {
        Map<String, Object> optionsMap = new HashMap<>();
        optionsMap.put("existingKey", 42);
        WorkloadOptions options = new WorkloadOptions(optionsMap);

        assertEquals(42, options.getInteger("existingKey", 0));
        assertEquals(0, options.getInteger("missingKey", 0));
    }

    @Test
    void testGetDoubleWithDefault() {
        Map<String, Object> optionsMap = new HashMap<>();
        optionsMap.put("existingKey", 3.14);
        WorkloadOptions options = new WorkloadOptions(optionsMap);

        assertEquals(3.14, options.getDouble("existingKey", 0.0));
        assertEquals(0.0, options.getDouble("missingKey", 0.0));
    }

    @Test
    void testGetStringWithDefault() {
        Map<String, Object> optionsMap = new HashMap<>();
        optionsMap.put("existingKey", "value");
        WorkloadOptions options = new WorkloadOptions(optionsMap);

        assertEquals("value", options.getString("existingKey", "default"));
        assertEquals("default", options.getString("missingKey", "default"));
    }

    @Test
    void testGetIntegerWithoutDefault() {
        Map<String, Object> optionsMap = new HashMap<>();
        optionsMap.put("existingKey", 42);
        WorkloadOptions options = new WorkloadOptions(optionsMap);

        assertEquals(42, options.getInteger("existingKey"));
        assertNull(options.getInteger("missingKey"));
    }

    @Test
    void testGetDoubleWithoutDefault() {
        Map<String, Object> optionsMap = new HashMap<>();
        optionsMap.put("existingKey", 3.14);
        WorkloadOptions options = new WorkloadOptions(optionsMap);

        assertEquals(3.14, options.getDouble("existingKey"));
        assertNull(options.getDouble("missingKey"));
    }

    @Test
    void testGetStringWithoutDefault() {
        Map<String, Object> optionsMap = new HashMap<>();
        optionsMap.put("existingKey", "value");
        WorkloadOptions options = new WorkloadOptions(optionsMap);

        assertEquals("value", options.getString("existingKey"));
        assertNull(options.getString("missingKey"));
    }

}
