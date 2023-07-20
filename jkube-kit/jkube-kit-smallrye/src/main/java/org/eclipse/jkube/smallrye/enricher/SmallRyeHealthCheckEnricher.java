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
package org.eclipse.jkube.smallrye.enricher;

import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.eclipse.jkube.microprofile.enricher.AbstractMicroprofileHealthCheckEnricher;

import static org.eclipse.jkube.smallrye.SmallRyeUtils.hasSmallRyeDependency;

public class SmallRyeHealthCheckEnricher extends AbstractMicroprofileHealthCheckEnricher {
  public SmallRyeHealthCheckEnricher(JKubeEnricherContext buildContext) {
    super(buildContext, "jkube-healthcheck-smallrye");
  }

  @Override
  public boolean shouldAddProbe() {
    return hasSmallRyeDependency(getContext().getProject());
  }
}
