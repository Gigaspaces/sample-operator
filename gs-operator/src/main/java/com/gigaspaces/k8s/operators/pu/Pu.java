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
        if (isStateful() && spec.getPartitions() != 0) {
            return getMetadata().getName() + "-" + getSpec().getApp() + "-" + partition;
        }
        return getMetadata().getName() + "-" + getSpec().getApp();
    }

    public String getServiceName(String partitionId, int statefulSetId) {
        if (isStateful() && spec.getPartitions() != 0) {
            return getMetadata().getName() + "-" + getSpec().getApp() + "-" + statefulSetId + partitionId + "-service";
        }
        return getMetadata().getName() + "-" + getSpec().getApp() + "-" + statefulSetId + "-service";
    }
    public String getVolumeName(String partitionId, int statefulSetId) {
        if (isStateful() && spec.getPartitions() != 0) {
            return spec.getMemoryXtendVolume().getVolumeMount().getName() + "-" + getMetadata().getName() + "-" + getSpec().getApp() + "-" + statefulSetId + "-" +  partitionId;
        }
        return spec.getMemoryXtendVolume().getVolumeMount().getName() + "-" + getMetadata().getName() + "-" + getSpec().getApp() + "-" + statefulSetId;
    }

    public boolean isStateful() {
        Integer partitions = spec.getPartitions();
        Integer instances = spec.getInstances();
        if (partitions != null && partitions > 0)
            return true;
        else return partitions != null && partitions == 0 && (instances == null || instances == 0);
    }
}