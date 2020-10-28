package com.gigaspaces.k8s.operators.common;

import io.fabric8.kubernetes.api.model.Quantity;

import java.util.HashMap;
import java.util.Map;

public class ResourcesValuesSpec {
    private String cpu;
    private String memory;

    public static ResourcesValuesSpec merge(ResourcesValuesSpec partitionSpec, ResourcesValuesSpec defaultSpec) {
        if (partitionSpec == null)
            return defaultSpec;
        if (defaultSpec == null)
            return partitionSpec;
        ResourcesValuesSpec result = new ResourcesValuesSpec();
        result.cpu = partitionSpec.cpu != null ? partitionSpec.cpu : defaultSpec.cpu;
        result.memory = partitionSpec.memory != null ? partitionSpec.memory : defaultSpec.memory;
        return result;
    }

    public String getCpu() {
        return cpu;
    }
    public void setCpu(String cpu) {
        this.cpu = cpu;
    }

    public String getMemory() {
        return memory;
    }
    public void setMemory(String memory) {
        this.memory = memory;
    }

    public Map<String, Quantity> toMap() {
        Map<String, Quantity> result = new HashMap<>();
        if (cpu != null)
            result.put("cpu", Quantity.parse(cpu));
        if (memory != null)
            result.put("memory", Quantity.parse(memory));
        return result;
    }
}
