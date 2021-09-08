package org.wildfly;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Plural;
import io.fabric8.kubernetes.model.annotation.ShortNames;
import io.fabric8.kubernetes.model.annotation.Version;

@Version("v1alpha1")
@Group("wildfly.org")
@Kind("WildFlyServer")
@Plural("wildflyservers")
@ShortNames("wfly")
public class WildFlyServer extends CustomResource<WildFlyServerSpec, WildFlyServerStatus> implements Namespaced {}

