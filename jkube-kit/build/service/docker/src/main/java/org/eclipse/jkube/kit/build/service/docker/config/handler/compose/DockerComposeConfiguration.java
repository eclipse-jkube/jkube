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
package org.eclipse.jkube.kit.build.service.docker.config.handler.compose;

import java.util.Map;

public class DockerComposeConfiguration {

    private final String basedir;
    private final String composeFile;
    private final boolean ignoreBuild;
    public DockerComposeConfiguration(Map<String, String> config) {
        basedir = config.getOrDefault("basedir", "src/main/docker");
        composeFile = config.getOrDefault("composerFile", "docker-compose.yml");
        ignoreBuild = Boolean.parseBoolean(config.get("ignoreBuild"));
    }

    String getBasedir() {
        return basedir;
    }

    String getComposeFile() {
        return composeFile;
    }

    public boolean isIgnoreBuild() {
        return ignoreBuild;
    }
}
