package org.wildfly;

import javax.inject.Inject;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.*;
import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.processing.event.EventSourceManager;

@Controller
public class WildFlyServerController implements ResourceController<WildFlyServer> {

    @Inject
    KubernetesClient client;

    @Override
    public void init(EventSourceManager eventSourceManager) {
        System.out.println("WildFlyServerController.init");
    }

    @Override
    public UpdateControl<WildFlyServer> createOrUpdateResource(
        WildFlyServer resource, Context<WildFlyServer> context) {

        return UpdateControl.noUpdate();
    }
}

