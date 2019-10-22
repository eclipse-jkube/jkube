package io.jkube.kit.build.service.docker.access;

import io.jkube.kit.build.api.model.ContainerDetails;
import io.jkube.kit.build.api.model.ExecDetails;

import java.util.Arrays;


/**
 * Exception thrown when the execution of an exec container failed
 *
 * @author roland
 * @since 18.01.18
 */
public class ExecException extends Exception {
    public ExecException(ExecDetails details, ContainerDetails container) {
        super(String.format(
                "Executing '%s' with args '%s' inside container '%s' [%s](%s) resulted in a non-zero exit code: %d",
                details.getEntryPoint(),
                Arrays.toString(details.getArguments()),
                container.getName(),
                container.getImage(),
                container.getId(),
                details.getExitCode()));
    }
}
