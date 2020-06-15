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

import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.jkube.kit.common.util.ResourceClassifier;
import org.eclipse.jkube.kit.config.resource.PlatformMode;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.eclipse.jkube.maven.plugin.mojo.OpenShift;

/**
 * Generates or copies the Kubernetes JSON file and attaches it to the build so its
 * installed and released to maven repositories like other build artifacts.
 */
@Mojo(name = "resource", defaultPhase = LifecyclePhase.PROCESS_RESOURCES, requiresDependencyResolution = ResolutionScope.COMPILE)
public class OpenshiftResourceMojo extends ResourceMojo {

    /**
     * The OpenShift deploy timeout in seconds:
     * See this issue for background of why for end users on slow wifi on their laptops
     * DeploymentConfigs usually barf: https://github.com/openshift/origin/issues/10531
     *
     * Please follow also the discussion at
     * <ul>
     *     <li>https://github.com/fabric8io/fabric8-maven-plugin/pull/944#discussion_r116962969</li>
     *     <li>https://github.com/fabric8io/fabric8-maven-plugin/pull/794</li>
     * </ul>
     * and the references within it for the reason of this ridiculous long default timeout
     * (in short: Its because Docker image download times are added to the deployment time, making
     * the default of 10 minutes quite unusable if multiple images are included in the deployment).
     */
    @Parameter(property = "jkube.openshift.deployTimeoutSeconds", defaultValue = "3600")
    private Long openshiftDeployTimeoutSeconds;

    /**
     * If set to true it would set the container image reference to "", this is done to handle weird
     * behavior of OpenShift 3.7 in which subsequent rollouts lead to ImagePullErr
     *
     * Please see discussion at
     * <ul>
     *     <li>https://github.com/openshift/origin/issues/18406</li>
     *     <li>https://github.com/fabric8io/fabric8-maven-plugin/issues/1130</li>
     * </ul>
     */
    @Parameter(property = "jkube.openshift.trimImageInContainerSpec", defaultValue = "false")
    private Boolean trimImageInContainerSpec;

    @Parameter(property = "jkube.openshift.generateRoute", defaultValue = "true")
    private Boolean generateRoute;

    @Parameter(property = "jkube.openshift.enableAutomaticTrigger", defaultValue = "true")
    private Boolean enableAutomaticTrigger;

    @Parameter(property = "jkube.openshift.imageChangeTrigger", defaultValue = "true")
    private Boolean enableImageChangeTrigger;

    @Parameter(property = "jkube.openshift.enrichAllWithImageChangeTrigger", defaultValue = "false")
    private Boolean erichAllWithImageChangeTrigger;

    @Override
    protected String getLogPrefix() {
        return OpenShift.DEFAULT_LOG_PREFIX;
    }

    @Override
    protected PlatformMode getPlatformMode() {
        return PlatformMode.openshift;
    }

    @Override
    protected ResourceClassifier getResourceClassifier() {
        return ResourceClassifier.OPENSHIFT;
    }

    @Override
    protected RuntimeMode getRuntimeMode() {
        return RuntimeMode.OPENSHIFT;
    }
}
