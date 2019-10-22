/**
 * Copyright 2016 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package io.jkube.maven.plugin.mojo.build;


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
            serviceHub.getRegistryService().pushImages(getResolvedImages(), retries, getRegistryConfig(pushRegistry), skipTag);
        } catch (Exception exp) {
            throw new MojoExecutionException(exp.getMessage());
        }
    }
}
