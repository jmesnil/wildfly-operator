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
package org.wildfly.operator.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.wildfly.operator.WildFlyServer;
import org.wildfly.operator.WildFlyServerController;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SimpleWildFlyServerIT {

    private IntegrationTestSupport integrationTestSupport = new IntegrationTestSupport();

    @BeforeEach
    public void initAndCleanup() {
        KubernetesClient k8sClient = new DefaultKubernetesClient();
        integrationTestSupport.initialize(
                k8sClient, new WildFlyServerController(k8sClient), "/wildflyserver.crd.yaml");
        integrationTestSupport.cleanup();
    }

    @Test
    public void createSimpleWildFlyServer() {
        integrationTestSupport.teardownIfSuccess(
                () -> {
                    final var applicationImage = "my-app:latest";
                    final var replicas = 1;
                    WildFlyServer resource = WildFlyServerSupport.create("wildfly-" + UUID.randomUUID().toString(), applicationImage, replicas);
                    integrationTestSupport.createResource(resource);

                    awaitStatusUpdated(resource.getMetadata().getName(), replicas);
                    // wait for sure, there are no more events
                    waitXms(300);

                    WildFlyServer wfly = (WildFlyServer) integrationTestSupport.getCustomResource(resource.getMetadata().getName());
                    assertThat(wfly.getSpec().getApplicationImage()).isEqualTo(applicationImage);
                    assertThat(wfly.getSpec().getReplicas()).isEqualTo(replicas);
                    assertThat(wfly.getStatus().getReplicas()).isEqualTo(replicas);
                });
    }

    void awaitStatusUpdated(String name, int expectedReplicas) {
        await("cr status updated")
                .atMost(5, TimeUnit.SECONDS)
                .untilAsserted(
                        () -> {
                            var wfly = (WildFlyServer) integrationTestSupport.getCustomResource(name);
                            assertThat(wfly.getMetadata().getFinalizers()).hasSize(1);
                            assertThat(wfly).isNotNull();
                            assertThat(wfly.getStatus()).isNotNull();
                            assertThat(wfly.getStatus().getReplicas()).isEqualTo(expectedReplicas);
                        });
    }


    public static void waitXms(int x) {
        try {
            Thread.sleep(x);
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }
}
