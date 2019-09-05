package io.jshift.kit.config.resource;

import org.apache.maven.plugins.annotations.Parameter;

public class ServiceAccountConfig {
    @Parameter
    private String name;

    @Parameter
    private String deploymentRef;

    public ServiceAccountConfig(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getDeploymentRef() {
        return deploymentRef;
    }
}

