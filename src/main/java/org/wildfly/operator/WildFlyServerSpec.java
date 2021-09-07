package org.wildfly.operator;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public class WildFlyServerSpec {

    private String applicationImage;
    private int replicas;

    // FIXME this should work after fabric8 5.7.2 (from whatever jackson version is incorporated)
    @JsonPropertyDescription("ApplicationImage is the name of the application image to be deployed")
    public String getApplicationImage() {
        return applicationImage;
    }

    public void setApplicationImage(String applicationImage) {
        this.applicationImage = applicationImage;
    }

    public int getReplicas() {
        return replicas;
    }

    public void setReplicas(int replicas) {
        this.replicas = replicas;
    }
}