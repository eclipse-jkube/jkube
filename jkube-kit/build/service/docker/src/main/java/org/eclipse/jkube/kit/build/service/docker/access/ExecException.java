/*
 * Copyright (c) 2019 Red Hat, Inc.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at:
 *
 *     https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.jkube.kit.build.service.docker.access;

import org.eclipse.jkube.kit.build.api.model.ContainerDetails;
import org.eclipse.jkube.kit.build.api.model.ExecDetails;

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
