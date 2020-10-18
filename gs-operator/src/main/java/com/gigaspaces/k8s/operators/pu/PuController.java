package com.gigaspaces.k8s.operators.pu;

import com.gigaspaces.k8s.operators.ListBuilder;
import com.gigaspaces.k8s.operators.MapBuilder;
import com.github.containersolutions.operator.api.Context;
import com.github.containersolutions.operator.api.Controller;
import com.github.containersolutions.operator.api.ResourceController;
import com.github.containersolutions.operator.api.UpdateControl;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.DoneableStatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.RollableScalableResource;
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
        for (int i = 0; i < partitions; i++) {
            String name = getStatefulSetName(pu, i);
            StatefulSet exists = statefulSet(pu, name).get();
            if (exists == null) {
                log.info("stateful set '" + name + "' does not exist");
            } else {
                Boolean delete = statefulSet(pu, name).delete();
                log.info("Deleted (" + delete + ") StatefulSet with name " + exists.getMetadata().getName());
            }
        }
        return true;
    }

    @Override
    public UpdateControl<Pu> createOrUpdateResource(Pu pu, Context<Pu> context) {
        log.info("\n===> createOrUpdateResource \n" + pu);
        return pu.getMetadata().getGeneration() == 1 ? create(pu) : update(pu);
    }

    private UpdateControl<Pu> create(Pu pu) {
        log.info("DEBUG - creating pu {}", pu.getMetadata().getName());
        // TODO: create zk topology if needed.
        PuSpec spec = pu.getSpec();
        int partitions = spec.getPartitions();
        int created = 0;
        for (int i = 0; i < partitions; i++) {
            StatefulSet statefulSet = statefulSet(pu, i).get();
            if (statefulSet == null) {
                createStatefulSet(pu, i);
                created++;
            } else {
                log.info("stateful set '" + statefulSet.getMetadata().getName() + "' already exists");
            }
        }
        return created == 0 ? UpdateControl.noUpdate() : UpdateControl.updateStatusSubResource(pu);
    }

    private UpdateControl<Pu> update(Pu pu) {
        if (isHorizontalScale(pu)) {
            log.info("Horizontal scaling is under construction"); // TODO: implement.
        } else {
            log.info("DEBUG - Updating pu {} to generation {}", pu.getMetadata().getName(), pu.getMetadata().getGeneration());
            PuSpec spec = pu.getSpec();
            int partitions = spec.getPartitions();
            int modifications = 0;
            for (int i = 0; i < partitions; i++) {
                StatefulSet statefulSet = statefulSet(pu, i).get();
                if (statefulSet == null) {
                    createStatefulSet(pu, i);
                    modifications++;
                } else {
                    if (updateStatefulSet(statefulSet, pu))
                        modifications++;
                }
            }
            return modifications == 0 ? UpdateControl.noUpdate() : UpdateControl.updateStatusSubResource(pu);
        }
        return null;
    }

    private boolean isHorizontalScale(Pu pu) {
        // TODO: fetch topology from zk and compare number of partitions
        return false;
    }

    private boolean updateStatefulSet(StatefulSet statefulSet, Pu pu) {
        log.info("DEBUG - updating statefulset {} from {} to {}",
                statefulSet.getMetadata().getName(),
                statefulSet.getMetadata().getGeneration(),
                pu.getMetadata().getGeneration());
        // TODO: update stateful set.
        /*
        statefulSet(pu, statefulSet.getMetadata().getName()).edit()
                ...
                .done();
         */
        return true;
    }

    private String getStatefulSetName(Pu pu, int partition) {
        return pu.getMetadata().getName() + "-" + xap_pu_name + "-" + partition;
    }

    private RollableScalableResource<StatefulSet, DoneableStatefulSet> statefulSet(Pu pu, int partition) {
        return statefulSet(pu, getStatefulSetName(pu, partition));
    }

    private RollableScalableResource<StatefulSet, DoneableStatefulSet> statefulSet(Pu pu, String name) {
        return kubernetesClient.apps().statefulSets()
                .inNamespace(pu.getMetadata().getNamespace())
                .withName(name);
    }

    private void createStatefulSet(Pu pu, int partition) {
        String name = getStatefulSetName(pu, partition);
        String namespace = pu.getMetadata().getNamespace();
        log.info("DEBUG - Creating statefulset {} with generation {}", name, pu.getMetadata().getGeneration());

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
