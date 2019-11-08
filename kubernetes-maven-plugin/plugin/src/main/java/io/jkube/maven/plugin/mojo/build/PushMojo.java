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
package io.jkube.maven.plugin.mojo.build;


import io.jkube.kit.build.service.docker.ImageConfiguration;
import io.jkube.kit.build.service.docker.JibBuildService;
import io.jkube.kit.build.service.docker.ServiceHub;
import io.jkube.kit.build.service.docker.auth.AuthConfigFactory;
import io.jkube.kit.config.access.ClusterAccess;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;

import java.util.List;

/**
 * Uploads the built Docker images to a Docker registry
 *
 * @author roland
 * @since 16/03/16
 */
@Mojo(name = "push", defaultPhase = LifecyclePhase.INSTALL, requiresDependencyResolution = ResolutionScope.COMPILE)
public class PushMojo extends AbstractDockerMojo {

    @Parameter(property = "docker.skip.push", defaultValue = "false")
    protected boolean skipPush;

    // Registry to use for push operations if no registry is specified
    @Parameter(property = "docker.push.registry")
    private String pushRegistry;

    /**
     * Skip building tags
     */
    @Parameter(property = "docker.skip.tag", defaultValue = "false")
    private boolean skipTag;

    @Parameter(property = "docker.push.retries", defaultValue = "0")
    private int retries;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip || skipPush) {
            return;
        }
        clusterAccess = new ClusterAccess(getClusterConfiguration());
        super.execute();
    }

    @Override
    public void contextualize(Context context) throws ContextException {
        authConfigFactory = new AuthConfigFactory((PlexusContainer) context.get(PlexusConstants.PLEXUS_KEY));
    }

    @Override
    public void executeInternal(ServiceHub serviceHub) throws MojoExecutionException {
        if (skipPush) {
            return;
        }

        try {
            List<ImageConfiguration> imageConfigurationList = getResolvedImages();
            if (Boolean.TRUE.equals(isJib)) {
                log.info("Building Container image with [[B]]JIB(Java Image Builder)[[B]] mode");
                JibBuildService jibBuildService = new JibBuildService(getBuildContext(), createMojoParameters(), log);
                for (ImageConfiguration imageConfig : imageConfigurationList) {
                    jibBuildService.buildImage(imageConfig, false);
                }
            } else {
                serviceHub.getRegistryService().pushImages(imageConfigurationList, retries, getRegistryConfig(pushRegistry), skipTag);
            }

        } catch (Exception exp) {
            throw new MojoExecutionException(exp.getMessage());
        }
    }
}
