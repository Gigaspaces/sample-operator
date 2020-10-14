package com.gigaspaces.k8s.operators.pu;

import io.fabric8.kubernetes.client.CustomResource;

public class PuResource extends CustomResource {
    private PuSpec spec;

    public PuSpec getSpec() {
        return spec;
    }

    public void setSpec(PuSpec spec) {
        this.spec = spec;
    }
}
