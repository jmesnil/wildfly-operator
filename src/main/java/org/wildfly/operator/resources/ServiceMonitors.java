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
import static org.wildfly.operator.resources.Resources.ownedBy;

import java.io.IOException;
import java.util.Map;

import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.openshift.api.model.monitoring.v1.EndpointBuilder;
import io.fabric8.openshift.api.model.monitoring.v1.ServiceMonitorBuilder;
import io.fabric8.openshift.api.model.monitoring.v1.ServiceMonitorSpecBuilder;
import io.fabric8.openshift.client.OpenShiftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wildfly.operator.WildFlyServer;

public class ServiceMonitors {

    private static final Logger log = LoggerFactory.getLogger(ServiceMonitors.class);

    public static void createOrUpdateServiceMonitor(OpenShiftClient client, WildFlyServer wildflyServer) throws IOException {
        log.info("Execution ServiceMonitors.createOrUpdateServiceMonitor for: {}", wildflyServer.getMetadata().getName());

        if (isServiceMonitorInstalled(client)) {
            Map<String, String> labels = labelsFor(wildflyServer.getMetadata().getName());
            OwnerReference ow = Resources.ownedBy(wildflyServer);
            log.info(ow.toString());
            client.monitoring().serviceMonitors()
                    .inNamespace(wildflyServer.getMetadata().getNamespace())
                    .createOrReplace(
                            new ServiceMonitorBuilder()
                                    .withNewMetadata()
                                    .withName(wildflyServer.getMetadata().getName())
                                    .withOwnerReferences(ownedBy(wildflyServer))
                                    .addToLabels(labels)
                                    .endMetadata()
                                    .withSpec(new ServiceMonitorSpecBuilder()
                                            .withNewSelector()
                                            .withMatchLabels(labels)
                                            .endSelector()
                                            .withEndpoints(
                                                    new EndpointBuilder()
                                                            .withPort("admin")
                                                            .build())
                                            .build()
                                    )
                                    .build());
        }
    }

    public static boolean isServiceMonitorInstalled(OpenShiftClient client) {
        return client.apiextensions().v1().customResourceDefinitions().list().getItems().stream().anyMatch(crd ->
                crd.getSpec().getNames().getKind().equals("ServiceMonitor") &&
                        crd.getSpec().getGroup().equals("monitoring.coreos.com") &&
                        crd.getSpec().getVersions().stream().anyMatch(v -> v.getName().equals("v1")));
    }
}
