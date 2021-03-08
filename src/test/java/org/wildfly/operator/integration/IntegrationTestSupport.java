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

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.javaoperatorsdk.operator.Operator;
import io.javaoperatorsdk.operator.api.ResourceController;
import io.javaoperatorsdk.operator.config.runtime.DefaultConfigurationService;
import io.javaoperatorsdk.operator.processing.retry.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wildfly.operator.WildFlyServer;

public class IntegrationTestSupport {

    public static final String TEST_NAMESPACE = "wildfly-operator-integration-test";
    public static final String TEST_CUSTOM_RESOURCE_PREFIX = "test-custom-resource-";

    private static final Logger log = LoggerFactory.getLogger(IntegrationTestSupport.class);
    private KubernetesClient k8sClient;
    private MixedOperation<
            CustomResource, KubernetesResourceList<CustomResource>, Resource<CustomResource>>
            crOperations;
    private Operator operator;
    private ResourceController controller;

    public void initialize(
            KubernetesClient k8sClient, ResourceController controller, String crdPath) {
        initialize(k8sClient, controller, crdPath, null);
    }

    public void initialize(
            KubernetesClient k8sClient, ResourceController controller, String crdPath, Retry retry) {
        log.info("Initializing integration test in namespace {}", TEST_NAMESPACE);
        this.k8sClient = k8sClient;

        // create the namespace containing the test resources
        final var namespaces = k8sClient.namespaces();
        if (namespaces.withName(TEST_NAMESPACE).get() == null) {
            namespaces.create(
                    new NamespaceBuilder().withNewMetadata().withName(TEST_NAMESPACE).endMetadata().build());
        }

        // create or replace the WildFlyServer CRD
        loadCRDAndApplyToCluster(crdPath);

        this.controller = controller;

        final var configurationService = DefaultConfigurationService.instance();
        final var config = configurationService.getConfigurationFor(controller);
        final var customResourceClass = config.getCustomResourceClass();
        this.crOperations = k8sClient.customResources(customResourceClass);
        operator = new Operator(k8sClient, configurationService);
        operator.register(controller);
        log.info("Operator is running with {}", controller.getClass().getCanonicalName());
    }

    public CustomResourceDefinition loadCRDAndApplyToCluster(String classPathYaml) {
        CustomResourceDefinition crd = loadYaml(CustomResourceDefinition.class, classPathYaml);
        k8sClient.apiextensions().v1().customResourceDefinitions().createOrReplace(crd);
        return crd;
    }

    public void cleanup() {
        log.info("Cleaning up namespace {}", TEST_NAMESPACE);

        // we depend on the actual operator from the startup to handle the finalizers and clean up
        // resources from previous test runs
        crOperations.inNamespace(TEST_NAMESPACE).delete(crOperations.list().getItems());

        await("all CRs cleaned up")
                .atMost(60, TimeUnit.SECONDS)
                .untilAsserted(
                        () -> assertThat(crOperations.inNamespace(TEST_NAMESPACE).list().getItems()).isEmpty());

        k8sClient
                .configMaps()
                .inNamespace(TEST_NAMESPACE)
                .withLabel("managedBy", controller.getClass().getSimpleName())
                .delete();

        await("all config maps cleaned up")
                .atMost(60, TimeUnit.SECONDS)
                .untilAsserted(
                        () -> {
                            assertThat(
                                    k8sClient
                                            .configMaps()
                                            .inNamespace(TEST_NAMESPACE)
                                            .withLabel("managedBy", controller.getClass().getSimpleName())
                                            .list()
                                            .getItems()
                                            .isEmpty());
                        });


        log.info("Cleaned up namespace " + TEST_NAMESPACE);
    }

    /**
     * Use this method to execute the cleanup of the integration test namespace only in case the test
     * was successful. This is useful to keep the Kubernetes resources around to debug a failed test
     * run. Unfortunately I couldn't make this work with standard JUnit methods as the @AfterAll
     * method doesn't know if the tests succeeded or not.
     *
     * @param test The code of the actual test.
     * @throws Exception if the test threw an exception.
     */
    public void teardownIfSuccess(TestRun test) {
        try {
            test.run();

            log.info("Deleting namespace {} and stopping operator", TEST_NAMESPACE);
            Namespace namespace = k8sClient.namespaces().withName(TEST_NAMESPACE).get();
            if (namespace.getStatus().getPhase().equals("Active")) {
                k8sClient.namespaces().withName(TEST_NAMESPACE).delete();
            }
            await("namespace deleted")
                    .atMost(90, SECONDS)
                    .until(() -> k8sClient.namespaces().withName(TEST_NAMESPACE).get() == null);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        } finally {
            k8sClient.close();
        }
    }

    private <T> T loadYaml(Class<T> clazz, String yaml) {
        try (InputStream is = getClass().getResourceAsStream(yaml)) {
            return Serialization.unmarshal(is, clazz);
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot find yaml on classpath: " + yaml);
        }
    }

    public CustomResource getCustomResource(String name) {
        return crOperations.inNamespace(TEST_NAMESPACE).withName(name).get();
    }

    public void createResource(CustomResource resource) {
        crOperations.inNamespace(TEST_NAMESPACE).create(resource);
    }

    public interface TestRun {

        void run() throws Exception;
    }
}