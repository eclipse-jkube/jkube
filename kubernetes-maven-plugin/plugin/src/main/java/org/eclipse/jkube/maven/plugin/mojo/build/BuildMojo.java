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
import org.eclipse.jkube.kit.common.util.EnvUtil;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;

import java.io.File;

import static org.eclipse.jkube.kit.common.util.BuildReferenceDateUtil.getBuildTimestamp;
import static org.eclipse.jkube.kit.common.util.BuildReferenceDateUtil.getBuildTimestampFile;


/**
 * Builds the docker images configured for this project via a Docker or S2I binary build.
 *
 * @author roland
 */
@Mojo(name = "build", defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST, requiresDependencyResolution = ResolutionScope.COMPILE)
public class BuildMojo extends AbstractContainerImageBuildMojo implements Contextualizable {

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

        jkubeServiceHub.getBuildService().postProcess(jkubeServiceHub.getBuildServiceConfig());
    }

    boolean shouldSkipBecauseOfPomPackaging() {
        if (!project.getPackaging().equals("pom")) {
            // No pom packaging
            return false;
        }
        if (skipBuildPom != null) {
            // If configured take the config option
            return skipBuildPom;
        }

        // Not specified: Skip if no image with build configured, otherwise don't skip
        for (ImageConfiguration image : getResolvedImages()) {
            if (image.getBuildConfiguration() != null) {
                return false;
            }
        }
        return true;
    }

    private void executeBuildGoal() throws MojoExecutionException {
        if (skipBuild) {
            return;
        }

        // Iterate over all the ImageConfigurations and process one by one
        for (ImageConfiguration imageConfig : getResolvedImages()) {
            processImageConfig(imageConfig);
        }
    }

    /**
     * Helper method to process an ImageConfiguration.
     *
     * @param aImageConfig ImageConfiguration that would be forwarded to build and tag
     * @throws MojoExecutionException Exception in executing plugin goal
     */
    private void processImageConfig(ImageConfiguration aImageConfig) throws MojoExecutionException {
        BuildConfiguration buildConfig = aImageConfig.getBuildConfiguration();

        if (buildConfig != null) {
            if (Boolean.TRUE.equals(buildConfig.getSkip())) {
                log.info("%s : Skipped building", aImageConfig.getDescription());
            } else {
                buildAndTag(aImageConfig);
            }
        }
    }

    private void buildAndTag(ImageConfiguration imageConfig)
            throws MojoExecutionException {

        try {
            // TODO need to refactor d-m-p to avoid this call
            EnvUtil.storeTimestamp(getBuildTimestampFile(project.getBuild().getDirectory(), DOCKER_BUILD_TIMESTAMP), getBuildTimestamp(getPluginContext(), CONTEXT_KEY_BUILD_TIMESTAMP, project.getBuild().getDirectory(), DOCKER_BUILD_TIMESTAMP));

            jkubeServiceHub.getBuildService().build(imageConfig);

        } catch (Exception ex) {
            throw new MojoExecutionException("Failed to execute the build", ex);
        }
    }

    File getAndEnsureOutputDirectory() {
        File outputDir = new File(new File(project.getBuild().getDirectory()), DOCKER_EXTRA_DIR);
        if (!outputDir.exists()) {
            boolean bCreated = outputDir.mkdirs();
            if (!bCreated) {
                log.warn("Couldn't create directory: %s", outputDir.getAbsolutePath());
            }
        }
        return outputDir;
    }
}