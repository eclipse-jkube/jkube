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
package org.eclipse.jkube.openliberty.enricher;

import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.eclipse.jkube.microprofile.enricher.AbstractMicroprofileHealthCheckEnricher;

import java.util.Optional;

import static org.eclipse.jkube.kit.common.Configs.asInteger;
import static org.eclipse.jkube.microprofile.MicroprofileHealthUtil.hasMicroProfileDependency;
import static org.eclipse.jkube.openliberty.OpenLibertyUtils.isMicroProfileHealthEnabled;

public class OpenLibertyHealthCheckEnricher extends AbstractMicroprofileHealthCheckEnricher {
  private static final String DEFAULT_OPENLIBERTY_PORT = "9080";
  public OpenLibertyHealthCheckEnricher(JKubeEnricherContext buildContext) {
    super(buildContext, "jkube-healthcheck-openliberty");
  }

  @Override
  protected boolean shouldAddProbe() {
    return hasMicroProfileDependency(getContext().getProject()) && isMicroProfileHealthEnabled(getContext().getProject());
  }

  @Override
  protected int getPort() {
    return asInteger(Optional.ofNullable(getPortFromConfiguration()).orElse(DEFAULT_OPENLIBERTY_PORT));
  }
}
