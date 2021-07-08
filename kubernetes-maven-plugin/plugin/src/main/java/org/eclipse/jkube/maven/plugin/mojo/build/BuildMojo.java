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
package org.eclipse.jkube.maven.plugin.mojo.build;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;


/**
 * Builds the docker images configured for this project via a Docker or S2I binary build.
 *
 * @author roland
 */
@Mojo(name = "build", defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST, requiresDependencyResolution = ResolutionScope.COMPILE)
public class BuildMojo extends AbstractDockerMojo implements Contextualizable {

    @Override
    protected boolean canExecute() {
        return super.canExecute() && !skipBuild;
    }

    @Override
    public void executeInternal() throws MojoExecutionException {
        if (skipBuild) {
            return;
        }
        if (shouldSkipBecauseOfPomPackaging()) {
            getLog().info("Disabling docker build for pom packaging");
            return;
        }
        if (getResolvedImages().isEmpty()) {
            log.warn("No image build configuration found or detected");
        }

        executeBuildGoal();

        jkubeServiceHub.getBuildService().postProcess();
    }

}