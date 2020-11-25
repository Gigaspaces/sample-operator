package com.gigaspaces.k8s.operators.common;

public class MemoryXtendSpec {
    private Boolean enabled;
    private VolumeMount volumeMount;

    public VolumeClaimTemplate getVolumeClaimTemplate() {
        return volumeClaimTemplate;
    }

    private VolumeClaimTemplate volumeClaimTemplate;

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public VolumeMount getVolumeMount() {
        return volumeMount;
    }

    public void setVolumeMount(VolumeMount volumeMount) {
        this.volumeMount = volumeMount;
    }

    public static class VolumeMount {
        private String name;
        private String mountPath;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getMountPath() {
            return mountPath;
        }

        public void setMountPath(String mountPath) {
            this.mountPath = mountPath;
        }
    }

    public static class VolumeClaimTemplate {
        private String storage;
        private String storageClassName;
        private String accessModes;

        public String getStorage() {
            return storage;
        }

        public void setStorage(String storage) {
            this.storage = storage;
        }

        public String getStorageClassName() {
            return storageClassName;
        }

        public String getAccessModes() {
            return accessModes;
        }

        public void setAccessModes(String accessModes) {
            this.accessModes = accessModes;
        }

        public String getPersistentVolumeReclaimPolicy() {
            return persistentVolumeReclaimPolicy;
        }

        public void setPersistentVolumeReclaimPolicy(String persistentVolumeReclaimPolicy) {
            this.persistentVolumeReclaimPolicy = persistentVolumeReclaimPolicy;
        }

        private String persistentVolumeReclaimPolicy;

    }
}


