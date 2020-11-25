package com.gigaspaces.k8s.operators.common;

public class ServiceSpec {
    private String type;
    private LrmiSpec lrmi;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public LrmiSpec getLrmi() {
        return lrmi;
    }

    public void setLrmi(LrmiSpec lrmi) {
        this.lrmi = lrmi;
    }

    public class LrmiSpec {
        private Boolean enabled;
        private int port;

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public int getInitialNodePort() {
            return initialNodePort;
        }

        public void setInitialNodePort(int initialNodePort) {
            this.initialNodePort = initialNodePort;
        }

        private int initialNodePort;

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }
    }


}
