package com.gigaspaces.k8s.operators.pu;

import io.fabric8.kubernetes.api.builder.Function;
import io.fabric8.kubernetes.client.CustomResourceDoneable;

public class DoneablePu extends CustomResourceDoneable<Pu> {
    public DoneablePu(Pu resource, Function<Pu, Pu> function) {
        super(resource, function);
    }
}
