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
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.kit.build.service.docker.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.OpenShiftBuildStrategy;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.eclipse.jkube.kit.config.service.BuildServiceConfig;
import org.eclipse.jkube.maven.plugin.mojo.OpenShift;

import java.util.List;

import static org.eclipse.jkube.kit.config.resource.RuntimeMode.kubernetes;

/**
 * Builds the docker images configured for this project via a Docker or S2I binary build.
 *
 * @author roland
 */
@Mojo(name = "build", defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST, requiresDependencyResolution = ResolutionScope.COMPILE)
public class OpenshiftBuildMojo extends BuildMojo {

    /**
     * Whether to perform a Kubernetes build (i.e. against a vanilla Docker daemon) or
     * an OpenShift build (with a Docker build against the OpenShift API server.
     */
    @Parameter(name="mode", property = "jkube.mode")
    protected RuntimeMode configuredRuntimeMode = RuntimeMode.DEFAULT;

    /**
     * OpenShift build mode when an OpenShift build is performed.
     * Can be either "s2i" for an s2i binary build mode or "docker" for a binary
     * docker mode.
     */
    @Parameter(property = "jkube.build.strategy")
    protected OpenShiftBuildStrategy buildStrategy = OpenShiftBuildStrategy.s2i;

    /**
     * The name of pullSecret to be used to pull the base image in case pulling from a protected
     * registry which requires authentication.
     */
    @Parameter(property = "jkube.build.pullSecret", defaultValue = "pullsecret-jkube")
    protected String openshiftPullSecret;

    /**
     * The S2I binary builder BuildConfig name suffix appended to the image name to avoid
     * clashing with the underlying BuildConfig for the Jenkins pipeline
     */
    @Parameter(property = "jkube.s2i.buildNameSuffix", defaultValue = "-s2i")
    protected String s2iBuildNameSuffix;

    /**
     * Allow the ImageStream used in the S2I binary build to be used in standard
     * Kubernetes resources such as Deployment or StatefulSet.
     */
    @Parameter(property = "jkube.s2i.imageStreamLookupPolicyLocal", defaultValue = "true")
    protected boolean s2iImageStreamLookupPolicyLocal = true;

    @Override
    protected boolean isDockerAccessRequired() {
        return runtimeMode == kubernetes;
    }

    @Override
    public RuntimeMode getConfiguredRuntimeMode() {
        return configuredRuntimeMode;
    }

    public List<ImageConfiguration> customizeConfig(List<ImageConfiguration> configs) {
        if (runtimeMode == RuntimeMode.openshift) {
            log.info("Using [[B]]OpenShift[[B]] build with strategy [[B]]%s[[B]]", buildStrategy.getLabel());
        }
        return super.customizeConfig(configs);
    }

    @Override
    protected BuildServiceConfig.BuildServiceConfigBuilder buildServiceConfigBuilder() {
        return super.buildServiceConfigBuilder()
            .openshiftBuildStrategy(buildStrategy)
            .openshiftPullSecret(openshiftPullSecret)
            .s2iBuildNameSuffix(s2iBuildNameSuffix)
            .s2iImageStreamLookupPolicyLocal(s2iImageStreamLookupPolicyLocal);
    }

    @Override
    protected GeneratorContext.GeneratorContextBuilder generatorContextBuilder() throws DependencyResolutionRequiredException {
        return super.generatorContextBuilder()
            .strategy(buildStrategy);
    }

    @Override
    protected String getLogPrefix() {
        return OpenShift.DEFAULT_LOG_PREFIX;
    }

    /**
     * Sets the configured {@link RuntimeMode} to be considered when resolving the effective runtime mode.
     *
     * <p>n.b this is a workaround for <code>{@code @Parameter(name="mode")}</code> being ignored
     *
     * @see <a href="https://issues.apache.org/jira/browse/MPLUGINTESTING-56">MPLUGINTESTING-56</a>
     * @see <a href="https://stackoverflow.com/questions/30913685/maven-annotation-api-parameter-name-method-seems-to-not-work">maven-annotation-api-parameter-name-method-seems-to-not-work</a>
     *
     * @param mode configured RuntimeMode
     */
    public void setMode(RuntimeMode mode) {
        configuredRuntimeMode = mode;
    }
}
