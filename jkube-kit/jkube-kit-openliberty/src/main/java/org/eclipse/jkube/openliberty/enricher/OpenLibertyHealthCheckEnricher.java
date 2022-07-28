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

import io.fabric8.kubernetes.api.model.Probe;
import io.fabric8.kubernetes.api.model.ProbeBuilder;
import org.eclipse.jkube.kit.common.Configs;
import org.eclipse.jkube.kit.common.util.SummaryUtil;
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

  protected Probe getReadinessProbe() {
    return discoverOpenLibertyHealthCheck(getConfig(Config.READINESS_PATH), getConfigAsInt(Config.READINESS_INITIAL_DELAY), getConfigAsInt(Config.READINESS_FAILURE_THRESHOLD), getConfigAsInt(Config.READINESS_SUCCESS_THRESHOLD), getConfigAsInt(Config.READINESS_PERIOD_SECONDS));
  }

  @Override
  protected Probe getLivenessProbe() {
    return discoverOpenLibertyHealthCheck(getConfig(Config.LIVENESS_PATH), getConfigAsInt(Config.LIVENESS_INITIAL_DELAY), getConfigAsInt(Config.LIVENESS_FAILURE_THRESHOLD), getConfigAsInt(Config.LIVENESS_SUCCESS_THRESHOLD), getConfigAsInt(Config.LIVENESS_PERIOD_SECONDS));
  }

  @Override
  protected Probe getStartupProbe() {
    if (isStartupEndpointSupported(getContext().getProject())) {
      return discoverOpenLibertyHealthCheck(getConfig(Config.STARTUP_PATH), getConfigAsInt(Config.STARTUP_INITIAL_DELAY), getConfigAsInt(Config.STARTUP_FAILURE_THRESHOLD), getConfigAsInt(Config.STARTUP_SUCCESS_THRESHOLD), getConfigAsInt(Config.STARTUP_PERIOD_SECONDS));
    }
    return null;
  }

  private Probe discoverOpenLibertyHealthCheck(String path, int initialDelay, int failureThreshold, int successThreshold, int periodSeconds) {
    if (hasMicroProfileDependency(getContext().getProject()) && isMicroProfileHealthEnabled(getContext().getProject())) {
      SummaryUtil.addToEnrichers(getName());
      return new ProbeBuilder()
          .withNewHttpGet()
          .withNewPort(asInteger(getConfig(Config.PORT)))
          .withPath(path)
          .withScheme(getConfig(Config.SCHEME))
          .endHttpGet()
          .withFailureThreshold(failureThreshold)
          .withSuccessThreshold(successThreshold)
          .withInitialDelaySeconds(initialDelay)
          .withPeriodSeconds(periodSeconds)
          .build();
    }
    return null;
  }

  private int getConfigAsInt(Configs.Config key) {
    return Integer.parseInt(getConfig(key));
  }
}
