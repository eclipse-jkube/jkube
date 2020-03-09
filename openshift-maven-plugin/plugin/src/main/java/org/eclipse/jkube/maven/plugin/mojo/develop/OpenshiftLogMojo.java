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
 * This goal tails the log of the most recent pod for the app that was deployed via <code>oc:deploy</code>
 * <p>
 * To terminate the log hit
 * <code>Ctrl+C</code>
 */
@Mojo(name = "log", requiresDependencyResolution = ResolutionScope.COMPILE, defaultPhase = LifecyclePhase.VALIDATE)
public class OpenshiftLogMojo extends LogMojo {

  @Override
  protected String getLogPrefix() {
    return DEFAULT_LOG_PREFIX;
  }
}
