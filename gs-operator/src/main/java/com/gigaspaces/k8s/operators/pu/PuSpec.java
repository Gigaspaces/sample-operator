package com.gigaspaces.k8s.operators.pu;

public class PuSpec {
    private String license;
    private int partitions;
    private boolean ha;

    public String getLicense() {
        return license;
    }
    public void setLicense(String license) {
        this.license = license;
    }

    public int getPartitions() {
        return partitions;
    }
    public void setPartitions(int partitions) {
        this.partitions = partitions;
    }

    public boolean isHa() {
        return ha;
    }
    public void setHa(boolean ha) {
        this.ha = ha;
    }

    public String getImage() {
        // TODO: Configurable
        // TODO: xap vs. insightedge
        return "gigaspaces/xap-enterprise:" + getVersion();
    }

    public String getChart() {
        // TODO: is this required, or just leftovers from helm? if so, configurable
        return getApp() + "-" + getVersion();
    }

    public String getApp() {
        // TODO: xap vs. insightedge
        return "xap-pu";
    }

    public String getManagerName() {
        // TODO: Configurable
        return "hello";
    }

    public int getManagerApiPort() {
        // TODO: Configurable
        return 8090;
    }

    private String getVersion() {
        // TODO: infer from platform version via dependency?
        return "15.5.1";
    }
}
