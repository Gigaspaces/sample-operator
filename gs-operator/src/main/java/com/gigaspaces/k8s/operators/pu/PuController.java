package com.gigaspaces.k8s.operators.pu;

import com.gigaspaces.k8s.operators.ListBuilder;
import com.gigaspaces.k8s.operators.MapBuilder;
import com.gigaspaces.k8s.operators.common.MemoryXtendSpec;
import com.gigaspaces.k8s.operators.common.ProbeSpec;
import com.gigaspaces.k8s.operators.common.ResourcesSpec;
import com.gigaspaces.k8s.operators.common.ServiceSpec;
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
import io.fabric8.kubernetes.client.dsl.ServiceResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

@Controller(crdName = "pus.gigaspaces.com", customResourceClass = Pu.class)
public class PuController implements ResourceController<Pu> {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final KubernetesClient kubernetesClient;

    private enum Scale {
        NONE, OUT, IN, UP, DOWN
    }

    public PuController(KubernetesClient kubernetesClient) {
        this.kubernetesClient = kubernetesClient;
    }

    @Override
    public boolean deleteResource(Pu pu, Context<Pu> context) {
        log.info("\n===> deleteResource \n" + pu);
        deleteService(pu);

        PuSpec spec = pu.getSpec().applyDefaults();
        int partitions = spec.getPartitions();
        if (partitions == 0) {
            deletePuResources(pu, 0);
        } else {
            for (int i = 1; i <= partitions; i++) {
                deletePuResources(pu, i);

            }
        }

        return true;
    }

    private void deletePuResources(Pu pu, int statefulSetId) {
        deleteStatefulSet(pu, statefulSetId);

        if (pu.getSpec().getService() != null && pu.getSpec().getService().getLrmi().getEnabled()) {
            deleteLrmiService(pu, statefulSetId, "-0");
            if (pu.getSpec().isHa()) {
                deleteLrmiService(pu, statefulSetId, "-1");
            }
        }
    }

    private void deleteStatefulSet(Pu pu, int statefulSetId) {
        String name = pu.getStatefulSetName(statefulSetId);
        StatefulSet exists = statefulSet(pu, name).get();
        if (exists == null) {
            log.info("stateful set '" + name + "' does not exist");
        } else {
            Boolean delete = statefulSet(pu, name).delete();
            log.info("Deleted (" + delete + ") StatefulSet with name " + exists.getMetadata().getName());
        }

    }

    private void deleteLrmiService(Pu pu, int statefulSetId, String partitionId) {
        String name = pu.getLrmiService(partitionId, statefulSetId);
        Service exists = service(pu, name).get();
        if (exists == null) {
            log.info("service '" + name + "' does not exist");
        } else {
            Boolean delete = service(pu, name).delete();
            log.info("Deleted (" + delete + ") service with name " + exists.getMetadata().getName());
        }
    }

    private void deleteService(Pu pu) {
        String name = pu.getServiceName();
        Service exists = service(pu, name).get();
        if (exists == null) {
            log.info("service '" + name + "' does not exist");
        } else {
            Boolean delete = service(pu, name).delete();
            log.info("Deleted (" + delete + ") service with name " + exists.getMetadata().getName());
        }
    }

    @Override
    public UpdateControl<Pu> createOrUpdateResource(Pu pu, Context<Pu> context) {
        log.info("\n===> createOrUpdateResource \n" + pu);
        pu.getSpec().applyDefaults();

        /////////////// HACK for using local dev ///////////////
//        ImageSpec image = new ImageSpec();
//        image.setTag("dev");
//        image.setRepository("gigaspaces/xap-enterprise");
//        pu.getSpec().setImage(image);
        /////////////// HACK ///////////////

        log.info("DEBUG - Creating/updating pu {} to generation {}", pu.getMetadata().getName(), pu.getMetadata().getGeneration());
        String name = pu.getServiceName();
        Service service = service(pu, name).get();
        if (service == null)
            createHeadlessService(pu);

        int modifications = 0;
        if (!pu.isStateful()) {
            if (createOrUpdateStatefulSet(pu, 1)) {
                modifications++;
            }
        } else {
            Scale horizontalScale = getTypeOfHorizontalScale(pu);
            switch (horizontalScale) {
                case NONE:
                case OUT:
                    modifications = doScaleOut(pu);
                    break;
                case IN:
                    modifications = doScaleIn(pu);
                    break;
            }
        }

        return modifications == 0 ? UpdateControl.noUpdate() : UpdateControl.updateStatusSubResource(pu);
    }

    private int doScaleOut(Pu pu) {
        PuSpec spec = pu.getSpec();
        int targetPartitions = spec.getPartitions();
        final int actualPartitions = countActualPartitions(pu);

        //perquisite
        if (actualPartitions > targetPartitions) return 0;

        int modifications = 0;
        if (targetPartitions == 0) {
            if (createOrUpdateStatefulSet(pu, 0)) {
                log.info("DEBUG - Create/Update Stateful Set with name: {}", pu.getStatefulSetName(0));
                modifications++;
            }
        } else {
            for (int i = 1; i <= targetPartitions; i++) {
                if (createOrUpdateStatefulSet(pu, i)) {
                    log.info("DEBUG - Create/Update Stateful Set with name: {}", pu.getStatefulSetName(i));
                    modifications++;
                }
            }
        }
        return modifications;
    }

    private int doScaleIn(Pu pu) {
        final int targetPartitions = pu.getSpec().getPartitions();
        final int actualPartitions = countActualPartitions(pu);

        //perquisite
        if (actualPartitions >= targetPartitions) return 0;

        int modifications = 0;
        for (int i = actualPartitions; i <= targetPartitions; i++) {
            RollableScalableResource<StatefulSet, DoneableStatefulSet> resource = statefulSet(pu, i);
            if (resource == null) break;

            log.info("DEBUG - Delete Stateful Set with name: {}", pu.getStatefulSetName(i));
            resource.delete();
            modifications++;
        }

        return modifications;
    }

    private boolean createOrUpdateStatefulSet(Pu pu, int statefulSetId) {
        StatefulSet statefulSet = statefulSet(pu, statefulSetId).get();
        if (statefulSet == null) {
            if (pu.getSpec().getService() != null && pu.getSpec().getService().getLrmi().getEnabled()) {
                if (pu.getSpec().getPartitions() > 0) {
                    createLrmiService(pu, statefulSetId + "-0", statefulSetId);
                    if (pu.getSpec().isHa()) {
                        createLrmiService(pu, statefulSetId + "-1", statefulSetId);

                    }
                } else {
                    createLrmiService(pu, "0", 0);

                }
            }
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

    private ServiceResource<Service, DoneableService> service(Pu pu, String name) {
        return kubernetesClient.services()
                .inNamespace(pu.getMetadata().getNamespace())
                .withName(name);
    }

    private void createHeadlessService(Pu pu) {

        String namespace = pu.getMetadata().getNamespace();
        String name = pu.getMetadata().getName();
        PuSpec spec = pu.getSpec();

        ServiceBuilder service = new ServiceBuilder();
        service.withApiVersion("v1")
                .withKind("Service")
                .withNewMetadata()
                .withNamespace(namespace)
                .withName(name + "-" + spec.getApp() + "-hs")
                .withLabels(new MapBuilder<String, String>()
                        .put("app", spec.getApp())
                        .put("release", pu.getMetadata().getName())
                        .build())
                .endMetadata()
                .withNewSpec()
                .withSelector(new MapBuilder<String, String>()
                        .put("selectorId", name + "-" + spec.getApp())
                        .build())
                .withNewType("ClusterIP")
                .withClusterIP("None")
                .endSpec();

        Service item = service.build();
        Service service1 = kubernetesClient.services().inNamespace(namespace).create(item);
        log.info("created Service with name " + service1.getMetadata().getName());
    }

    private void createLrmiService(Pu pu, String partitionId, int statefulSetId) {

        String namespace = pu.getMetadata().getNamespace();
        String name = pu.getMetadata().getName();
        PuSpec spec = pu.getSpec();
        String selectorId = name + "-" + spec.getApp();

        if (pu.getSpec().getPartitions() > 0)
            selectorId = selectorId + "-" + statefulSetId;

        ServiceSpec serviceSpec = pu.getSpec().getService();
        ServiceBuilder service = new ServiceBuilder();
        service.withApiVersion("v1")
                .withKind("Service")
                .withNewMetadata()
                .withNamespace(namespace)
                .withName(name + "-" + spec.getApp() + "-" + partitionId + "-service")
                .withLabels(new MapBuilder<String, String>()
                        .put("app", spec.getApp())
                        .put("release", pu.getMetadata().getName())
                        .build())
                .endMetadata()
                .withNewSpec()
                .withSelector(new MapBuilder<String, String>()
                        .put("statefulset.kubernetes.io/pod-name", name + "-" + spec.getApp() + "-" + partitionId)
                        .put("app", spec.getApp())
                        .put("partitionId", String.valueOf(statefulSetId))
                        .put("selectorId", selectorId)
                        .build())
                .withNewType(serviceSpec.getType())
                .withPorts(ListBuilder.singletonList(buildServicePort(serviceSpec)))
                .endSpec();

        Service item = service.build();
        Service service1 = kubernetesClient.services().inNamespace(namespace).create(item);
        log.info("created Service with name " + service1.getMetadata().getName());
    }

    private ServicePort buildServicePort(ServiceSpec serviceSpec) {
        ServicePort port = new ServicePort();
        port.setName("lrmi");
        port.setProtocol("TCP");
        port.setPort(serviceSpec.getLrmi().getPort());
        return port;
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
                .withServiceName(pu.getMetadata().getName() + "-" + spec.getApp() + "-hs")
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
        if (pu.getSpec().getMemoryXtendVolume() != null && pu.getSpec().getMemoryXtendVolume().getEnabled()) {
            item.getSpec().setVolumeClaimTemplates(ListBuilder.singletonList(persistentVolumeClaimBuilder(pu, partition)));
        }

        if (pu.getSpec().getNodeSelector() != null && pu.getSpec().getNodeSelector().getEnabled() && pu.getSpec().getNodeSelector().getSelector() != null) {
            Map<String, String> nodeSelectorProperties = Arrays.stream(pu.getSpec().getNodeSelector().getSelector().replace(" ", "").split(", "))
                    .map(arrayData -> arrayData.split(":"))
                    .collect(Collectors.toMap(d -> d[0].trim(), d -> d[1]));

            item.getSpec().getTemplate().getSpec().setNodeSelector(nodeSelectorProperties);
        }

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
        if (pu.getSpec().getMemoryXtendVolume() != null && pu.getSpec().getMemoryXtendVolume().getEnabled()) {
            VolumeMount volumeMount = new VolumeMount();
            volumeMount.setMountPath(pu.getSpec().getMemoryXtendVolume().getVolumeMount().getMountPath());
            volumeMount.setName(pu.getSpec().getMemoryXtendVolume().getVolumeMount().getName());
            container.setVolumeMounts(ListBuilder.singletonList(volumeMount));
        }

        if (pu.isStateful()) {
            ProbeSpec livenessProbe = spec.getLivenessProbe();
            ProbeSpec probeSpec = new ProbeSpec(30, 5, 3);
            if (livenessProbe == null) {
                container.setLivenessProbe(createSpaceLivenessProbe(probeSpec));
            } else if (livenessProbe.getEnabled())
                container.setLivenessProbe(createSpaceLivenessProbe(livenessProbe));
            ProbeSpec readinessProbe = spec.getReadinessProbe();
            if (livenessProbe == null) {
                container.setReadinessProbe(createSpaceReadinessProbe(probeSpec));
            } else if (readinessProbe.getEnabled())
                container.setReadinessProbe(createSpaceReadinessProbe(readinessProbe));
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
        args.add("java.heap=" + spec.getJavaHeap());
        args.add("manager.name=" + spec.getManager().getName());
        args.add("manager.ports.api=" + spec.getManager().getPorts().getApi());
        if (pu.getSpec().getService() != null && pu.getSpec().getService().getLrmi().getEnabled()) {
            args.add("lrmi.port=" + pu.getSpec().getService().getLrmi().getPort());
            args.add("external.lrmi.enabled=true");
        }
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

    private Scale getTypeOfHorizontalScale(Pu pu) {
        final int actualPartitions = countActualPartitions(pu);
        if (actualPartitions == 0) return Scale.NONE;

        final int desiredPartitions = pu.getSpec().getPartitions();
        if (desiredPartitions > actualPartitions) return Scale.OUT;
        else if (desiredPartitions < actualPartitions) return Scale.IN;
        else return Scale.NONE;
    }

    private int countActualPartitions(Pu pu) {
        int actualPartitions = 0;
        int i = 0;
        for (; ; ) {
            i++;
            if (null == statefulSet(pu, i).get()) break;
            actualPartitions++;
        }
        return actualPartitions;
    }


    private Probe createSpaceLivenessProbe(ProbeSpec livenessProbe) {
        Probe probe = new Probe();
        HTTPGetAction httpGetAction = new HTTPGetAction();
        httpGetAction.setPath("/probes/alive");
        httpGetAction.setPort(new IntOrString(8089));
        probe.setHttpGet(httpGetAction);
        probe.setInitialDelaySeconds(livenessProbe.getInitialDelaySeconds());
        probe.setPeriodSeconds(livenessProbe.getPeriodSeconds());
        probe.setFailureThreshold(livenessProbe.getFailureThreshold());
        return probe;
    }

    private Probe createSpaceReadinessProbe(ProbeSpec readinessProbe) {
        Probe probe = new Probe();
        HTTPGetAction httpGetAction = new HTTPGetAction();
        httpGetAction.setPath("/probes/ready");
        httpGetAction.setPort(new IntOrString(8089));
        probe.setHttpGet(httpGetAction);
        probe.setInitialDelaySeconds(readinessProbe.getInitialDelaySeconds());
        probe.setPeriodSeconds(readinessProbe.getPeriodSeconds());
        probe.setFailureThreshold(readinessProbe.getFailureThreshold());
        return probe;
    }


    private PersistentVolumeClaim persistentVolumeClaimBuilder(Pu pu, int statefulSetId) {
        MemoryXtendSpec memoryXtend = pu.getSpec().getMemoryXtendVolume();
        String namespace = pu.getMetadata().getNamespace();

        PersistentVolumeClaimBuilder persistentVolumeClaimBuilder = new PersistentVolumeClaimBuilder();
        persistentVolumeClaimBuilder
                .withApiVersion("v1")
                .withKind("PersistentVolumeClaim")
                .withNewMetadata()
                .withNamespace(namespace)
                .withName(memoryXtend.getVolumeMount().getName())
                .withLabels(new MapBuilder<String, String>()
                        .put("selectorId", pu.getMetadata().getName() + "-" + pu.getSpec().getApp() + "-" + statefulSetId)
                        .build())
                .endMetadata()
                .withNewSpec()
                .withAccessModes(memoryXtend.getVolumeClaimTemplate().getAccessModes())
                .withNewResources()
                .addToRequests("storage", new Quantity(memoryXtend.getVolumeClaimTemplate().getStorage()))
                .endResources()
                .withStorageClassName("standard")
                .withVolumeMode("Filesystem")
                .endSpec()
        ;
        PersistentVolumeClaim build = persistentVolumeClaimBuilder.build();
        return build;
    }

}
