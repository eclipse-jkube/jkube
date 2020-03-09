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

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import static org.eclipse.jkube.maven.plugin.mojo.Openshift.DEFAULT_LOG_PREFIX;

/**
 * Ensures that the current app has debug enabled, then opens the debug port so that you can debug the latest pod
 * from your IDE
 */
@Mojo(name = "debug", requiresDependencyResolution = ResolutionScope.COMPILE, defaultPhase = LifecyclePhase.PACKAGE)
public class OpenshiftDebugMojo extends DebugMojo {

  @Override
  protected String getLogPrefix() {
    return DEFAULT_LOG_PREFIX;
  }
}