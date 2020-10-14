package com.gigaspaces.k8s.operators;

import com.github.containersolutions.operator.Operator;
import com.gigaspaces.k8s.operators.pu.PuController;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;

/**
 * Class intended for running operator manually
 */
public class DevOperator {
    public static void main(String[] args) {
        KubernetesClient client = new DefaultKubernetesClient();
        Operator operator = new Operator(client);
        operator.registerController(new PuController(client));
    }
}
