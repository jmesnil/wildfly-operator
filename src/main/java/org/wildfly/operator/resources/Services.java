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

import static org.wildfly.operator.resources.Resources.ownedBy;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.fabric8.kubernetes.api.model.ServiceSpec;
import io.fabric8.kubernetes.api.model.ServiceSpecBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wildfly.operator.OperatorConfig;
import org.wildfly.operator.WildFlyServer;

@ApplicationScoped
public class Services {

    private static final Logger log = LoggerFactory.getLogger(Services.class);

    @Inject
    KubernetesClient client;

    @Inject
    OperatorConfig config;

    public void createOrUpdateLoadBalancer(WildFlyServer wildflyServer) {
        log.info("Execution Services.createOrUpdateLoadBalancer for: {}", wildflyServer.getMetadata().getName());

        createOrUpdate(wildflyServer,
                wildflyServer.getMetadata().getName() + "-loadbalancer",
                new ServiceSpecBuilder()
                        .withType("LoadBalancer")
                        .withSelector(config.labelsFor(wildflyServer.getMetadata().getName()))
                        .withPorts(
                                new ServicePortBuilder()
                                        .withName("http")
                                        .withPort(8080)
                                        .build())
                        .build());
    }

    public void createOrUpdateAdmin(WildFlyServer wildflyServer) {
        log.info("Execution Services.createOrUpdateAdmin for: {}", wildflyServer.getMetadata().getName());

        createOrUpdate(wildflyServer,
                wildflyServer.getMetadata().getName() + "-admin",
                new ServiceSpecBuilder()
                        .withType("ClusterIP")
                        .withSelector(config.labelsFor(wildflyServer.getMetadata().getName()))
                        .withPorts(
                                new ServicePortBuilder()
                                        .withName("admin")
                                        .withPort(9990)
                                        .build())
                        .build());
    }

    private void createOrUpdate(WildFlyServer wildflyServer, String serviceName, ServiceSpec spec) {
        client.services()
                .inNamespace(wildflyServer.getMetadata().getNamespace())
                .createOrReplace(
                        new ServiceBuilder()
                                .withNewMetadata()
                                .withName(serviceName)
                                .withNamespace(wildflyServer.getMetadata().getNamespace())
                                .withOwnerReferences(ownedBy(wildflyServer))
                                .addToLabels(config.labelsFor(wildflyServer.getMetadata().getName()))
                                .endMetadata()
                                .withSpec(spec)
                                .build());
    }
}
