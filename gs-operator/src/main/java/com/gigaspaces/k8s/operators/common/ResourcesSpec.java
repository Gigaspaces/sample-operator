package com.gigaspaces.k8s.operators.common;

public class ResourcesSpec {
    private ResourcesValuesSpec limits;
    private ResourcesValuesSpec requests;

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
