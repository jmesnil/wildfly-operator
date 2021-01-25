/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.wildfly.operator.resources;

import static org.wildfly.operator.WildFlyServerController.labelsFor;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wildfly.operator.WildFlyServer;

public class ServiceMonitors {

    private static final Logger log = LoggerFactory.getLogger(ServiceMonitors.class);

    public static void createOrUpdateServiceMonitor(KubernetesClient client, WildFlyServer wildflyServer) throws IOException {
        log.info("Execution ServiceMonitors.createOrUpdateServiceMonitor for: {}", wildflyServer.getMetadata().getName());

        if (isServiceMonitorInstalled(client)) {
            Map<String, String> labels = labelsFor(wildflyServer.getMetadata().getName());
            OwnerReference ow = Resources.ownedBy(wildflyServer);
            log.info(ow.toString());
            // create or update a ServiceMonitor that exposed WildFly Metrics on the admin /metrics
            String rawServiceMonitor = "apiVersion: monitoring.coreos.com/v1\n" +
                    "kind: ServiceMonitor\n" +
                    "metadata:\n" +
                    "  name: "+ wildflyServer.getMetadata().getName() + "\n" +
                    "  labels:\n" +
                    indentedLabels(labels, 4) +
                    "spec:\n" +
                    "  endpoints:\n" +
                    "    - port: admin\n" +
                    "  namespaceSelector: {}\n" +
                    "  selector:\n" +
                    "    matchLabels:\n" +
                    indentedLabels(labels, 6);
            client.customResource(serviceMonitor()).createOrReplace(wildflyServer.getMetadata().getNamespace(), rawServiceMonitor);
        }
    }

    private static  String indentedLabels(Map<String, String> labels, int tab) {
        String separator = "";
        for (int i = 0; i < tab; i++) {
            separator += " ";
        }
        return labels.keySet().stream()
                .map(k -> k + ": " + labels.get(k))
                .collect(Collectors.joining("\n" +separator, separator, "\n"));
    }

    public static void main(String[] args) {
        Map<String, String> labels = new HashMap<>();
        labels.put("foo", "a");
        labels.put("bar", "b");
        System.out.println(indentedLabels(labels, 2));
        System.out.println(indentedLabels(labels, 4));
    }

    public static boolean isServiceMonitorInstalled(KubernetesClient client) {
        return client.apiextensions().v1().customResourceDefinitions().list().getItems().stream().anyMatch(crd ->
                crd.getSpec().getNames().getKind().equals("ServiceMonitor") &&
                        crd.getSpec().getGroup().equals("monitoring.coreos.com") &&
                        crd.getSpec().getVersions().stream().anyMatch(v -> v.getName().equals("v1")));
    }

    public static CustomResourceDefinitionContext serviceMonitor() {
        CustomResourceDefinitionContext serviceMonitorDefinitionContext = new CustomResourceDefinitionContext.Builder()
                .withName("ServiceMonitor")
                .withGroup("monitoring.coreos.com")
                .withVersion("v1")
                .withPlural("servicemonitors")
                .withScope("Namespaced")
                .build();
        return serviceMonitorDefinitionContext;
    }
}
