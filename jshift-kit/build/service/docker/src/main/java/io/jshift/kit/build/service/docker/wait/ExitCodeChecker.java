package io.jshift.kit.build.service.docker.wait;

import io.jshift.kit.build.service.docker.QueryService;
import io.jshift.kit.build.service.docker.access.DockerAccessException;

public class ExitCodeChecker implements WaitChecker {

    private final int exitCodeExpected;
    private final String containerId;
    private final QueryService queryService;

    public ExitCodeChecker(int exitCodeExpected, QueryService queryService, String containerId) {
        this.exitCodeExpected = exitCodeExpected;
        this.containerId = containerId;
        this.queryService = queryService;
    }

    @Override
    public boolean check() {
        try {
            Integer exitCodeActual = queryService.getMandatoryContainer(containerId).getExitCode();
            // container still running
            return exitCodeActual != null && exitCodeActual == exitCodeExpected;
        } catch (DockerAccessException e) {
            return false;
        }
    }

    @Override
    public void cleanUp() {
        // No cleanup required
    }

    @Override
    public String getLogLabel() {
        return "on exit code " + exitCodeExpected;
    }
}
