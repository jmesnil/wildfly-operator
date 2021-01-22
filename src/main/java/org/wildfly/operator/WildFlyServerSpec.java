package org.wildfly.operator;

public class WildFlyServerSpec {

    private String applicationImage;
    private int replicas;

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