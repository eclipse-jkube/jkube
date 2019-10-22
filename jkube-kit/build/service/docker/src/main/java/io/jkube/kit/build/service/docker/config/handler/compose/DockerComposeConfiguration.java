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
package io.jkube.kit.build.service.docker.config.handler.compose;

import java.util.Map;

public class DockerComposeConfiguration {

    private final String basedir;
    private final String composeFile;
    private final boolean ignoreBuild;
    public DockerComposeConfiguration(Map<String, String> config) {
        basedir = config.containsKey("basedir") ? config.get("basedir") : "src/main/docker";
        composeFile = config.containsKey("composeFile") ? config.get("composeFile") : "docker-compose.yml";
        ignoreBuild = config.containsKey("ignoreBuild") ? Boolean.parseBoolean(config.get("ignoreBuilder")) : false;
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
