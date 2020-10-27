package com.gigaspaces.k8s.operators.pu;

import com.gigaspaces.k8s.operators.ProductInfo;

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
        return "gigaspaces/" + ProductInfo.instance().getProductName() + "-enterprise:" + ProductInfo.instance().getVersion();
    }

    public String getApp() {
        return ProductInfo.instance().getProductName() + "-pu";
    }

    public String getManagerName() {
        // TODO: Configurable
        return "hello";
    }

    public int getManagerApiPort() {
        // TODO: Configurable
        return 8090;
    }
}
