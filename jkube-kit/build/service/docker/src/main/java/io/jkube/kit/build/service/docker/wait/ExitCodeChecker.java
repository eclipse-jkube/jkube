/**
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
package io.jkube.kit.build.service.docker.wait;

import io.jkube.kit.build.service.docker.QueryService;
import io.jkube.kit.build.service.docker.access.DockerAccessException;

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
