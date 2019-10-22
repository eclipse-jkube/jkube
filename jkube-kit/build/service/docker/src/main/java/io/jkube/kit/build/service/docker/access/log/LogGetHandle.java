package io.jkube.kit.build.service.docker.access.log;


import io.jkube.kit.build.service.docker.access.DockerAccessException;

/**
 * @author roland
 * @since 29/11/14
 */
public interface LogGetHandle {

    void finish();

    boolean isError();

    DockerAccessException getException();
}
