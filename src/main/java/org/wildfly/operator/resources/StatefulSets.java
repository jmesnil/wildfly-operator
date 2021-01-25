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

import java.util.Map;

import io.fabric8.kubernetes.api.model.ContainerPortBuilder;
import io.fabric8.kubernetes.api.model.PodSpecBuilder;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSetSpecBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.wildfly.operator.WildFlyServer;

public class StatefulSets {

    public static void createOrUpdate(KubernetesClient client, WildFlyServer wildflyServer) {
        Map<String, String> labels = labelsFor(wildflyServer.getMetadata().getName());

        StatefulSet statefulSet = client.apps().statefulSets()
                .inNamespace(wildflyServer.getMetadata().getNamespace())
                .createOrReplace(
                        new StatefulSetBuilder()
                                .withNewMetadata()
                                .withName(wildflyServer.getMetadata().getName())
                                .withOwnerReferences(ownedBy(wildflyServer))
                                .addToLabels(labels)
                                .endMetadata()
                                .withSpec(new StatefulSetSpecBuilder()
                                        .withReplicas(wildflyServer.getSpec().getReplicas())
                                        .withNewSelector()
                                        .withMatchLabels(labels)
                                        .endSelector()
                                        .withTemplate(new PodTemplateSpecBuilder()
                                                .withNewMetadata()
                                                .withLabels(labels)
                                                .endMetadata()
                                                .withSpec(new PodSpecBuilder()
                                                        .addNewContainer()
                                                        .withName(wildflyServer.getMetadata().getName())
                                                        .withImage(wildflyServer.getSpec().getApplicationImage())
                                                        .withPorts(
                                                                new ContainerPortBuilder()
                                                                        .withName("http")
                                                                        .withContainerPort(8080)
                                                                        .build(),
                                                                new ContainerPortBuilder()
                                                                        .withName("admin")
                                                                        .withContainerPort(9990)
                                                                        .build())

                                                        .endContainer()
                                                        .build())
                                                .build())
                                        .build())
                                .build());

        wildflyServer.getStatus().setReplicas(statefulSet.getStatus().getReplicas());
    }
}
