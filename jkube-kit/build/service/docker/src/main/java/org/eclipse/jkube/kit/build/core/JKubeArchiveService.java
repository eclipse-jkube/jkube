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
package org.eclipse.jkube.kit.build.core;

import java.io.File;
import java.io.IOException;

import org.eclipse.jkube.kit.build.core.assembly.ArchiverCustomizer;
import org.eclipse.jkube.kit.build.core.assembly.DockerAssemblyManager;
import org.eclipse.jkube.kit.build.core.config.JKubeBuildConfiguration;
import org.eclipse.jkube.kit.common.KitLogger;

/**
 * @author roland
 * @since 30/11/15
 */
public class JKubeArchiveService {

    private DockerAssemblyManager dockerAssemblyManager;

    public JKubeArchiveService(DockerAssemblyManager dockerAssemblyManager) {
        this.dockerAssemblyManager = dockerAssemblyManager;
    }

    public File createArchive(String imageName, JKubeBuildConfiguration buildConfig, JKubeBuildContext ctx, KitLogger log)
        throws IOException {
        return createArchive(imageName, buildConfig, ctx, log, null);
    }

    File createArchive(String imageName, JKubeBuildConfiguration buildConfig, JKubeBuildContext ctx, KitLogger log, ArchiverCustomizer customizer)
        throws IOException {
        return dockerAssemblyManager.createDockerTarArchive(imageName, ctx, buildConfig, log, customizer);
    }
}
