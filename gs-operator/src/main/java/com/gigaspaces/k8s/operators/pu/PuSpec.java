package com.gigaspaces.k8s.operators.pu;

import com.gigaspaces.k8s.operators.ProductInfo;
import com.gigaspaces.k8s.operators.common.ImageSpec;

public class PuSpec {
    private ImageSpec image;
    private String license;
    private int partitions;
    private boolean ha;

    public PuSpec applyDefaults() {
        if (image == null)
            image = new ImageSpec();
        image.applyDefaults();
        return this;
    }

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

    public ImageSpec getImage() {
        return image;
    }
    public void setImage(ImageSpec image) {
        this.image = image;
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
