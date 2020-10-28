package com.gigaspaces.k8s.operators.common;

public class ResourcesSpec {
    private Integer id;
    private ResourcesValuesSpec limits;
    private ResourcesValuesSpec requests;

    public static ResourcesSpec merge(ResourcesSpec partitionSpec, ResourcesSpec defaultSpec) {
        if (partitionSpec == null)
            return defaultSpec;
        if (defaultSpec == null)
            return partitionSpec;
        ResourcesSpec result = new ResourcesSpec();
        result.id = partitionSpec.id;
        result.limits = ResourcesValuesSpec.merge(partitionSpec.limits, defaultSpec.limits);
        result.requests = ResourcesValuesSpec.merge(partitionSpec.requests, defaultSpec.requests);
        return result;
    }

    public Integer getId() {
        return id;
    }
    public void setId(Integer id) {
        this.id = id;
    }

    public ResourcesValuesSpec getLimits() {
        return limits;
    }
    public void setLimits(ResourcesValuesSpec limits) {
        this.limits = limits;
    }

    public ResourcesValuesSpec getRequests() {
        return requests;
    }
    public void setRequests(ResourcesValuesSpec requests) {
        this.requests = requests;
    }
}
