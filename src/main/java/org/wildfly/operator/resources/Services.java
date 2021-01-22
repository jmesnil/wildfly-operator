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

import java.util.Collections;

import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.ServiceSpec;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wildfly.operator.WildFlyServer;
import org.wildfly.operator.WildFlyServerController;

public class Services {

    private static final Logger log = LoggerFactory.getLogger(Services.class);

    public static void createOrUpdate(KubernetesClient client, WildFlyServer wildflyServer) {
        log.info("Execution Services.createOrUpdate for: {}", wildflyServer.getMetadata().getName());

        ServicePort servicePort = new ServicePort();
        servicePort.setPort(8080);
        ServiceSpec serviceSpec = new ServiceSpec();
        serviceSpec.setPorts(Collections.singletonList(servicePort));

        System.out.println(10);
        client
                .services()
                .inNamespace(wildflyServer.getMetadata().getNamespace())
                .createOrReplace(
                        new ServiceBuilder()
                                .withNewMetadata()
                                .withName(wildflyServer.getSpec().getApplicationImage())
                                .addToLabels("testLabel", "" + wildflyServer.getSpec().getReplicas())
                                .endMetadata()
                                .withSpec(serviceSpec)
                                .build());
    }
}
