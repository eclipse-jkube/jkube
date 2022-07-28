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
package org.eclipse.jkube.micronaut.enricher;

import java.util.Collections;

import org.eclipse.jkube.kit.common.Configs;
import org.eclipse.jkube.kit.common.util.SummaryUtil;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.eclipse.jkube.kit.enricher.specific.AbstractHealthCheckEnricher;

import io.fabric8.kubernetes.api.model.Probe;
import io.fabric8.kubernetes.api.model.ProbeBuilder;
import lombok.AllArgsConstructor;
import lombok.Getter;

import static org.eclipse.jkube.kit.common.Configs.asInteger;
import static org.eclipse.jkube.kit.common.util.JKubeProjectUtil.getClassLoader;
import static org.eclipse.jkube.micronaut.MicronautUtils.getMicronautConfiguration;
import static org.eclipse.jkube.micronaut.MicronautUtils.hasMicronautPlugin;
import static org.eclipse.jkube.micronaut.MicronautUtils.isHealthEnabled;

public class MicronautHealthCheckEnricher extends AbstractHealthCheckEnricher {

  @AllArgsConstructor
  private enum Config implements Configs.Config {
    READINESS_PROBE_INITIAL_DELAY_SECONDS("readinessProbeInitialDelaySeconds", null),
    READINESS_PROBE_PERIOD_SECONDS("readinessProbePeriodSeconds", null),
    LIVENESS_PROBE_INITIAL_DELAY_SECONDS("livenessProbeInitialDelaySeconds", null),
    LIVENESS_PROBE_PERIOD_SECONDS("livenessProbePeriodSeconds", null),
    FAILURE_THRESHOLD("failureThreshold", "3"),
    SUCCESS_THRESHOLD("successThreshold", "1"),
    TIMEOUT_SECONDS("timeoutSeconds", null),
    SCHEME("scheme", "HTTP"),
    PORT("port", null),
    PATH("path", "/health");

    @Getter
    protected String key;
    @Getter
    protected String defaultValue;
  }

  public MicronautHealthCheckEnricher(JKubeEnricherContext buildContext) {
    super(buildContext, "jkube-healthcheck-micronaut");
  }

  @Override
  protected Probe getReadinessProbe() {
    return buildProbe(
        toInteger(Config.READINESS_PROBE_INITIAL_DELAY_SECONDS),
        toInteger(Config.READINESS_PROBE_PERIOD_SECONDS)
    );
  }

  @Override
  protected Probe getLivenessProbe() {
    return buildProbe(
        toInteger(Config.LIVENESS_PROBE_INITIAL_DELAY_SECONDS),
        toInteger(Config.LIVENESS_PROBE_PERIOD_SECONDS)
    );
  }

  private boolean isApplicable() {
    if (!hasMicronautPlugin(getContext().getProject())){
      return false;
    }
    return isHealthEnabled(getMicronautConfiguration(getClassLoader(getContext().getProject())));
  }

  private Probe buildProbe(Integer initialDelaySeconds, Integer periodSeconds){
    if (!isApplicable()) {
      return null;
    }

    SummaryUtil.addToEnrichers(getName());
    final String firstImagePort = getImages().stream().findFirst()
        .map(ImageConfiguration::getBuild).map(BuildConfiguration::getPorts)
        .orElse(Collections.emptyList()).stream()
        .findFirst().orElse(null);
    return new ProbeBuilder()
        .withInitialDelaySeconds(initialDelaySeconds)
        .withPeriodSeconds(periodSeconds)
        .withFailureThreshold(toInteger(Config.FAILURE_THRESHOLD))
        .withSuccessThreshold(toInteger(Config.SUCCESS_THRESHOLD))
        .withTimeoutSeconds(toInteger(Config.TIMEOUT_SECONDS))
        .withNewHttpGet()
        .withScheme(getConfig(Config.SCHEME))
        .withNewPort(asInteger(getConfig(Config.PORT, firstImagePort)))
        .withPath(getConfig(Config.PATH))
        .endHttpGet()
        .build();
  }

  private Integer toInteger(Config config) {
    return asInteger(getConfig(config));
  }
}
