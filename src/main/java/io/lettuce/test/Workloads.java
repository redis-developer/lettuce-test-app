package io.lettuce.test;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

class Workloads {

    private final Set<ContinousWorkload> workloads = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public void add(ContinousWorkload workload) {
        workloads.add(workload);
    }

    public void remove(ContinousWorkload workload) {
        workloads.remove(workload);
    }

    public boolean contains(ContinousWorkload workload) {
        return workloads.contains(workload);
    }

    public void stop() {
        for (ContinousWorkload workload : workloads) {
            workload.stop();
        }
        workloads.clear();
    }

    public boolean isEmpty() {
        return workloads.isEmpty();
    }

}
