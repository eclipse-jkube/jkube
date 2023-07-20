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
package org.eclipse.jkube.microprofile.enricher;

import io.fabric8.kubernetes.api.model.Probe;
import io.fabric8.kubernetes.api.model.ProbeBuilder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.eclipse.jkube.kit.common.Configs;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.eclipse.jkube.kit.enricher.specific.AbstractHealthCheckEnricher;

import java.util.Optional;

import static org.eclipse.jkube.kit.common.Configs.asInteger;
import static org.eclipse.jkube.microprofile.MicroprofileHealthUtil.DEFAULT_LIVENESS_PATH;
import static org.eclipse.jkube.microprofile.MicroprofileHealthUtil.DEFAULT_READINESS_PATH;
import static org.eclipse.jkube.microprofile.MicroprofileHealthUtil.DEFAULT_STARTUP_PATH;
import static org.eclipse.jkube.microprofile.MicroprofileHealthUtil.hasMicroProfileDependency;
import static org.eclipse.jkube.microprofile.MicroprofileHealthUtil.isStartupEndpointSupported;

public abstract class AbstractMicroprofileHealthCheckEnricher extends AbstractHealthCheckEnricher {
  private static final String DEFAULT_PORT = "8080";

  protected AbstractMicroprofileHealthCheckEnricher(JKubeEnricherContext buildContext, String name) {
    super(buildContext, name);
  }

  @AllArgsConstructor
  private enum Config implements Configs.Config {

    SCHEME("scheme", "HTTP"),
    PORT("port", null),
    LIVENESS_FAILURE_THRESHOLD("livenessFailureThreshold", "3"),
    LIVENESS_SUCCESS_THRESHOLD("livenessSuccessThreshold", "1"),
    LIVENESS_INITIAL_DELAY("livenessInitialDelay", "0"),
    LIVENESS_PERIOD_SECONDS("livenessPeriodSeconds", "10"),
    LIVENESS_PATH("livenessPath", DEFAULT_LIVENESS_PATH),
    READINESS_FAILURE_THRESHOLD("readinessFailureThreshold", "3"),
    READINESS_SUCCESS_THRESHOLD("readinessSuccessThreshold", "1"),
    READINESS_INITIAL_DELAY("readinessInitialDelay", "0"),
    READINESS_PERIOD_SECONDS("readinessPeriodSeconds", "10"),
    READINESS_PATH("readinessPath", DEFAULT_READINESS_PATH),
    STARTUP_FAILURE_THRESHOLD("startupFailureThreshold", "3"),
    STARTUP_SUCCESS_THRESHOLD("startupSuccessThreshold", "1"),
    STARTUP_INITIAL_DELAY("startupInitialDelay", "0"),
    STARTUP_PERIOD_SECONDS("startupPeriodSeconds", "10"),
    STARTUP_PATH("startupPath", DEFAULT_STARTUP_PATH);

    @Getter
    protected String key;
    @Getter
    protected String defaultValue;
  }

  @Override
  protected Probe getReadinessProbe() {
    return discoverAbstractMicroprofileHealthCheck(getConfig(Config.READINESS_PATH), getConfigAsInt(Config.READINESS_INITIAL_DELAY), getConfigAsInt(Config.READINESS_FAILURE_THRESHOLD), getConfigAsInt(Config.READINESS_SUCCESS_THRESHOLD), getConfigAsInt(Config.READINESS_PERIOD_SECONDS));
  }

  @Override
  protected Probe getLivenessProbe() {
    return discoverAbstractMicroprofileHealthCheck(getConfig(Config.LIVENESS_PATH), getConfigAsInt(Config.LIVENESS_INITIAL_DELAY), getConfigAsInt(Config.LIVENESS_FAILURE_THRESHOLD), getConfigAsInt(Config.LIVENESS_SUCCESS_THRESHOLD), getConfigAsInt(Config.LIVENESS_PERIOD_SECONDS));
  }

  @Override
  protected Probe getStartupProbe() {
    if (isStartupEndpointSupported(getContext().getProject())) {
      return discoverAbstractMicroprofileHealthCheck(getConfig(Config.STARTUP_PATH), getConfigAsInt(Config.STARTUP_INITIAL_DELAY), getConfigAsInt(Config.STARTUP_FAILURE_THRESHOLD), getConfigAsInt(Config.STARTUP_SUCCESS_THRESHOLD), getConfigAsInt(Config.STARTUP_PERIOD_SECONDS));
    }
    return null;
  }

  protected boolean shouldAddProbe() {
    return hasMicroProfileDependency(getContext().getProject());
  }

  protected int getPort() {
    return asInteger(Optional.ofNullable(getPortFromConfiguration()).orElse(DEFAULT_PORT));
  }

  protected String getPortFromConfiguration() {
    return getConfig(Config.PORT);
  }

  private int getConfigAsInt(Configs.Config key) {
    return Integer.parseInt(getConfig(key));
  }

  private Probe discoverAbstractMicroprofileHealthCheck(String path, int initialDelay, int failureThreshold, int successThreshold, int periodSeconds) {
    if (shouldAddProbe()) {
      return new ProbeBuilder()
          .withNewHttpGet()
          .withNewPort(getPort())
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
}
