package io.jkube.kit.config.resource;


import org.apache.maven.plugins.annotations.Parameter;

public class SecretConfig {

    @Parameter
    private String name;

    @Parameter
    private String dockerServerId;

    @Parameter
    private String namespace;

    public String getName() {
        return name;
    }

    public String getDockerServerId() {
        return dockerServerId;
    }

    public String getNamespace() {
        return namespace;
    }

}
