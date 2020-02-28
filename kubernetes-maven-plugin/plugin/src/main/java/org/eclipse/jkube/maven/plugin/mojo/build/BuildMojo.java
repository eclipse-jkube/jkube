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

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.eclipse.jkube.kit.build.service.docker.DockerAccessFactory;
import org.eclipse.jkube.kit.build.service.docker.ServiceHub;
import org.eclipse.jkube.kit.build.service.docker.access.DockerAccess;
import org.eclipse.jkube.kit.build.service.docker.access.log.LogOutputSpecFactory;
import org.eclipse.jkube.kit.build.service.docker.config.ConfigHelper;
import org.eclipse.jkube.kit.common.util.AnsiLogger;
import org.eclipse.jkube.kit.common.util.MavenUtil;
import org.eclipse.jkube.kit.config.access.ClusterAccess;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;
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

            // Build the JKube service hub
            jkubeServiceHub = new JKubeServiceHub.Builder()
                    .log(log)
                    .clusterAccess(clusterAccess)
                    .platformMode(mode)
                    .dockerServiceHub(hub)
                    .buildServiceConfig(getBuildServiceConfig())
                    .jkubeProject(MavenUtil.convertMavenProjectToJKubeProject(project, session))
                    .build();

            executeBuildGoal(hub);

            jkubeServiceHub.getBuildService().postProcess(getBuildServiceConfig());
        } catch (IOException | DependencyResolutionRequiredException exception) {
            throw new MojoExecutionException(exception.getMessage());
        }
    }

    public void executeDockerBuild() throws MojoExecutionException, MojoFailureException {
        if (!skip) {
            log = new AnsiLogger(getLog(), useColor, verbose, !settings.getInteractiveMode(), getLogPrefix());
            authConfigFactory.setLog(log);
            imageConfigResolver.setLog(log);

            LogOutputSpecFactory logSpecFactory = new LogOutputSpecFactory(useColor, logStdout, logDate);


            DockerAccess access = null;
            try {
                ConfigHelper.validateExternalPropertyActivation(MavenUtil.convertMavenProjectToJKubeProject(project, session), images);

                // The 'real' images configuration to use (configured images + externally resolved images)
                this.minimalApiVersion = initImageConfiguration(getBuildTimestamp());
                if (isDockerAccessRequired()) {
                    DockerAccessFactory.DockerAccessContext dockerAccessContext = getDockerAccessContext();
                    access = dockerAccessFactory.createDockerAccess(dockerAccessContext);
                }
                ServiceHub serviceHub = serviceHubFactory.createServiceHub(access, log, logSpecFactory);
                executeInternal(serviceHub);
            } catch (IOException exp) {
                logException(exp);
                throw new MojoExecutionException(exp.getMessage());
            } catch (MojoExecutionException exp) {
                logException(exp);
                throw exp;
            } catch (DependencyResolutionRequiredException dependencyException) {
                logException(dependencyException);
                throw new MojoExecutionException("Instructed to use project classpath, but cannot. Continuing build if we can: ", dependencyException);
            } finally {
                if (access != null) {
                    access.shutdown();
                }
            }
        }
    }
}