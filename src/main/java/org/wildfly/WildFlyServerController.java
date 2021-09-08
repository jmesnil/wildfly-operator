package org.wildfly;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.*;
import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.processing.event.EventSourceManager;

@Controller
public class WildFlyServerController implements ResourceController<WildFlyServer> {

    private final KubernetesClient client;

    public WildFlyServerController(KubernetesClient client) {
        this.client = client;
    }

    // TODO Fill in the rest of the controller

    @Override
    public void init(EventSourceManager eventSourceManager) {
        // TODO: fill in init
    }

    @Override
    public UpdateControl<WildFlyServer> createOrUpdateResource(
        WildFlyServer resource, Context<WildFlyServer> context) {
        // TODO: fill in logic

        return UpdateControl.noUpdate();
    }
}

