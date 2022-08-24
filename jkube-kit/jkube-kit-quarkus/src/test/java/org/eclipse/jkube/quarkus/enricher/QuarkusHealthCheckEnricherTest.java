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
package org.eclipse.jkube.quarkus.enricher;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpec;
import mockit.Expectations;
import mockit.Mocked;
import org.assertj.core.api.AbstractListAssert;
import org.assertj.core.api.ObjectAssert;
import org.assertj.core.groups.Tuple;
import org.eclipse.jkube.kit.common.Dependency;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class QuarkusHealthCheckEnricherTest {

  @Mocked
  private JKubeEnricherContext context;

  @Mocked
  private JavaProject javaProject;

  private Properties properties;
  private ProcessorConfig processorConfig;
  private KubernetesListBuilder klb;

  @BeforeEach
  void setUp() {
    properties = new Properties();
    processorConfig = new ProcessorConfig();
    klb = new KubernetesListBuilder();
    // @formatter:off
    klb.addToItems(new DeploymentBuilder()
        .editOrNewSpec()
          .editOrNewTemplate()
            .editOrNewMetadata()
              .withName("template-name")
            .endMetadata()
            .editOrNewSpec()
              .addNewContainer()
                .withImage("container/image")
              .endContainer()
            .endSpec()
          .endTemplate()
        .endSpec()
        .build());
    new Expectations() {{
      context.getProperties(); result = properties;
      context.getConfiguration().getProcessorConfig(); result = processorConfig;
      javaProject.getProperties(); result = properties; minTimes = 0;
      javaProject.getOutputDirectory(); result = new File("/tmp/ignored"); minTimes = 0;
    }};
    // @formatter:on
  }

  @Test
  void create_withCustomPath_shouldReturnCustomPath() {
    // Given
    withSmallryeDependency();
    properties.put("jkube.enricher.jkube-healthcheck-quarkus.path", "/my-custom-path");
    // When
    new QuarkusHealthCheckEnricher(context).create(PlatformMode.kubernetes, klb);
    // Then
    assertLivenessReadinessStartupProbes(klb, tuple("HTTP", "/my-custom-path/live", "HTTP", "/my-custom-path/ready", null, null));
  }

  @Test
  void create_withDefaultsAndQuarkus1_shouldReturnDefaults() {
    // Given
    withSmallryeDependency();
    withQuarkus("1.9.0.CR1");
    // When
    new QuarkusHealthCheckEnricher(context).create(PlatformMode.kubernetes, klb);
    // Then
    assertLivenessReadinessStartupProbes(klb, tuple("HTTP", "/health/live", "HTTP", "/health/ready", null, null));
  }

  @Test
  void create_withQuarkus1_0AndAbsoluteHealthPathProperty_shouldIgnoreLeadingSlash() {
    // Given
    withSmallryeDependency();
    withQuarkus("1.1.0.Final");
    properties.put("quarkus.http.non-application-root-path", "/not-ignored");
    properties.put("quarkus.smallrye-health.root-path", "/absolute");
    // When
    new QuarkusHealthCheckEnricher(context).create(PlatformMode.kubernetes, klb);
    // Then
    assertLivenessReadinessStartupProbes(klb, tuple("HTTP", "/not-ignored/absolute/live", "HTTP", "/not-ignored/absolute/ready", null, null));
  }

  @ParameterizedTest(name = "With  ''{1}''  path property should return customized paths")
  @MethodSource("create_withQuarkus1TestData")
  void create_withQuarkus1AndCustomizedHealthProperties(String healthPath, String healthPathValue, Tuple expected) {
    // Given
    withSmallryeDependency();
    withQuarkus("1.0.0.Final");
    properties.put(healthPath, healthPathValue);
    // When
    new QuarkusHealthCheckEnricher(context).create(PlatformMode.kubernetes, klb);
    // Then
    assertLivenessReadinessStartupProbes(klb, expected);
  }

  public static Stream<Arguments> create_withQuarkus1TestData() {
    return Stream.of(
            Arguments.of("quarkus.smallrye-health.liveness-path", "/absolute/liveness", tuple("HTTP", "/health/absolute/liveness", "HTTP", "/health/ready", null, null)),
            Arguments.of("quarkus.smallrye-health.readiness-path", "/absolute/readiness", tuple("HTTP", "/health/live", "HTTP", "/health/absolute/readiness", null, null)),
            Arguments.of("quarkus.smallrye-health.startup-path", "/absolute/startup", tuple("HTTP", "/health/live", "HTTP", "/health/ready", null, null))
    );
  }

  @Test
  void create_withQuarkus2AndDefaults_shouldReturnPrefixedDefaults() {
    // Given
    withSmallryeDependency();
    withQuarkus("2.0.1.1.Final");
    // When
    new QuarkusHealthCheckEnricher(context).create(PlatformMode.kubernetes, klb);
    // Then
    assertLivenessReadinessStartupProbes(klb, tuple("HTTP", "/q/health/live", "HTTP", "/q/health/ready", null, null));
  }

  @Test
  void create_withQuarkus2AndAbsoluteHealthPathProperty_shouldReturnCustomizedPaths() {
    // Given
    withSmallryeDependency();
    withQuarkus("2.0.0.Final");
    properties.put("quarkus.http.root-path", "/ignored-for-health");
    properties.put("quarkus.http.non-application-root-path", "/ignored-for-health");
    properties.put("quarkus.smallrye-health.root-path", "/absolute/health");
    // When
    new QuarkusHealthCheckEnricher(context).create(PlatformMode.kubernetes, klb);
    // Then
    assertLivenessReadinessStartupProbes(klb, tuple("HTTP", "/absolute/health/live", "HTTP", "/absolute/health/ready", null, null));
  }

  @Test
  void create_withQuarkus2AndApplicationProperties_shouldReturnCustomizedPaths() {
    // Given
    withSmallryeDependency();
    withQuarkus("2.0.1.Final");
    properties.put("quarkus.http.root-path", "/");
    properties.put("quarkus.http.non-application-root-path", "not-app");
    properties.put("quarkus.smallrye-health.root-path", "health");
    properties.put("quarkus.smallrye-health.readiness-path", "im-ready");
    properties.put("quarkus.smallrye-health.liveness-path", "im-alive");
    properties.put("quarkus.smallrye-health.startup-path", "im-started");
    // When
    new QuarkusHealthCheckEnricher(context).create(PlatformMode.kubernetes, klb);
    // Then
    assertLivenessReadinessStartupProbes(klb, tuple("HTTP", "/not-app/health/im-alive", "HTTP", "/not-app/health/im-ready", null, null));
  }

  @Test
  void create_withQuarkus2AndCustomRootProperty_shouldReturnCustomizedPaths() {
    // Given
    withSmallryeDependency();
    withQuarkus("2.0.0.Final");
    properties.put("quarkus.http.root-path", "/root");
    properties.put("quarkus.http.non-application-root-path", "q");
    properties.put("quarkus.smallrye-health.root-path", "health");
    properties.put("quarkus.smallrye-health.liveness-path", "liveness");
    // When
    new QuarkusHealthCheckEnricher(context).create(PlatformMode.kubernetes, klb);
    // Then
    assertLivenessReadinessStartupProbes(klb, tuple("HTTP", "/root/q/health/liveness", "HTTP", "/root/q/health/ready", null, null));
  }

  @Test
  void create_withQuarkus2AndAbsoluteNonApplicationRootPathProperty_shouldReturnCustomizedPaths() {
    // Given
    withSmallryeDependency();
    withQuarkus("2.1.3.7.Final");
    properties.put("quarkus.http.root-path", "/ignored-for-health");
    properties.put("quarkus.http.non-application-root-path", "/absolute");
    properties.put("quarkus.smallrye-health.root-path", "health");
    properties.put("quarkus.smallrye-health.liveness-path", "liveness");
    // When
    new QuarkusHealthCheckEnricher(context).create(PlatformMode.kubernetes, klb);
    // Then
    assertLivenessReadinessStartupProbes(klb, tuple("HTTP", "/absolute/health/liveness", "HTTP", "/absolute/health/ready", "HTTP", "/absolute/health/started"));
  }

  @ParameterizedTest(name = "With  ''{1}''  path property should return customized paths")
  @MethodSource("create_withQuarkus3TestData")
  void create_withQuarkus3AndCustomizedHealthProperties(String healthPathProp, String pathValue, Tuple expected) {
    // Given
    withSmallryeDependency();
    withQuarkus("3.1337.3.Final");
    properties.put(healthPathProp, pathValue);
    // When
    new QuarkusHealthCheckEnricher(context).create(PlatformMode.kubernetes, klb);
    // Then
    assertLivenessReadinessStartupProbes(klb, expected);
  }

  public static Stream<Arguments> create_withQuarkus3TestData() {
    return Stream.of(
            Arguments.of("quarkus.smallrye-health.liveness-path", "/absolute/liveness", tuple("HTTP", "/absolute/liveness", "HTTP", "/q/health/ready", "HTTP", "/q/health/started")),
            Arguments.of("quarkus.smallrye-health.readiness-path", "/absolute/readiness", tuple("HTTP", "/q/health/live", "HTTP", "/absolute/readiness", "HTTP", "/q/health/started")),
            Arguments.of("quarkus.smallrye-health.startup-path", "/absolute/startup", tuple("HTTP", "/q/health/live", "HTTP", "/q/health/ready", "HTTP", "/absolute/startup"))
    );
  }

  @Test
  void create_withNoSmallrye_shouldNotAddProbes() {
    // When
    new QuarkusHealthCheckEnricher(context).create(PlatformMode.kubernetes, klb);
    // Then
    assertContainers(klb)
        .extracting(
            "livenessProbe", "readinessProbe", "startupProbe")
        .containsExactly(tuple(null, null, null));
  }

  private AbstractListAssert<?, List<? extends Container>, Container, ObjectAssert<Container>> assertContainers(
      KubernetesListBuilder kubernetesListBuilder) {
    return assertThat(kubernetesListBuilder.build().getItems())
        .hasSize(1)
        .extracting("spec", DeploymentSpec.class)
        .extracting("template", PodTemplateSpec.class)
        .extracting("spec", PodSpec.class)
        .flatExtracting(PodSpec::getContainers);
  }

  private void assertLivenessReadinessStartupProbes(KubernetesListBuilder kubernetesListBuilder, Tuple... values) {
    assertContainers(kubernetesListBuilder)
            .extracting(
                    "livenessProbe.httpGet.scheme", "livenessProbe.httpGet.path",
                    "readinessProbe.httpGet.scheme", "readinessProbe.httpGet.path",
                    "startupProbe.httpGet.scheme", "startupProbe.httpGet.path")
            .containsExactly(values);
  }

  private void withQuarkus(String version) {
    // @formatter:off
    new Expectations() {{
      javaProject.getDependencies();
      result = Collections.singletonList(Dependency.builder()
              .groupId("io.quarkus")
              .artifactId("quarkus-universe-bom")
              .version(version)
              .build());
    }};
    // @formatter:on
  }

  private void withSmallryeDependency() {
    // @formatter:off
    new Expectations() {{
      context.hasDependency("io.quarkus", "quarkus-smallrye-health"); result = true;
    }};
    // @formatter:on
  }

}
