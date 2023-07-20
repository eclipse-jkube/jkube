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

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(name = "migrate", requiresDependencyResolution = ResolutionScope.RUNTIME, defaultPhase = LifecyclePhase.INSTALL)
public class OpenShiftMigrateMojo extends MigrateMojo {
    private static final String PLUGIN_ARTIFACT_ID = "openshift-maven-plugin";

    @Override
    protected String getPluginArtifactId() {
        return PLUGIN_ARTIFACT_ID;
    }
}
