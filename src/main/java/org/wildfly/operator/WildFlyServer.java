package org.wildfly.operator;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.ShortNames;
import io.fabric8.kubernetes.model.annotation.Singular;
import io.fabric8.kubernetes.model.annotation.Version;

@Group("wildfly.org")
@Version("v1alpha1")
@ShortNames("wfly")
@Singular("wildflyserver")
public class WildFlyServer extends CustomResource<WildFlyServerSpec, WildFlyServerStatus> implements Namespaced {}