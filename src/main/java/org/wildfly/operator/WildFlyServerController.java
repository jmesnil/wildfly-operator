package org.wildfly.operator;

import static java.util.Objects.requireNonNullElse;

import java.util.HashMap;
import java.util.Map;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.api.Controller;
import io.javaoperatorsdk.operator.api.DeleteControl;
import io.javaoperatorsdk.operator.api.ResourceController;
import io.javaoperatorsdk.operator.api.UpdateControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wildfly.operator.resources.Services;
import org.wildfly.operator.resources.StatefulSets;

/** A very simple sample controller that creates a service with a label. */
@Controller
public class WildFlyServerController implements ResourceController<WildFlyServer> {

    private static final Logger log = LoggerFactory.getLogger(WildFlyServerController.class);

    public static final String KIND = "Wil";

    private final KubernetesClient kubernetesClient;

    public WildFlyServerController(KubernetesClient kubernetesClient) {
        this.kubernetesClient = kubernetesClient;
    }

    @Override
    public DeleteControl deleteResource(WildFlyServer resource, Context<WildFlyServer> context) {
        log.info("Execution deleteResource for: {}", resource.getMetadata().getName());
        return DeleteControl.DEFAULT_DELETE;
    }

    @Override
    public UpdateControl<WildFlyServer> createOrUpdateResource(
            WildFlyServer wildflyServer, Context<WildFlyServer> context) {
        log.info("createOrUpdateResource for {}", wildflyServer.getMetadata().getName());

        StatefulSets.createOrUpdate(kubernetesClient, wildflyServer);
        Services.createOrUpdateLoadBalancer(kubernetesClient, wildflyServer);

        WildFlyServerStatus status = requireNonNullElse(wildflyServer.getStatus(), new WildFlyServerStatus());
        status.setReplicas(wildflyServer.getSpec().getReplicas());
        wildflyServer.setStatus(status);

        return UpdateControl.updateCustomResource(wildflyServer);
    }

    public static Map<String, String> labelsFor(String wildflyServerName) {
        Map<String, String> labels = new HashMap<>();
        labels.put("app.kubernetes.io/name", wildflyServerName);
        labels.put("app.kubernetes.io/managed-by","wildfly-operator");
        labels.put("app.kubernetes.io/runtime", "wildfly");
	return labels;
    }
}