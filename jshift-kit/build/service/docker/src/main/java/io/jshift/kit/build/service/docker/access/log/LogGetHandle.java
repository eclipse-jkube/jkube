package io.jshift.kit.build.service.docker.access.log;


import io.jshift.kit.build.service.docker.access.DockerAccessException;

/**
 * @author roland
 * @since 29/11/14
 */
public interface LogGetHandle {

    void finish();

    boolean isError();

    DockerAccessException getException();
}
