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

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class OperatorConfig {


    @ConfigProperty(name = "label.app.managed.by", defaultValue = "wildfly-operator")
    String managedByLabel;

    @ConfigProperty(name = "label.app.runtime", defaultValue = "wildfly")
    String runtimeLabel;

    public Map<String, String> labelsFor(String wildflyServerName) {
        Map<String, String> labels = new HashMap<>();
        labels.put("app.kubernetes.io/name", wildflyServerName);
        labels.put("app.kubernetes.io/managed-by", managedByLabel);
        labels.put("app.kubernetes.io/runtime", runtimeLabel);
        return labels;
    }

    public String getManagedByLabel() {
        return managedByLabel;
    }
}