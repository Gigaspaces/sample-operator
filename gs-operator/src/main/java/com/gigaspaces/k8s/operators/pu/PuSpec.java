package com.gigaspaces.k8s.operators.pu;

import com.gigaspaces.k8s.operators.ProductInfo;
import com.gigaspaces.k8s.operators.common.ImageSpec;
import com.gigaspaces.k8s.operators.common.ResourcesSpec;

public class PuSpec {
    private ImageSpec image;
    private ResourcesSpec resources;
    private ManagerSpec manager;
    private String license;
    private int partitions;
    private boolean ha;

    public PuSpec applyDefaults() {
        if (image == null)
            image = new ImageSpec();
        image.applyDefaults();
        if (manager == null)
            manager = new ManagerSpec();
        manager.applyDefaults();
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

    public ResourcesSpec getResources() {
        return resources;
    }

    public void setResources(ResourcesSpec resources) {
        this.resources = resources;
    }

    public ManagerSpec getManager() {
        return manager;
    }

    public void setManager(ManagerSpec manager) {
        this.manager = manager;
    }
}
