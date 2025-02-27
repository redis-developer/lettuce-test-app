package io.lettuce.test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WorkloadsTest {

    private Workloads workloads;

    private ContinousWorkload mockWorkload;

    @BeforeEach
    void setUp() {
        workloads = new Workloads();
        mockWorkload = mock(ContinousWorkload.class);
    }

    @Test
    void addWorkload() {
        workloads.add(mockWorkload);
        assertTrue(workloads.contains(mockWorkload));
    }

    @Test
    void removeWorkload() {
        workloads.add(mockWorkload);
        workloads.remove(mockWorkload);
        assertFalse(workloads.contains(mockWorkload));
    }

    @Test
    void containsWorkload() {
        workloads.add(mockWorkload);
        assertTrue(workloads.contains(mockWorkload));
        workloads.remove(mockWorkload);
        assertFalse(workloads.contains(mockWorkload));
    }

    @Test
    void stopAllWorkloads() {
        workloads.add(mockWorkload);
        workloads.stop();
        verify(mockWorkload).stop();
        assertTrue(workloads.isEmpty());
    }

    @Test
    void isEmptyWhenNoWorkloads() {
        assertTrue(workloads.isEmpty());
    }

    @Test
    void isNotEmptyWhenWorkloadsAdded() {
        workloads.add(mockWorkload);
        assertFalse(workloads.isEmpty());
    }

}
