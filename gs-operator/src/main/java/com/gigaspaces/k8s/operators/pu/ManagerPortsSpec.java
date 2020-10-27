package com.gigaspaces.k8s.operators.pu;

public class ManagerPortsSpec {
    private static final int NOT_SET = -1;
    private int api = NOT_SET;

    public void applyDefaults() {
        if (api == NOT_SET)
            api = 8090;
    }

    public int getApi() {
        return api;
    }
    public void setApi(int api) {
        this.api = api;
    }
}
