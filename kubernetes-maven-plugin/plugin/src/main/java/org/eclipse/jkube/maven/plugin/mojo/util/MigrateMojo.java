/*
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
package org.eclipse.jkube.maven.plugin.mojo.util;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.eclipse.jkube.kit.common.util.MavenUtil;
import org.eclipse.jkube.maven.plugin.mojo.build.AbstractJKubeMojo;

@Mojo(name = "migrate", requiresDependencyResolution = ResolutionScope.RUNTIME, defaultPhase = LifecyclePhase.INSTALL)
public class MigrateMojo extends AbstractJKubeMojo {
    private static final String PLUGIN_ARTIFACT_ID = "kubernetes-maven-plugin";
    private static final String PLUGIN_GROUP_ID = "org.eclipse.jkube";

    @Override
    public void executeInternal() throws MojoExecutionException {
        log = createLogger(null);
        try {
            jkubeServiceHub.getMigrateService().migrate(PLUGIN_GROUP_ID, getPluginArtifactId(), MavenUtil.getVersion(PLUGIN_GROUP_ID, getPluginArtifactId()));
        } catch (Exception exception) {
            throw new MojoExecutionException("Unable to migrate project to Eclipse JKube: ", exception);
        }
    }

    protected String getPluginArtifactId() {
        return PLUGIN_ARTIFACT_ID;
    }
}
