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

import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import static org.eclipse.jkube.maven.plugin.mojo.Openshift.DEFAULT_LOG_PREFIX;

/**
 * This goal forks the install goal then applies the generated kubernetes resources to the current cluster.
 *
 * Note that the goals oc:resource and oc:build must be bound to the proper execution phases.
 *
 * @author roland
 * @since 09/06/16
 */

@Mojo(name = "deploy", requiresDependencyResolution = ResolutionScope.COMPILE, defaultPhase = LifecyclePhase.VALIDATE)
@Execute(phase = LifecyclePhase.INSTALL)
public class OpenshiftDeployMojo extends DeployMojo {

    @Override
    protected String getLogPrefix() {
        return DEFAULT_LOG_PREFIX;
    }
}
