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
package org.wildfly.operator;

import javax.inject.Inject;

import io.fabric8.openshift.client.OpenShiftClient;
import io.javaoperatorsdk.operator.Operator;
import io.javaoperatorsdk.operator.api.config.ConfigurationService;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@QuarkusMain
public class WildFlyOperator implements QuarkusApplication {

    private static final Logger log = LoggerFactory.getLogger(WildFlyServerController.class);

    @Inject
    OpenShiftClient client;

    @Inject
    Operator operator;

    @Inject
    ConfigurationService configuration;

    public static void main(String... args) {
        Quarkus.run(WildFlyOperator.class, args);
    }

    @Override
    public int run(String... args) throws Exception {
        WildFlyServerController controller = new WildFlyServerController(client);
        final var config = configuration.getConfigurationFor(controller);

        log.info("Watching for CustomResourceDefinition: {}", config.getCRDName());
        log.info("Using Custom Resource {}", config.getCustomResourceClass());

        Quarkus.waitForExit();
        return 0;
    }
}
