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
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.eclipse.jkube.kit.common.util.SummaryUtil;

/**
 * Uploads the built Docker images to a Docker registry
 *
 * @author roland
 * @since 16/03/16
 */
@Mojo(name = "push", defaultPhase = LifecyclePhase.INSTALL, requiresDependencyResolution = ResolutionScope.COMPILE)
public class PushMojo extends AbstractDockerMojo {

    @Parameter(property = "jkube.skip.push", defaultValue = "false")
    protected boolean skipPush;

    // Registry to use for push operations if no registry is specified
    @Parameter(property = "jkube.docker.push.registry")
    private String pushRegistry;

    /**
     * Skip building tags
     */
    @Parameter(property = "jkube.skip.tag", defaultValue = "false")
    private boolean skipTag;

    @Parameter(property = "jkube.docker.push.retries", defaultValue = "0")
    private int retries;

    @Override
    protected boolean shouldSkip() {
        return super.shouldSkip() || skipPush;
    }

    @Override
    public void executeInternal() throws MojoExecutionException {
        if (skipPush) {
            return;
        }

        try {
            jkubeServiceHub.getBuildService().push(getResolvedImages(), retries, getRegistryConfig(pushRegistry), skipTag);
        } catch (Exception ex) {
            SummaryUtil.setFailureIfSummaryEnabledOrThrow(summaryEnabled, ex.getMessage(), () -> new MojoExecutionException(ex.getMessage(), ex));
        }
    }
}
