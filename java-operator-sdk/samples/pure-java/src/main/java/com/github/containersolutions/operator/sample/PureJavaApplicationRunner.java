package com.github.containersolutions.operator.sample;

import com.github.containersolutions.operator.Operator;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.batch.JobList;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.kubernetes.client.informers.SharedInformerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PureJavaApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PureJavaApplicationRunner.class);

    public static void main(String[] args) throws InterruptedException {
        KubernetesClient client = new DefaultKubernetesClient();
        Operator operator = new Operator(client);
//        operator.registerController(new CustomServiceController(client));
        operator.registerController(new PuController(client));

//        JobList list = client.batch().jobs().list();
//        client.pods().list().getItems().forEach(p->log.info("pod="+podDetails(p)));
//        list.getItems().forEach(j-> log.info("job="+j.getMetadata().getName()));
        //listenForPodsUsingFabric8(client);

        //Thread.sleep(Long.MAX_VALUE);

    }

    private static void listenForPodsUsingFabric8(KubernetesClient client) {
        SharedInformerFactory informerFactory = client.informers();
        SharedIndexInformer<Pod> podSharedIndexInformer = informerFactory.sharedIndexInformerFor(Pod.class, PodList.class, 10 * 60 * 1000);
        podSharedIndexInformer.addEventHandler(new ResourceEventHandler<Pod>() {
            @Override
            public void onAdd(Pod pod) {
                log.info("onAdd: pod="+podDetails(pod));
            }

            @Override
            public void onUpdate(Pod oldPod, Pod newPod) {
                log.info("onUpdate: oldPod="+podDetails(oldPod) + " newPod="+podDetails(newPod));
            }

            @Override
            public void onDelete(Pod pod, boolean b) {
                log.info("onDelete: pod="+podDetails(pod));
            }
        });
        informerFactory.startAllRegisteredInformers();
    }

    public static String podDetails(Pod pod) {
        return pod.getMetadata().getName();
    }
}
