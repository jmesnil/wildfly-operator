package org.wildfly.operator;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class OperatorConfig {

    @ConfigProperty(name = "label.app.managed.by", defaultValue = "wildfly-operator")
    public String managedByLabel;

    @ConfigProperty(name = "label.app.runtime", defaultValue = "wildfly")
    public String runtimeLabel;

    public Map<String, String> labelsFor(String wildflyServerName) {
        Map<String, String> labels = new HashMap<>();
        labels.put("app.kubernetes.io/name", wildflyServerName);
        labels.put("app.kubernetes.io/managed-by", managedByLabel);
        labels.put("app.kubernetes.io/runtime", runtimeLabel);
        return labels;
    }
}