package io.lettuce.test;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

class Workloads {

    private final Set<ContinuousWorkload> workloads = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public void add(ContinuousWorkload workload) {
        workloads.add(workload);
    }

    public void remove(ContinuousWorkload workload) {
        workloads.remove(workload);
    }

    public boolean contains(ContinuousWorkload workload) {
        return workloads.contains(workload);
    }

    public void stop() {
        for (ContinuousWorkload workload : workloads) {
            workload.stop();
        }
        workloads.clear();
    }

    public boolean isEmpty() {
        return workloads.isEmpty();
    }

}
