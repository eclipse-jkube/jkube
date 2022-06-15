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

import org.eclipse.jkube.maven.plugin.mojo.build.ApplyMojo;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * This goal forks the install goal then applies the generated kubernetes resources to the current cluster.
 *
 * Note that the goals k8s:resource and k8s:build must be bound to the proper execution phases.
 *
 * @author roland
 */

@Mojo(name = "deploy", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, defaultPhase = LifecyclePhase.VALIDATE)
@Execute(phase = LifecyclePhase.INSTALL)
public class DeployMojo extends ApplyMojo {

    @Parameter(property = "jkube.skip.deploy", defaultValue = "false")
    protected boolean skipDeploy;

    @Override
    protected boolean shouldSkip() {
        return super.shouldSkip() || skipDeploy;
    }
}
