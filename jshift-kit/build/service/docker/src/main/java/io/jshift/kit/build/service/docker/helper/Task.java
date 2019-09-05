package io.jshift.kit.build.service.docker.helper;

/**
 * Represents a generic task to be executed on a object.
 */
public interface Task<T> {

    void execute(T object) throws Exception;

}
