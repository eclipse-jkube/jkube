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

import org.eclipse.jkube.kit.build.service.docker.DockerAccessFactory;
import org.eclipse.jkube.kit.build.service.docker.ServiceHub;
import org.eclipse.jkube.kit.build.service.docker.access.DockerAccess;
import org.eclipse.jkube.kit.build.service.docker.access.log.LogOutputSpecFactory;
import org.eclipse.jkube.kit.build.service.docker.config.ConfigHelper;
import org.eclipse.jkube.kit.build.service.docker.helper.AnsiLogger;
import org.eclipse.jkube.kit.config.access.ClusterAccess;
import org.eclipse.jkube.kit.config.service.JkubeServiceHub;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;

import java.io.IOException;

/**
 * Builds the docker images configured for this project via a Docker or S2I binary build.
 *
 * @author roland
 * @since 16/03/16
 */
@Mojo(name = "build", defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST, requiresDependencyResolution = ResolutionScope.COMPILE)
public class BuildMojo extends AbstractDockerMojo implements Contextualizable {

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip || skipBuild) {
            return;
        }
        clusterAccess = new ClusterAccess(getClusterConfiguration());
        // Platform mode is already used in executeInternal()
        executeDockerBuild();
    }

    @Override
    public void executeInternal(ServiceHub hub) throws MojoExecutionException {
        if (skipBuild) {
            return;
        }
        try {
            if (shouldSkipBecauseOfPomPackaging()) {
                getLog().info("Disabling docker build for pom packaging");
                return;
            }
            if (getResolvedImages().size() == 0) {
                log.warn("No image build configuration found or detected");
            }

            // Build the Jkube service hub
            jkubeServiceHub = new JkubeServiceHub.Builder()
                    .log(log)
                    .clusterAccess(clusterAccess)
                    .platformMode(mode)
                    .dockerServiceHub(hub)
                    .buildServiceConfig(getBuildServiceConfig())
                    .repositorySystem(repositorySystem)
                    .mavenProject(project)
                    .build();

            executeBuildGoal(hub);

            jkubeServiceHub.getBuildService().postProcess(getBuildServiceConfig());
        } catch (IOException exception) {
            throw new MojoExecutionException(exception.getMessage());
        }
    }

    public void executeDockerBuild() throws MojoExecutionException, MojoFailureException {
        if (!skip) {
            log = new AnsiLogger(getLog(), useColor, verbose, !settings.getInteractiveMode(), getLogPrefix());
            authConfigFactory.setLog(log);
            imageConfigResolver.setLog(log);

            LogOutputSpecFactory logSpecFactory = new LogOutputSpecFactory(useColor, logStdout, logDate);

            ConfigHelper.validateExternalPropertyActivation(project, images);

            DockerAccess access = null;
            try {
                // The 'real' images configuration to use (configured images + externally resolved images)
                this.minimalApiVersion = initImageConfiguration(getBuildTimestamp());
                if (isDockerAccessRequired()) {
                    DockerAccessFactory.DockerAccessContext dockerAccessContext = getDockerAccessContext();
                    access = dockerAccessFactory.createDockerAccess(dockerAccessContext);
                }
                ServiceHub serviceHub = serviceHubFactory.createServiceHub(project, session, access, log, logSpecFactory);
                executeInternal(serviceHub);
            } catch (IOException exp) {
                logException(exp);
                throw new MojoExecutionException(exp.getMessage());
            } catch (MojoExecutionException exp) {
                logException(exp);
                throw exp;
            } finally {
                if (access != null) {
                    access.shutdown();
                }
            }
        }
    }
}