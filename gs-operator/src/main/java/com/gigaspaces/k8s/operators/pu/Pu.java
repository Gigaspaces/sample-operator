package com.gigaspaces.k8s.operators.pu;

import io.fabric8.kubernetes.client.CustomResource;

public class Pu extends CustomResource {
    private PuSpec spec;

    public PuSpec getSpec() {
        return spec;
    }

    public void setSpec(PuSpec spec) {
        this.spec = spec;
    }

    public String getStatefulSetName(int partition) {
        return getMetadata().getName() + "-" + getSpec().getApp() + "-" + partition;
    }

    public boolean isStateful() {
        // TODO: user indication if pu is stateful or not, or different CRDs per pu type.
        return true;
    }
}
