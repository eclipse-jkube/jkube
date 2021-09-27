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
package org.eclipse.jkube.maven.plugin.mojo.develop;

import io.fabric8.kubernetes.client.KubernetesClient;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.kit.config.image.build.JKubeBuildStrategy;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.eclipse.jkube.maven.plugin.mojo.OpenShift;

import java.io.File;

import static org.eclipse.jkube.maven.plugin.mojo.build.ApplyMojo.DEFAULT_OPENSHIFT_MANIFEST;


// TODO: Similar to the DebugMojo the WatchMojo should scale down any deployment to 1 replica (or ensure that its running only with one replica)
// The WatchEnricher has been removed since the enrichment shouldn't know anything about the mode running and should
// always create the same resources

/**
 * Used to automatically rebuild Docker images and restart containers in case of updates.
 */
@Mojo(name = "watch", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.COMPILE)
@Execute(goal = "deploy")
public class OpenshiftWatchMojo extends WatchMojo {

    /**
     * OpenShift build mode when an OpenShift build is performed.
     * Can be either "s2i" for an S2I binary build mode or "docker" for a binary
     * docker mode.
     */
    @Parameter(property = "jkube.build.strategy")
    protected JKubeBuildStrategy buildStrategy = JKubeBuildStrategy.s2i;

    /**
     * The generated openshift YAML file
     */
    @Parameter(property = "jkube.openshiftManifest", defaultValue = DEFAULT_OPENSHIFT_MANIFEST)
    private File openshiftManifest;

    @Override
    public RuntimeMode getConfiguredRuntimeMode() {
        return RuntimeMode.OPENSHIFT;
    }

    @Override
    public File getManifest(KubernetesClient kubernetesClient) {
        return OpenShift.getOpenShiftManifest(kubernetesClient, getKubernetesManifest(), openshiftManifest);
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

    @Override
    protected boolean isDockerAccessRequired() {
        return false;
    }
}
