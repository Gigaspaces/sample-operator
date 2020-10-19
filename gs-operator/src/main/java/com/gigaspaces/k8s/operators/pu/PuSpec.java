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
        return "gigaspaces/xap-enterprise:15.8.0-m6";
    }

    public String getChart() {
        // TODO: is this required, or just leftovers from helm? if so, configurable
        // TODO: xap vs. insightedge
        return "xap-pu-15.8.0-m6";
    }

    public String getApp() {
        // TODO: xap vs. insightedge
        return "xap-pu";
    }
}
