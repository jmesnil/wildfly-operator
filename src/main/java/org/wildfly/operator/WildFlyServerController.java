/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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
package org.wildfly.operator;

import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.api.Controller;
import io.javaoperatorsdk.operator.api.ResourceController;
import io.javaoperatorsdk.operator.api.UpdateControl;
import io.javaoperatorsdk.operator.processing.event.EventSourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wildfly.operator.events.WildFlyEventSources;
import org.wildfly.operator.resources.Services;
import org.wildfly.operator.resources.StatefulSets;

@Controller
public class WildFlyServerController implements ResourceController<WildFlyServer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(WildFlyServerController.class);

    @Inject
    KubernetesClient client;

    @Inject
    OperatorConfig operatorConfig;

    @Inject
    StatefulSets statefulSets;

    @Inject
    Services services;

    @Override
    public void init(EventSourceManager eventSourceManager) {
        eventSourceManager.registerEventSource("wildfly-event-source", new WildFlyEventSources(client, operatorConfig));
    }

    @Override
    public UpdateControl<WildFlyServer> createOrUpdateResource(
        WildFlyServer wildflyServer, Context<WildFlyServer> context) {

        // new resource
        if (wildflyServer.getStatus() == null) {
            LOGGER.info("Create resources for WildFlyServer resource {} in namespace {}",
                    wildflyServer.getMetadata().getName(),
                    wildflyServer.getMetadata().getNamespace());
            wildflyServer.setStatus(new WildFlyServerStatus());
        }

        statefulSets.createOrUpdate(wildflyServer);
        services.createOrUpdateAdmin(wildflyServer);
        services.createOrUpdateLoadBalancer(wildflyServer);

        boolean statusUpdated = false;
        // Update the wildflyServer status based on the statefulset status.replicas
        StatefulSet statefulSet = statefulSets.get(wildflyServer);
        if (statefulSet != null && statefulSet.getStatus() !=  null) {
            wildflyServer.getStatus().setReplicas(statefulSet.getStatus().getReplicas());
            statusUpdated = true;
        }

        if (statusUpdated) {
            return UpdateControl.updateCustomResourceAndStatus(wildflyServer);
        } else {
            return UpdateControl.updateCustomResource(wildflyServer);
        }
    }
}

