package io.lettuce.test;

public interface CommonWorkflowOptions extends WorkloadOptions {

    int valueSize();

    int elementsCount();

    int iterationCount();

    double getSetRatio();

    int transactionSize();

}
