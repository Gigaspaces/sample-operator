package com.gigaspaces.k8s.operators.pu;

public class ManagerSpec {
    private static final int NOT_SET = -1;
    private String name;
    private ManagerPortsSpec ports;
    private int discoveryTimeoutSeconds = NOT_SET;

    public void applyDefaults() {
        // TODO: come up with a proper default for manager name.
        if (name == null)
            name = "hello";
        if (ports == null)
            ports = new ManagerPortsSpec();
        ports.applyDefaults();
        if (discoveryTimeoutSeconds == NOT_SET)
            discoveryTimeoutSeconds = 60;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public ManagerPortsSpec getPorts() {
        return ports;
    }
    public void setPorts(ManagerPortsSpec ports) {
        this.ports = ports;
    }

    public int getDiscoveryTimeoutSeconds() {
        return discoveryTimeoutSeconds;
    }
    public void setDiscoveryTimeoutSeconds(int discoveryTimeoutSeconds) {
        this.discoveryTimeoutSeconds = discoveryTimeoutSeconds;
    }
}
