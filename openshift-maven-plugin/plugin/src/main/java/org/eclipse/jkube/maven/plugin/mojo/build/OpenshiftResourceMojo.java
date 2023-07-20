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
package org.eclipse.jkube.maven.plugin.mojo.build;

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
