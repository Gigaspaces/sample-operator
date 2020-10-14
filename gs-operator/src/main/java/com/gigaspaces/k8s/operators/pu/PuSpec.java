package com.gigaspaces.k8s.operators.pu;

public class PuSpec {
    private String license;
    private boolean ha;

    public String getLicense() {
        return license;
    }
    public void setLicense(String license) {
        this.license = license;
    }

    public boolean isHa() {
        return ha;
    }
    public void setHa(boolean ha) {
        this.ha = ha;
    }
}
