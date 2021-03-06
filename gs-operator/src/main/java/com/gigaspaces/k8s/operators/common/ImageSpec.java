package com.gigaspaces.k8s.operators.common;

import com.gigaspaces.k8s.operators.ProductInfo;

public class ImageSpec {
    private String repository;
    private String tag;
    private String pullPolicy;

    public void applyDefaults(String productType, String productVersion) {
        if (repository == null)
            repository = "gigaspaces/" + productType + "-enterprise";
        if (tag == null)
            tag = productVersion;
    }

    @Override
    public String toString() {
        return repository + ":" + tag;
    }

    public String getRepository() {
        return repository;
    }
    public void setRepository(String repository) {
        this.repository = repository;
    }

    public String getTag() {
        return tag;
    }
    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getPullPolicy() {
        return pullPolicy;
    }
    public void setPullPolicy(String pullPolicy) {
        this.pullPolicy = pullPolicy;
    }
}
