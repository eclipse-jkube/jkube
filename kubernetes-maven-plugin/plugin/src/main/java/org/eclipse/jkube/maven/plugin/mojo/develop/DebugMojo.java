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

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import io.fabric8.kubernetes.client.dsl.FilterWatchListDeletable;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.api.model.DeploymentConfigSpec;
import io.fabric8.openshift.client.OpenShiftClient;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.KubernetesHelper;
import org.eclipse.jkube.kit.config.service.JKubeServiceException;
import org.eclipse.jkube.kit.config.service.PortForwardService;
import org.eclipse.jkube.kit.enricher.api.util.DebugConstants;
import org.eclipse.jkube.kit.enricher.api.util.KubernetesResourceUtil;
import org.eclipse.jkube.maven.plugin.mojo.build.ApplyMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.eclipse.jkube.maven.plugin.mojo.build.ApplyMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.util.Set;

/**
 * Ensures that the current app has debug enabled, then opens the debug port so that you can debug the latest pod
 * from your IDE
 */
@Mojo(name = "debug", requiresDependencyResolution = ResolutionScope.COMPILE, defaultPhase = LifecyclePhase.PACKAGE)
public class DebugMojo extends ApplyMojo {

    @Parameter(property = "jkube.debug.port", defaultValue = "5005")
    private String localDebugPort;

    @Parameter(property = "jkube.debug.suspend", defaultValue = "false")
    private boolean debugSuspend;

    @Override
    protected void applyEntities(KubernetesClient kubernetes, String namespace, String fileName, Set<HasMetadata> entities) throws Exception {
        try {
            PortForwardService.PortForwardThread portForwardThread = jkubeServiceHub.getDebugService().debug(kubernetes, namespace, fileName, entities, localDebugPort, debugSuspend, createLogger("[[Y]][W][[Y]] [[s]]"));
            portForwardThread.run();
        } catch (Exception exp) {
            throw new MojoExecutionException(exp.getMessage());
        }
    }
}


