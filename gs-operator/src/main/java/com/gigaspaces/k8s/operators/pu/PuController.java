package com.gigaspaces.k8s.operators.pu;

import com.gigaspaces.k8s.operators.ListBuilder;
import com.gigaspaces.k8s.operators.MapBuilder;
import com.github.containersolutions.operator.api.Context;
import com.github.containersolutions.operator.api.Controller;
import com.github.containersolutions.operator.api.ResourceController;
import com.github.containersolutions.operator.api.UpdateControl;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller(crdName = "pus.gigaspaces.com", customResourceClass = Pu.class)
public class PuController implements ResourceController<Pu> {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final KubernetesClient kubernetesClient;

    String xap_pu_name = "xap-pu";
    String image = "gigaspaces/xap-enterprise:15.8.0-m6";
    String chart = "xap-pu-15.8.0-m6";

    public PuController(KubernetesClient kubernetesClient) {
        this.kubernetesClient = kubernetesClient;
    }

    @Override
    public boolean deleteResource(Pu pu, Context<Pu> context) {
        log.info("\n===> deleteResource \n" + pu);

        PuSpec spec = pu.getSpec();
        int partitions = spec.getPartitions();
        String namespace = pu.getMetadata().getNamespace();
        for (int i = 0; i < partitions; i++) {
            String name = getStatefulSetName(pu, i);
            StatefulSet exists = kubernetesClient.apps().statefulSets().inNamespace(namespace).withName(name).get();
            if (exists == null) {
                log.info("stateful set '" + name + "' does not exist");
            } else {
                Boolean delete = kubernetesClient.apps().statefulSets().inNamespace(namespace).withName(name).delete();
                log.info("Deleted (" + delete + ") StatefulSet with name " + exists.getMetadata().getName());
            }
        }
        return true;
    }

    @Override
    public UpdateControl createOrUpdateResource(Pu pu, Context<Pu> context) {
        log.info("\n===> createOrUpdateResource \n" + pu);

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

        pu.getMetadata().getName();
        PuSpec spec = pu.getSpec();

        int partitions = spec.getPartitions();
        int created = 0;
        for (int i = 0; i < partitions; i++) {
            if (createStatefulSet(pu, i))
                created++;
        }
        return created == 0 ? UpdateControl.noUpdate() : UpdateControl.updateStatusSubResource(pu);
    }

    private String getStatefulSetName(Pu pu, int partition) {
        return pu.getMetadata().getName() + "-" + xap_pu_name + "-" + partition;
    }

    private boolean createStatefulSet(Pu pu, int partition) {
        String name = getStatefulSetName(pu, partition);
        String namespace = pu.getMetadata().getNamespace();
        StatefulSet exists = kubernetesClient.apps().statefulSets().inNamespace(namespace).withName(name).get();
        if (exists != null) {
            log.info("stateful set '" + name + "' already exists");
            return false;
        }

        PuSpec spec = pu.getSpec();
        StatefulSetBuilder statefulSet = new StatefulSetBuilder();
        statefulSet.withNewMetadata()
                .withNamespace(namespace)
                .withName(name)
                .withLabels(new MapBuilder<String,String>()
                        .put("app", xap_pu_name)
                        .put("chart", chart)
                        .put("release", pu.getMetadata().getName())
                        .build())
                .endMetadata();
        statefulSet.withNewSpec()
                .withReplicas(spec.isHa() ? 2 : 1)
                .withServiceName(xap_pu_name)
                .withNewSelector()
                .withMatchLabels(MapBuilder.singletonMap("selectorId", name))
                .endSelector()
                .withNewTemplate()
                .withNewMetadata()
                .withLabels(new MapBuilder<String,String>()
                        .put("app", xap_pu_name)
                        .put("release", pu.getMetadata().getName())
                        .put("component", "space")
                        .put("selectorId", name)
                        .put("partitionId", "1")
                        .build())
                .endMetadata()
                .withNewSpec()
                //.withNewAffinity() //TODO
                .withRestartPolicy("Always")
                .withTerminationGracePeriodSeconds(30L)
                .withContainers(getContainer(pu))
                .endSpec()
                .endTemplate()
                .endSpec();

        StatefulSet item = statefulSet.build();
        StatefulSet created = kubernetesClient.apps().statefulSets().inNamespace(namespace).create(item);
        log.info("created StatefulSet with name " + created.getMetadata().getName());
        return true;
    }

    private Container getContainer(Pu pu) {
        PuSpec spec = pu.getSpec();
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
                .add("name=" + pu.getMetadata().getName())
                .add("release.namespace=" + pu.getMetadata().getNamespace())
                .add("license=" + spec.getLicense())
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
