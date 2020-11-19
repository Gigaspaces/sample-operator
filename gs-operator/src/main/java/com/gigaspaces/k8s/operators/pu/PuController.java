package com.gigaspaces.k8s.operators.pu;

import com.gigaspaces.k8s.operators.ListBuilder;
import com.gigaspaces.k8s.operators.MapBuilder;
import com.gigaspaces.k8s.operators.common.ResourcesSpec;
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

import java.util.*;

@Controller(crdName = "pus.gigaspaces.com", customResourceClass = Pu.class)
public class PuController implements ResourceController<Pu> {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final KubernetesClient kubernetesClient;

    public PuController(KubernetesClient kubernetesClient) {
        this.kubernetesClient = kubernetesClient;
    }

    @Override
    public boolean deleteResource(Pu pu, Context<Pu> context) {
        log.info("\n===> deleteResource \n" + pu);

        PuSpec spec = pu.getSpec().applyDefaults();
        int partitions = spec.getPartitions();
        if (partitions == 0)
            deleteStatefulSet(pu, 0);
        else {
            for (int i = 1; i <= partitions; i++) {
                deleteStatefulSet(pu, i);
            }
        }
        return true;
    }

    private void deleteStatefulSet(Pu pu, int partitionId) {
        String name = pu.getStatefulSetName(partitionId);
        StatefulSet exists = statefulSet(pu, name).get();
        if (exists == null) {
            log.info("stateful set '" + name + "' does not exist");
        } else {
            Boolean delete = statefulSet(pu, name).delete();
            log.info("Deleted (" + delete + ") StatefulSet with name " + exists.getMetadata().getName());
        }
    }

    @Override
    public UpdateControl<Pu> createOrUpdateResource(Pu pu, Context<Pu> context) {
        log.info("\n===> createOrUpdateResource \n" + pu);
        pu.getSpec().applyDefaults();
        if (isHorizontalScale(pu)) {
            log.info("Horizontal scaling is under construction"); // TODO: implement.
            return UpdateControl.noUpdate();
        }

        log.info("DEBUG - Creating/updating pu {} to generation {}", pu.getMetadata().getName(), pu.getMetadata().getGeneration());
        PuSpec spec = pu.getSpec();
        int modifications = 0;

        if (!pu.isStateful()) {
            if (createOrUpdateStatefulSet(pu, 1)) {
                modifications++;
            }
        } else {
            int partitions = spec.getPartitions();
            if (partitions == 0) {
                if (createOrUpdateStatefulSet(pu, 0)) {
                    modifications++;
                }
            } else {
                for (int i = 1; i <= partitions; i++) {
                    if (createOrUpdateStatefulSet(pu, i))
                        modifications++;
                }
            }
        }
        return modifications == 0 ? UpdateControl.noUpdate() : UpdateControl.updateStatusSubResource(pu);
    }

    private boolean createOrUpdateStatefulSet(Pu pu, int statefulSetId) {
        StatefulSet statefulSet = statefulSet(pu, statefulSetId).get();
        if (statefulSet == null) {
            createStatefulSet(pu, statefulSetId);
            return true;
        }
        return updateStatefulSet(statefulSet, pu, statefulSetId);
    }

    private boolean updateStatefulSet(StatefulSet statefulSet, Pu pu, int partitionId) {
        StatefulSet updated = kubernetesClient.apps().statefulSets()
                .inNamespace(statefulSet.getMetadata().getNamespace())
                .withName(statefulSet.getMetadata().getName())
                //.rolling()
                .edit()
                .editSpec()
                .editTemplate()
                .editSpec()
                .editFirstContainer()
                .withImage(pu.getSpec().getImage().toString())
                .withResources(getResourceRequirements(pu.getSpec(), partitionId))
                .withArgs(getContainerArgs(pu, partitionId))
                .withEnv(getContainerEnvVars(pu, partitionId))
                .endContainer()
                .endSpec()
                .endTemplate()
                .endSpec()
                .done();
        boolean result = updated.getMetadata().getGeneration() > statefulSet.getMetadata().getGeneration();
        log.info("DEBUG - partition: {} updated={}, orig gen: {}, new gen: {}", partitionId, result, statefulSet.getMetadata().getGeneration(), updated.getMetadata().getGeneration());
        return result;
    }

    private RollableScalableResource<StatefulSet, DoneableStatefulSet> statefulSet(Pu pu, int partition) {
        return statefulSet(pu, pu.getStatefulSetName(partition));
    }

    private RollableScalableResource<StatefulSet, DoneableStatefulSet> statefulSet(Pu pu, String name) {
        return kubernetesClient.apps().statefulSets()
                .inNamespace(pu.getMetadata().getNamespace())
                .withName(name);
    }

    private void createStatefulSet(Pu pu, int partition) {
        String name = pu.getStatefulSetName(partition);
        String namespace = pu.getMetadata().getNamespace();
        log.info("DEBUG - Creating statefulset {} with generation {}", name, pu.getMetadata().getGeneration());
        PuSpec spec = pu.getSpec();
        int replicas = 0;
        if (pu.isStateful()) {
            replicas = spec.isHa() ? 2 : 1;
        } else {
            replicas = spec.getInstances();
        }

        StatefulSetBuilder statefulSet = new StatefulSetBuilder();
        statefulSet.withNewMetadata()
                .withNamespace(namespace)
                .withName(name)
                .withLabels(new MapBuilder<String, String>()
                        .put("app", spec.getApp())
                        .put("release", pu.getMetadata().getName())
                        .build())
                .endMetadata();
        statefulSet.withNewSpec()
                .withReplicas(replicas)
                .withServiceName(spec.getApp())
                .withNewSelector()
                .withMatchLabels(MapBuilder.singletonMap("selectorId", name))
                .endSelector()
                .withNewTemplate()
                .withNewMetadata()
                .withLabels(new MapBuilder<String, String>()
                        .put("app", spec.getApp())
                        .put("release", pu.getMetadata().getName())
                        .put("component", "space")
                        .put("selectorId", name)
                        .put("partitionId", String.valueOf(partition))
                        .build())
                .endMetadata()
                .withNewSpec()
                .withRestartPolicy("Always")
                .withAffinity(addAntiAffinity(spec.isAntiAffinity(), name))
                .withTerminationGracePeriodSeconds(30L)
                .withContainers(getContainer(pu, partition))
                .endSpec()
                .endTemplate()
                .endSpec();

        StatefulSet item = statefulSet.build();
        StatefulSet created = kubernetesClient.apps().statefulSets().inNamespace(namespace).create(item);
        log.info("created StatefulSet with name " + created.getMetadata().getName());
    }


    private Affinity addAntiAffinity(boolean isAntiAffinity, String name) {
        if (isAntiAffinity)
            return new Affinity(null, null, buildAntiAffinity(name));
        return null;
    }

    private PodAntiAffinity buildAntiAffinity(String name) {
        List<LabelSelectorRequirement> matchExpressions = new ArrayList<>();
        List<String> values = new ArrayList<>();
        values.add(name);
        LabelSelectorRequirement labelSelectorRequirement = new LabelSelectorRequirement("selectorId", "In", values);
        matchExpressions.add(labelSelectorRequirement);
        LabelSelector labelSelector = new LabelSelector(matchExpressions, null);
        PodAffinityTerm podAffinityTerm = new PodAffinityTerm(labelSelector, null, "kubernetes.io/hostname");
        List<PodAffinityTerm> requiredDuringSchedulingIgnoredDuringExecution = new ArrayList<>();
        requiredDuringSchedulingIgnoredDuringExecution.add(podAffinityTerm);
        return new PodAntiAffinity(null, requiredDuringSchedulingIgnoredDuringExecution);
    }


    private Container getContainer(Pu pu, int partitionId) {
        PuSpec spec = pu.getSpec();
        Container container = new Container();

        container.setName("pu-container");
        container.setResources(getResourceRequirements(spec, partitionId));
        container.setImage(spec.getImage().toString());
        if (spec.getImage().getPullPolicy() != null)
            container.setImagePullPolicy(spec.getImage().getPullPolicy());
        container.setEnv(getContainerEnvVars(pu, partitionId));
        container.setCommand(ListBuilder.singletonList("tools/kubernetes/entrypoint.sh"));
        container.setArgs(getContainerArgs(pu, partitionId));
        if (pu.isStateful()) {
            container.setLivenessProbe(createSpaceLivenessProbe());
            container.setReadinessProbe(createSpaceReadinessProbe());
        }

        return container;
    }

    private List<EnvVar> getContainerEnvVars(Pu pu, int partitionId) {
        return ListBuilder.singletonList(new EnvVar("GS_OPTIONS_EXT", null, null));
    }

    private List<String> getContainerArgs(Pu pu, int partitionId) {
        PuSpec spec = pu.getSpec();
        List<String> args = new ArrayList<>();
        args.add("component=pu");
        args.add("verbose=true");
        args.add("name=" + pu.getMetadata().getName());
        args.add("release.namespace=" + pu.getMetadata().getNamespace());
        args.add("license=" + spec.getLicense());
        args.add("java.heap=limit-150Mi");
        args.add("manager.name=" + spec.getManager().getName());
        args.add("manager.ports.api=" + spec.getManager().getPorts().getApi());
        if (spec.getResourceUrl() != null) {
            args.add("pu.resourceUrl=" + spec.getResourceUrl());
        }
        if (pu.isStateful()) {
            args.add("partitions=" + spec.getPartitions()); // TODO: redundant when zk integration is done
            args.add("ha=" + spec.isHa()); // TODO: redundant when zk integration is done
            args.add("partitionId=" + partitionId);
        } else {
            args.add("instances=" + spec.getInstances());
        }
        if (spec.getProperties() != null) {
            args.add("pu.properties=" + spec.getProperties());
        }
        return args;
    }

    private ResourceRequirements getResourceRequirements(PuSpec spec, int partitionId) {
        Map<String, Quantity> limits = null;
        Map<String, Quantity> requests = null;
        List<ResourcesSpec> resourcesList = spec.getResources();
        if (resourcesList == null) {
            log.info("resourcesList is null");
        } else {
            StringJoiner sj = new StringJoiner(", ", "IDs: [", "]");
            resourcesList.forEach(rs -> sj.add(String.valueOf(rs.getId())));
            log.info("resourcesList.size is {}, {}", resourcesList.size(), sj.toString());
        }
        ResourcesSpec defaultResources = spec.getResources(null);
        ResourcesSpec partitionResources = spec.getResources(partitionId);
        ResourcesSpec resources = ResourcesSpec.merge(partitionResources, defaultResources);

        if (resources != null) {
            if (resources.getLimits() != null)
                limits = resources.getLimits().toMap();
            if (resources.getRequests() != null)
                requests = resources.getRequests().toMap();
        }

        if (limits == null) {
            limits = new MapBuilder<String, Quantity>()
                    .put("memory", Quantity.parse("400Mi"))
                    .put("cpu", Quantity.parse("1000m"))
                    .build();
        }
        log.info("limits: " + limits);


        return new ResourceRequirements(limits, requests);
    }

    private boolean isHorizontalScale(Pu pu) {
        // TODO: fetch topology from zk and compare number of partitions
        return false;
    }

    private ServicePort buildServicePort() {
        ServicePort port = new ServicePort();
        port.setName("lrmi");
        port.setProtocol("TCP");
        port.setPort(8200);
        //port.setNodePort();
        return port;
    }

    private Probe createSpaceLivenessProbe() {
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

    private Probe createSpaceReadinessProbe() {
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
