package com.github.containersolutions.operator.sample.pu;

import com.github.containersolutions.operator.api.Context;
import com.github.containersolutions.operator.api.Controller;
import com.github.containersolutions.operator.api.ResourceController;
import com.github.containersolutions.operator.api.UpdateControl;
import com.github.containersolutions.operator.sample.ListBuilder;
import com.github.containersolutions.operator.sample.MapBuilder;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller(
        crdName = "pus.pu.sample.javaoperatorsdk",
        customResourceClass = PuResource.class)
public class PuController implements ResourceController<PuResource> {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private KubernetesClient kubernetesClient;

    String xap_pu_name = "xap-pu";
    String pod_suffix = "0";
    String xap_pu_fullname = "world-xap-pu";
    String xap_pu_fullname_pod_suffix = "world-xap-pu-0";
    String xap_pu_service_fullname_pod_suffix = "world-xap-pu-0-service";
    String release = "world";
    String image = "gigaspaces/xap-enterprise:15.8.0-m6";
    String chart = "xap-pu-15.8.0-m6";
    String namespace = "default";

    public PuController(KubernetesClient kubernetesClient) {
        this.kubernetesClient = kubernetesClient;
    }

    @Override
    public boolean deleteResource(PuResource puResource, Context<PuResource> context) {
        log.info("\n===> deleteResource \n" + puResource);

        StatefulSet exists = kubernetesClient.apps().statefulSets().inNamespace(namespace)
                .withName(xap_pu_fullname_pod_suffix).get();
        if (exists == null) {
            log.info("stateful set does not exist");
            return true;
        }

        Boolean delete = kubernetesClient.apps().statefulSets().inNamespace(namespace).withName(xap_pu_fullname_pod_suffix).delete();
        log.info("Deleted ("+delete+") StatefulSet with name " + exists.getMetadata().getName());
        return true;
    }

    @Override
    public UpdateControl createOrUpdateResource(PuResource puResource, Context<PuResource> context) {
        log.info("\n===> createOrUpdateResource \n" + puResource);

//        Service serviceExists = kubernetesClient.services().withName(xap_pu_service_fullname_pod_suffix).get();
//        if (serviceExists != null) {
//            log.info("service already exists");
//        } else {
//
//            ServiceBuilder serviceBuilder = new ServiceBuilder();
//            serviceBuilder.withNewMetadata()
//                    .withName(xap_pu_service_fullname_pod_suffix)
//                    .withLabels(new MapBuilder<String, String>()
//                            .put("app", xap_pu_name)
//                            .put("chart", chart)
//                            .put("release", release)
//                            .build())
//                    .endMetadata();
//            serviceBuilder.withNewSpec()
//                    .withType("LoadBalancer")
//                    .withSelector(new MapBuilder<String, String>()
//                            .put("statefulset.kubernetes.io/pod-name", xap_pu_service_fullname_pod_suffix)
//                            .put("app", xap_pu_name)
//                            .put("selectorId", xap_pu_fullname_pod_suffix)
//                            .put("partitionId", "1")
//                            .build())
//                    .withPorts(buildServicePort())
//                    .endSpec();
//
//        }

        StatefulSet exists = kubernetesClient.apps().statefulSets().inNamespace(namespace)
                .withName(xap_pu_fullname_pod_suffix).get();
        if (exists != null) {
            log.info("stateful set already exists");
            return UpdateControl.noUpdate();
        }

        StatefulSetBuilder statefulSet = new StatefulSetBuilder();
        statefulSet.withNewMetadata()
                .withNamespace(namespace)
                .withName(xap_pu_fullname_pod_suffix)
                .withLabels(new MapBuilder<String,String>()
                        .put("app", xap_pu_name)
                        .put("chart", chart)
                        .put("release", release)
                        .build())
                .endMetadata();
        statefulSet.withNewSpec()
                .withReplicas(1)
                .withServiceName(xap_pu_name)
                .withNewSelector()
                    .withMatchLabels(MapBuilder.singletonMap("selectorId", xap_pu_fullname_pod_suffix))
                .endSelector()
                .withNewTemplate()
                    .withNewMetadata()
                        .withLabels(new MapBuilder<String,String>()
                                .put("app", xap_pu_name)
                                .put("release", release)
                                .put("component", "space")
                                .put("selectorId", xap_pu_fullname_pod_suffix)
                                .put("partitionId", "1")
                                .build())
                    .endMetadata()
                    .withNewSpec()
                        //.withNewAffinity() //TODO
                        .withRestartPolicy("Always")
                        .withTerminationGracePeriodSeconds(30L)
                        .withContainers(getContainer())
                    .endSpec()
                .endTemplate()
                .endSpec();

        StatefulSet item = statefulSet.build();
        StatefulSet created = kubernetesClient.apps().statefulSets().inNamespace(namespace).create(item);
        log.info("created StatefulSet with name " + created.getMetadata().getName());

        return UpdateControl.updateStatusSubResource(puResource);
    }

    private Container getContainer() {
        Container container = new Container();
        container.setName("pu-container");
        container.setResources(new ResourceRequirements(
                MapBuilder.singletonMap("memory", new Quantity("400", "Mi")),
                MapBuilder.singletonMap("memory", new Quantity("400", "Mi"))));
        container.setImage(image);
        container.setEnv(ListBuilder.singletonList(new EnvVar("GS_OPTIONS_EXT", null, null)));

        container.setCommand(ListBuilder.singletonList("tools/kubernetes/entrypoint.sh"));
        container.setArgs(new ListBuilder<String>()
                .add("component=pu")
                .add("verbose=true")
                .add("name=world")
                .add("release.namespace="+namespace)
                .add("license=tryme")
                .add("partitionId=1")
                .add("java.heap=limit-150Mi")
                .add("manager.name=hello")
                .add("manager.ports.api=8090")
                .build());

        //by default - disabled
        //container.setLivenessProbe(buildLivenessProbe());
        //container.setReadinessProbe(getReadinessProbe());

        return container;
    }

    private ServicePort buildServicePort() {
        ServicePort port = new ServicePort();
        port.setName("lrmi");
        port.setProtocol("TCP");
        port.setPort(8200);
        //port.setNodePort();
        return port;
    }

    private Probe buildLivenessProbe() {
        Probe probe = new Probe();
        HTTPGetAction httpGetAction = new HTTPGetAction();
        httpGetAction.setPath("/probes/alive");
        httpGetAction.setPort(new IntOrString(8089));
        probe.setHttpGet(httpGetAction);
        probe.setInitialDelaySeconds(30);
        probe.setPeriodSeconds(5);
        probe.setFailureThreshold(3);
        return probe;
    }


    private Probe getReadinessProbe() {
        Probe probe = new Probe();
        HTTPGetAction httpGetAction = new HTTPGetAction();
        httpGetAction.setPath("/probes/ready");
        httpGetAction.setPort(new IntOrString(8089));
        probe.setHttpGet(httpGetAction);
        probe.setInitialDelaySeconds(30);
        probe.setPeriodSeconds(5);
        probe.setFailureThreshold(3);
        return probe;
    }
}
