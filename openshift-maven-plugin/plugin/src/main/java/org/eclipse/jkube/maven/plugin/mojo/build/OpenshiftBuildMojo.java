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
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.JKubeBuildStrategy;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.eclipse.jkube.kit.config.service.BuildServiceConfig;
import org.eclipse.jkube.maven.plugin.mojo.OpenShift;

import java.util.List;

import static org.eclipse.jkube.kit.config.resource.RuntimeMode.KUBERNETES;

/**
 * Builds the docker images configured for this project via a Docker or S2I binary build.
 *
 * @author roland
 */
@Mojo(name = "build", defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST, requiresDependencyResolution = ResolutionScope.COMPILE)
public class OpenshiftBuildMojo extends BuildMojo {

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
        return runtimeMode == KUBERNETES;
    }

    @Override
    public RuntimeMode getConfiguredRuntimeMode() {
        return RuntimeMode.OPENSHIFT;
    }

    public List<ImageConfiguration> customizeConfig(List<ImageConfiguration> configs) {
        if (runtimeMode == RuntimeMode.OPENSHIFT) {
            log.info("Using [[B]]OpenShift[[B]] build with strategy [[B]]%s[[B]]", getJKubeBuildStrategy().getLabel());
        }
        return super.customizeConfig(configs);
    }

    @Override
    protected BuildServiceConfig.BuildServiceConfigBuilder buildServiceConfigBuilder() {
        return super.buildServiceConfigBuilder()
            .jKubeBuildStrategy(getJKubeBuildStrategy())
            .openshiftPullSecret(openshiftPullSecret)
            .s2iBuildNameSuffix(s2iBuildNameSuffix)
            .s2iImageStreamLookupPolicyLocal(s2iImageStreamLookupPolicyLocal);
    }

    @Override
    protected GeneratorContext.GeneratorContextBuilder generatorContextBuilder() throws DependencyResolutionRequiredException {
        return super.generatorContextBuilder()
            .strategy(getJKubeBuildStrategy());
    }

    @Override
    protected String getLogPrefix() {
        return OpenShift.DEFAULT_LOG_PREFIX;
    }

    @Override
    protected JKubeBuildStrategy getJKubeBuildStrategy() {
        if (buildStrategy != null) {
            return buildStrategy;
        }
        return JKubeBuildStrategy.s2i;
    }

}
