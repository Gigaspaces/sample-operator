package com.gigaspaces.k8s.operators.common;

public class NodeSelectorSpec {
    private Boolean enabled;
    private String selector;


    public String getSelector() {
        return selector;
    }

    public void setSelector(String selector) {
        this.selector = selector;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

}
