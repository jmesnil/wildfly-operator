package org.wildfly.operator;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import io.fabric8.openshift.client.OpenShiftClient;
import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.api.Controller;
import io.javaoperatorsdk.operator.api.DeleteControl;
import io.javaoperatorsdk.operator.api.ResourceController;
import io.javaoperatorsdk.operator.api.UpdateControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wildfly.operator.resources.ServiceMonitors;
import org.wildfly.operator.resources.Services;
import org.wildfly.operator.resources.StatefulSets;

/** A very simple sample controller that creates a service with a label. */
@Controller
public class WildFlyServerController implements ResourceController<WildFlyServer> {

    private static final Logger log = LoggerFactory.getLogger(WildFlyServerController.class);

    public static final String KIND = "Wil";

    private final OpenShiftClient client;

    public WildFlyServerController(OpenShiftClient client) {
        this.client = client;
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

        if (wildflyServer.getStatus() == null) {
            wildflyServer.setStatus(new WildFlyServerStatus());
        }

        StatefulSets.createOrUpdate(client, wildflyServer);
        Services.createOrUpdateLoadBalancer(client, wildflyServer);
        Services.createOrUpdateAdmin(client, wildflyServer);
        try {
            ServiceMonitors.createOrUpdateServiceMonitor(client, wildflyServer);
        } catch (IOException e) {
            e.printStackTrace();
        }

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