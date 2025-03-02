package io.lettuce.test;

public interface CommonWorkloadOptions extends WorkloadOptions {

    int valueSize();

    int elementsCount();

    int iterationCount();

    double getSetRatio();

    int transactionSize();

}
