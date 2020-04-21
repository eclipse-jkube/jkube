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

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import static org.eclipse.jkube.maven.plugin.mojo.Openshift.DEFAULT_LOG_PREFIX;

@Mojo(name = "push", defaultPhase = LifecyclePhase.INSTALL, requiresDependencyResolution = ResolutionScope.COMPILE)
public class OpenshiftPushMojo extends PushMojo {
  @Override
  protected String getLogPrefix() {
    return DEFAULT_LOG_PREFIX;
  }

  @Override
  protected boolean canExecute() {
    log.warn("Image is pushed to OpenShift's internal registry during oc:build goal." +
        " Skipping...");
    return false;
  }
}
