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
package org.eclipse.jkube.springboot.enricher;

import io.fabric8.kubernetes.api.model.Probe;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jkube.springboot.enricher.SpringBootHealthCheckEnricher.REQUIRED_CLASSES_SPRING_BOOT;
import static org.eclipse.jkube.springboot.enricher.SpringBootHealthCheckEnricher.REQUIRED_CLASSES_SPRING_BOOT_4;
import static org.mockito.Mockito.when;


class SpringBoot4HealthCheckEnricherTest extends AbstractSpringBootHealthCheckEnricherTestSupport {

  @Override
  protected String getSpringBootVersion() {
    return "4.0.0";
  }

  @Override
  protected String getActuatorDefaultBasePath() {
    return "/actuator";
  }

  @Test
  @DisplayName("should detect Spring Boot 4 actuator classes and generate probes")
  void whenSpringBoot4Classes_thenProbesGenerated() {
    // Given
    when(context.getProjectClassLoaders().isClassInCompileClasspath(true, REQUIRED_CLASSES_SPRING_BOOT))
      .thenReturn(false);
    when(context.getProjectClassLoaders().isClassInCompileClasspath(true, REQUIRED_CLASSES_SPRING_BOOT_4))
      .thenReturn(true);
    when(context.getProjectClassLoaders().getCompileClassLoader())
      .thenReturn(new URLClassLoader(new URL[0], AbstractSpringBootHealthCheckEnricherTestSupport.class.getClassLoader()));

    SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);

    // When
    Probe readinessProbe = enricher.getReadinessProbe();
    Probe livenessProbe = enricher.getLivenessProbe();

    // Then
    assertThat(readinessProbe).isNotNull();
    assertThat(livenessProbe).isNotNull();
    assertThat(readinessProbe.getHttpGet().getPath()).isEqualTo(getActuatorDefaultBasePath() + "/health");
    assertThat(livenessProbe.getHttpGet().getPath()).isEqualTo(getActuatorDefaultBasePath() + "/health");
  }

  @Test
  @DisplayName("should generate probes with custom config for Spring Boot 4")
  void whenSpringBoot4WithCustomConfig_thenCustomProbesGenerated() {
    // Given
    TreeMap<String, Object> enricherConfig = new TreeMap<>();
    enricherConfig.put("readinessProbeInitialDelaySeconds", "25");
    enricherConfig.put("livenessProbeInitialDelaySeconds", "400");
    enricherConfig.put("timeoutSeconds", "90");
    context.getConfiguration().getProcessorConfig()
      .setConfig(Collections.singletonMap("jkube-healthcheck-spring-boot", enricherConfig));
    when(context.getProjectClassLoaders().isClassInCompileClasspath(true, REQUIRED_CLASSES_SPRING_BOOT))
      .thenReturn(false);
    when(context.getProjectClassLoaders().isClassInCompileClasspath(true, REQUIRED_CLASSES_SPRING_BOOT_4))
      .thenReturn(true);
    when(context.getProjectClassLoaders().getCompileClassLoader())
      .thenReturn(new URLClassLoader(new URL[0], AbstractSpringBootHealthCheckEnricherTestSupport.class.getClassLoader()));

    SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);

    // When
    Probe readinessProbe = enricher.getReadinessProbe();
    Probe livenessProbe = enricher.getLivenessProbe();

    // Then
    assertThat(readinessProbe.getInitialDelaySeconds()).isEqualTo(25);
    assertThat(livenessProbe.getInitialDelaySeconds()).isEqualTo(400);
    assertThat(readinessProbe.getTimeoutSeconds()).isEqualTo(90);
    assertThat(livenessProbe.getTimeoutSeconds()).isEqualTo(90);
  }

  @Test
  @DisplayName("should generate probes with management.health.probes.enabled for Spring Boot 4")
  void whenSpringBoot4WithManagementHealthProbesEnabled_thenProbesWithSuffixGenerated() {
    // Given
    props.put("management.health.probes.enabled", "true");
    writeProps();
    when(context.getProjectClassLoaders().isClassInCompileClasspath(true, REQUIRED_CLASSES_SPRING_BOOT))
      .thenReturn(false);
    when(context.getProjectClassLoaders().isClassInCompileClasspath(true, REQUIRED_CLASSES_SPRING_BOOT_4))
      .thenReturn(true);
    when(context.getProjectClassLoaders().getCompileClassLoader())
      .thenReturn(new URLClassLoader(new URL[0], AbstractSpringBootHealthCheckEnricherTestSupport.class.getClassLoader()));

    SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);

    // When
    Probe livenessProbe = enricher.getLivenessProbe();
    Probe readinessProbe = enricher.getReadinessProbe();

    // Then
    assertThat(livenessProbe.getHttpGet().getPath()).isEqualTo(getActuatorDefaultBasePath() + "/health/liveness");
    assertThat(readinessProbe.getHttpGet().getPath()).isEqualTo(getActuatorDefaultBasePath() + "/health/readiness");
  }

  @Test
  @DisplayName("should work when both Spring Boot 3 and Spring Boot 4 classes are present")
  void whenBothSpringBoot3And4Classes_thenProbesGenerated() {
    // Given
    when(context.getProjectClassLoaders().isClassInCompileClasspath(true, REQUIRED_CLASSES_SPRING_BOOT))
      .thenReturn(true);
    when(context.getProjectClassLoaders().isClassInCompileClasspath(true, REQUIRED_CLASSES_SPRING_BOOT_4))
      .thenReturn(true);
    when(context.getProjectClassLoaders().getCompileClassLoader())
      .thenReturn(new URLClassLoader(new URL[0], AbstractSpringBootHealthCheckEnricherTestSupport.class.getClassLoader()));

    SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);

    // When
    Probe readinessProbe = enricher.getReadinessProbe();
    Probe livenessProbe = enricher.getLivenessProbe();

    // Then
    assertThat(readinessProbe).isNotNull();
    assertThat(livenessProbe).isNotNull();
  }

  @Test
  @DisplayName("should return null when neither Spring Boot 3 nor Spring Boot 4 classes are present")
  void whenNoSpringBootClasses_thenNoProbesGenerated() {
    // Given
    when(context.getProjectClassLoaders().isClassInCompileClasspath(true, REQUIRED_CLASSES_SPRING_BOOT))
      .thenReturn(false);
    when(context.getProjectClassLoaders().isClassInCompileClasspath(true, REQUIRED_CLASSES_SPRING_BOOT_4))
      .thenReturn(false);
    when(context.getProjectClassLoaders().getCompileClassLoader())
      .thenReturn(new URLClassLoader(new URL[0], AbstractSpringBootHealthCheckEnricherTestSupport.class.getClassLoader()));

    SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);

    // When
    Probe readinessProbe = enricher.getReadinessProbe();
    Probe livenessProbe = enricher.getLivenessProbe();

    // Then
    assertThat(readinessProbe).isNull();
    assertThat(livenessProbe).isNull();
  }
}
