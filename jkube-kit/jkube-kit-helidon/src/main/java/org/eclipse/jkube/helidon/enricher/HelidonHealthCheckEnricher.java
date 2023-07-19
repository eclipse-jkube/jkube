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
package org.eclipse.jkube.helidon.enricher;

import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.eclipse.jkube.microprofile.enricher.AbstractMicroprofileHealthCheckEnricher;

import static org.eclipse.jkube.helidon.HelidonUtils.extractPort;
import static org.eclipse.jkube.helidon.HelidonUtils.getHelidonConfiguration;
import static org.eclipse.jkube.helidon.HelidonUtils.hasHelidonHealthDependency;
import static org.eclipse.jkube.kit.common.Configs.asInteger;

public class HelidonHealthCheckEnricher extends AbstractMicroprofileHealthCheckEnricher {
  private static final String DEFAULT_HELIDON_PORT = "8080";
  public HelidonHealthCheckEnricher(JKubeEnricherContext buildContext) {
    super(buildContext, "jkube-healthcheck-helidon");
  }

  @Override
  protected boolean shouldAddProbe() {
    return hasHelidonHealthDependency(getContext().getProject());
  }

  @Override
  protected int getPort() {
    return asInteger(extractPort(getHelidonConfiguration(getContext().getProject()), DEFAULT_HELIDON_PORT));
  }
}