package com.gigaspaces.k8s.operators;

public class ProductInfo {
    private static final ProductInfo instance = new ProductInfo();

    public static ProductInfo instance() {
        return instance;
    }

    public String getProductName() {
        // TODO: infer xap vs. insightedge from platform version
        return "xap";
    }
    public String getVersion() {
        // TODO: infer from platform version
        return "15.5.1";
    }
}
