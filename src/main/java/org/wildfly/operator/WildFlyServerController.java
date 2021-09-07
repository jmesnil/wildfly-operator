package org.wildfly.operator;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.api.Controller;
import io.javaoperatorsdk.operator.api.DeleteControl;
import io.javaoperatorsdk.operator.api.ResourceController;
import io.javaoperatorsdk.operator.api.UpdateControl;
import io.javaoperatorsdk.operator.processing.event.EventSourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wildfly.operator.events.StatefulSetEventSource;
import org.wildfly.operator.resources.ServiceMonitors;
import org.wildfly.operator.resources.Services;
import org.wildfly.operator.resources.StatefulSets;

/** A very simple sample controller that creates a service with a label. */
@Controller(namespaces = Controller.WATCH_CURRENT_NAMESPACE)
public class WildFlyServerController implements ResourceController<WildFlyServer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(WildFlyServerController.class);

    @Inject
    KubernetesClient kubernetesClient;

    @Override
    public void init(EventSourceManager eventSourceManager) {
        LOGGER.info("WildFlyServerController.init");
        eventSourceManager.registerEventSource("statefulset", new StatefulSetEventSource(kubernetesClient));
    }

    @Override
    public DeleteControl deleteResource(WildFlyServer resource, Context<WildFlyServer> context) {
        LOGGER.info("Execution deleteResource for: {}", resource.getMetadata().getName());
        return DeleteControl.DEFAULT_DELETE;
    }

    @Override
    public UpdateControl<WildFlyServer> createOrUpdateResource(
            WildFlyServer wildflyServer, Context<WildFlyServer> context) {
        LOGGER.info("createOrUpdateResource for {}", wildflyServer.getMetadata().getName());

        if (wildflyServer.getStatus() == null) {
            wildflyServer.setStatus(new WildFlyServerStatus());
        }

        StatefulSets.createOrUpdate(kubernetesClient, wildflyServer);
        Services.createOrUpdateLoadBalancer(kubernetesClient, wildflyServer);
        Services.createOrUpdateAdmin(kubernetesClient, wildflyServer);
        try {
            ServiceMonitors.createOrUpdateServiceMonitor(kubernetesClient, wildflyServer);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Update the wildflyServer status based on the statefulset status.replicas
        StatefulSet statefulSet = kubernetesClient.apps().statefulSets()
                .inNamespace(wildflyServer.getMetadata().getNamespace())
                .withName(wildflyServer.getMetadata().getName())
                .get();
        if (statefulSet != null && statefulSet.getStatus() !=  null) {
            wildflyServer.getStatus().setReplicas(statefulSet.getStatus().getReplicas());
        }

        return UpdateControl.updateCustomResourceAndStatus(wildflyServer);
    }

    public static Map<String, String> labelsFor(String wildflyServerName) {
        Map<String, String> labels = new HashMap<>();
        labels.put("app.kubernetes.io/name", wildflyServerName);
        labels.put("app.kubernetes.io/managed-by","wildfly-operator");
        labels.put("app.kubernetes.io/runtime", "wildfly");
        return labels;
    }
}