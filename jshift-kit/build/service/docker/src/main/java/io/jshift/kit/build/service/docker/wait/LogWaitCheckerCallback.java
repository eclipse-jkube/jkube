package io.jshift.kit.build.service.docker.wait;


/**
 * Interface called during waiting on log when a log line matches
 */
public interface LogWaitCheckerCallback {
    void matched();
}
