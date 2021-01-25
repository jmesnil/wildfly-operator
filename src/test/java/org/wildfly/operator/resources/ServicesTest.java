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

import static org.assertj.core.api.Assertions.assertThat;
import static org.wildfly.operator.resources.IsOwnedByMatcher.ownedBy;

import java.util.UUID;

import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.wildfly.operator.WildFlyServer;
import org.wildfly.operator.WildFlyServerSpec;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnableKubernetesMockClient(crud = true)
public class ServicesTest {

    KubernetesClient client;

    @Test
    public void testCreationOfLoadbalancerService() {
        WildFlyServer wfly = new WildFlyServer();
        wfly.getMetadata().setName(UUID.randomUUID().toString());
        wfly.getMetadata().setNamespace(UUID.randomUUID().toString());
        wfly.getMetadata().setUid(UUID.randomUUID().toString());
        wfly.setSpec(new WildFlyServerSpec());
        wfly.getSpec().setApplicationImage("whatever");
        wfly.getSpec().setReplicas(1);

        Services.createOrUpdateLoadBalancer(client, wfly);

        var services = client.services().inNamespace(wfly.getMetadata().getNamespace()).list();
        var loadBalancer = services.getItems().stream()
                .filter(s -> s.getMetadata().getName().equals(wfly.getMetadata().getName() + "-loadbalancer"))
                .findFirst()
                .get();
        assertThat(loadBalancer).isNotNull();
        assertThat(loadBalancer).is(ownedBy(wfly));

        assertThat(loadBalancer.getSpec().getType()).isEqualTo("LoadBalancer");
        assertThat(loadBalancer.getSpec().getPorts().size()).isEqualTo(1);

        ServicePort servicePort = loadBalancer.getSpec().getPorts().get(0);
        assertThat(servicePort.getName()).isEqualTo("http");
        assertThat(servicePort.getPort()).isEqualTo(8080);
    }
}
