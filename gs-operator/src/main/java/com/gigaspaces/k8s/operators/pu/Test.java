package com.gigaspaces.k8s.operators.pu;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class Test {
    public static final String MODULE_DIR = "/Users/meron/Work/minikube/sample-operator/gs-operator";
    public static final String EXAMPLES_PU_YAML = MODULE_DIR+"/examples/pu.yaml";

    public static void main(String[] args) {
        new Test();
    }

    public Test() {
        KubernetesClient client = new DefaultKubernetesClient();
        test(client);
    }

    public void test(KubernetesClient kubernetesClient)  {

        CustomResourceDefinition customResourceDefinition = kubernetesClient
                .customResourceDefinitions()
                .withName("pus.gigaspaces.com")
                .get();

        if (customResourceDefinition == null) {
            System.out.println("crd does not exist");
            //introduceCRD(kubernetesClient);
            return;
        }

        CustomResourceDefinitionContext crdContext = new CustomResourceDefinitionContext
                .Builder()
                .withGroup("gigaspaces.com")
                .withKind("ProcessingUnit")
                .withPlural("pus")
                .withScope("Namespaced")
                .withVersion("v1")
                .build();

        //typedAPI(kubernetesClient, crdContext);
        typelessAPI(kubernetesClient, crdContext);

    }

    private void introduceCRD(KubernetesClient kubernetesClient) {
        try (InputStream resourceAsStream = this.getClass().getResourceAsStream("/pu-crd.yaml")) {
            CustomResourceDefinition crd = kubernetesClient.customResourceDefinitions().load(resourceAsStream).get();
            kubernetesClient.customResourceDefinitions().create(crd);
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    private void typelessAPI(KubernetesClient kubernetesClient, CustomResourceDefinitionContext crdContext) {
        try {
            File f = new File(EXAMPLES_PU_YAML);
            FileInputStream fileInputStream = new FileInputStream(f);
            // Load from Yaml
            Map<String, Object> dummyObject = kubernetesClient.customResource(crdContext)
                    .load(fileInputStream);
            // Create Custom Resource
            kubernetesClient.customResource(crdContext).create("default", dummyObject);
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    private void typedAPI(KubernetesClient kubernetesClient, CustomResourceDefinitionContext crdContext) {
        Pu demoPu2 = new Pu();
        demoPu2.setKind("ProcessingUnit");
        demoPu2.setMetadata(new ObjectMetaBuilder().withName("demo2").build());

        PuSpec puSpec = new PuSpec();
        puSpec.setHa(false);
        puSpec.setPartitions(1);
        puSpec.setLicense("tryme");
        demoPu2.setSpec(puSpec);

        MixedOperation<Pu, PuList, DoneablePu, Resource<Pu, DoneablePu>> mixedOperation =
                kubernetesClient.customResources(crdContext, Pu.class, PuList.class, DoneablePu.class);
        mixedOperation.inNamespace("default").createOrReplace(demoPu2);
    }
}
