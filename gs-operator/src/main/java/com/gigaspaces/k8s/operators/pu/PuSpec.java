package com.gigaspaces.k8s.operators.pu;

import com.gigaspaces.k8s.operators.ProductInfo;
import com.gigaspaces.k8s.operators.common.ImageSpec;
import com.gigaspaces.k8s.operators.common.ResourcesSpec;

import java.util.List;
import java.util.Objects;

public class PuSpec {
    private ImageSpec image;
    private List<ResourcesSpec> resources;
    private ManagerSpec manager;
    private String license;
    private String resourceUrl;
    private Integer partitions;
    private Integer instances;
    private boolean ha;
    private boolean antiAffinity;

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
    public String getResourceUrl() {
        return resourceUrl;
    }
    public void setResourceUrl(String resourceUrl) {
        this.resourceUrl = resourceUrl;
    }

    public Integer getPartitions() {
        return partitions;
    }
    public void setPartitions(int partitions) {
        this.partitions = partitions;
    }
    public Integer getInstances() {
        return instances;
    }
    public void setInstances(int instances) {
        this.instances = instances;
    }

    public boolean isHa() {
        return ha;
    }
    public void setHa(boolean ha) {
        this.ha = ha;
    }
    public boolean isAntiAffinity() {
        return antiAffinity;
    }
    public void setAntiAffinity(boolean antiAffinity) {
        this.antiAffinity = antiAffinity;
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

    public List<ResourcesSpec> getResources() {
        return resources;
    }

    public void setResources(List<ResourcesSpec> resources) {
        this.resources = resources;
    }

    public ResourcesSpec getResources(Integer id) {
        if (resources != null) {
            for (ResourcesSpec resource : resources) {
                if (Objects.equals(id, resource.getId()))
                    return resource;
            }
        }
        return null;
    }

    public ManagerSpec getManager() {
        return manager;
    }

    public void setManager(ManagerSpec manager) {
        this.manager = manager;
    }
}
