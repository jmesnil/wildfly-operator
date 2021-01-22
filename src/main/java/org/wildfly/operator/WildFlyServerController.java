package org.wildfly.operator;

import static java.util.Objects.requireNonNullElse;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.api.Controller;
import io.javaoperatorsdk.operator.api.DeleteControl;
import io.javaoperatorsdk.operator.api.ResourceController;
import io.javaoperatorsdk.operator.api.UpdateControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wildfly.operator.resources.Services;

/** A very simple sample controller that creates a service with a label. */
@Controller
public class WildFlyServerController implements ResourceController<WildFlyServer> {

    private static final Logger log = LoggerFactory.getLogger(WildFlyServerController.class);

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
        log.info("Execution createOrUpdateResource for: {}", wildflyServer.getMetadata().getName());

        Services.createOrUpdate(kubernetesClient, wildflyServer);

        WildFlyServerStatus status = requireNonNullElse(wildflyServer.getStatus(), new WildFlyServerStatus());
        status.setReplicas(wildflyServer.getSpec().getReplicas());
        wildflyServer.setStatus(status);

        return UpdateControl.updateCustomResource(wildflyServer);
    }
}