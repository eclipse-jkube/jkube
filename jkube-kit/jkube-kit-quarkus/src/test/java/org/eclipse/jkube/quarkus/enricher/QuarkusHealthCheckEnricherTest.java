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

import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpec;
import mockit.Expectations;
import mockit.Mocked;
import org.assertj.core.groups.Tuple;
import org.eclipse.jkube.kit.common.Dependency;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class QuarkusHealthCheckEnricherTest {

  @Mocked
  private JKubeEnricherContext context;

  @Mocked
  private JavaProject javaProject;

  private Properties properties;
  private ProcessorConfig processorConfig;
  private KubernetesListBuilder klb;

  @Before
  public void setUp() {
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
      context.hasDependency("io.quarkus", "quarkus-smallrye-health"); result = true;
    }};
    // @formatter:on
  }

  @Test
  public void createWithDefaultsInKubernetes() {
    // Given
    setupJavaProjectWithOutputDirectoryQuarkusVersionAndProperties("1.9.0.CR1", new Properties());
    // When
    new QuarkusHealthCheckEnricher(context).create(PlatformMode.kubernetes, klb);
    // Then
    assertLivenessReadinessProbes(klb, tuple("HTTP", "/health/live", "HTTP", "/health/ready"));
  }

  @Test
  public void createWithCustomPathInKubernetes() {
    // Given
    properties.put("jkube.enricher.jkube-healthcheck-quarkus.path", "/my-custom-path");
    setupJavaProjectWithOutputDirectory();
    setupJavaProjectWithProperties(properties);
    // When
    new QuarkusHealthCheckEnricher(context).create(PlatformMode.kubernetes, klb);
    // Then
    assertLivenessReadinessProbes(klb, tuple("HTTP", "/my-custom-path/live", "HTTP", "/my-custom-path/ready"));
  }

  @Test
  public void createWithDefaultQuarkusPost2_0PropertiesInKubernetes() {
    // Given
    Properties properties = createHttpRootPathPropertiesWithValue("/", "q", "health", "liveness");
    setupJavaProjectWithOutputDirectoryQuarkusVersionAndProperties("2.0.1.Final", properties);

    // When
    new QuarkusHealthCheckEnricher(context).create(PlatformMode.kubernetes, klb);

    // Then
    assertLivenessReadinessProbes(klb, tuple("HTTP", "/q/health/liveness", "HTTP", "/q/health/ready"));
  }

  @Test
  public void createWithQuarkusPost2_0ChangedHttpRootPathInKubernetes() {
    // Given
    Properties properties = createHttpRootPathPropertiesWithValue("/root", "q", "health", "liveness");
    setupJavaProjectWithOutputDirectoryQuarkusVersionAndProperties("2.0.1.Final", properties);

    // When
    new QuarkusHealthCheckEnricher(context).create(PlatformMode.kubernetes, klb);

    // Then
    assertLivenessReadinessProbes(klb, tuple("HTTP", "/root/q/health/liveness", "HTTP", "/root/q/health/ready"));
  }

  @Test
  public void createWithQuarkusPost2_0AbsoluteNonApplicationRootPathInKubernetes() {
    // Given
    Properties properties = createHttpRootPathPropertiesWithValue("/root", "/q", "health", "liveness");
    setupJavaProjectWithOutputDirectoryQuarkusVersionAndProperties("2.0.1.Final", properties);

    // When
    new QuarkusHealthCheckEnricher(context).create(PlatformMode.kubernetes, klb);

    // Then
    assertLivenessReadinessProbes(klb, tuple("HTTP", "/q/health/liveness", "HTTP", "/q/health/ready"));
  }

  @Test
  public void createWithQuarkusPost2_0AbsoluteNonApplicationRootPathAndLivenessInKubernetes() {
    // Given
    Properties properties = createHttpRootPathPropertiesWithValue("/root", "/q", "health", "/liveness");
    setupJavaProjectWithOutputDirectoryQuarkusVersionAndProperties("2.0.0.Final", properties);

    // When
    new QuarkusHealthCheckEnricher(context).create(PlatformMode.kubernetes, klb);

    // Then
    assertLivenessReadinessProbes(klb, tuple("HTTP", "/liveness", "HTTP", "/q/health/ready"));
  }

  @Test
  public void createWithQuarkusPost2_0AbsoluteHealthPathInKubernetes() {
    // Given
    Properties properties = createHttpRootPathPropertiesWithValue("/root", "q", "/health", "liveness");
    setupJavaProjectWithOutputDirectoryQuarkusVersionAndProperties("2.0.0.Final", properties);

    // When
    new QuarkusHealthCheckEnricher(context).create(PlatformMode.kubernetes, klb);

    // Then
    assertLivenessReadinessProbes(klb, tuple("HTTP", "/health/liveness", "HTTP", "/health/ready"));
  }

  @Test
  public void createWithQuarkusPost2_0NonApplicationRootPathRemovedInKubernetes() {
    // Given
    Properties properties = createHttpRootPathPropertiesWithValue("/root", "/root", "health", "liveness");
    setupJavaProjectWithOutputDirectoryQuarkusVersionAndProperties("2.0.0.Final", properties);

    // When
    new QuarkusHealthCheckEnricher(context).create(PlatformMode.kubernetes, klb);

    // Then
    assertLivenessReadinessProbes(klb, tuple("HTTP", "/root/health/liveness", "HTTP", "/root/health/ready"));
  }

  @Test
  public void createWithQuarkusPre2_0InKubernetes() {
    // Given
    Properties properties = createHttpRootPathPropertiesWithValue("/", null, "/health", "live");
    setupJavaProjectWithOutputDirectoryQuarkusVersionAndProperties("1.10.5.Final", properties);
    // When
    new QuarkusHealthCheckEnricher(context).create(PlatformMode.kubernetes, klb);

    // Then
    assertLivenessReadinessProbes(klb, tuple("HTTP", "/health/live", "HTTP", "/health/ready"));
  }

  @Test
  public void createWithQuarkusPre2_0InKubernetesWithRootPathInProperties() {
    // Given
    Properties properties = createHttpRootPathPropertiesWithValue("/root", null, "/robot", "livenessapp");
    setupJavaProjectWithOutputDirectoryQuarkusVersionAndProperties("1.10.5.Final", properties);
    // When
    new QuarkusHealthCheckEnricher(context).create(PlatformMode.kubernetes, klb);

    // Then
    assertLivenessReadinessProbes(klb, tuple("HTTP", "/root/robot/livenessapp", "HTTP", "/root/robot/ready"));
  }

  @Test
  public void createWithQuarkusPost1_11InKubernetesWithAbsoluteNonApplicationRootPath() {
    // Given
    Properties properties = createHttpRootPathPropertiesWithValue("/root", "/q", "health", "liveness");
    setupJavaProjectWithOutputDirectoryQuarkusVersionAndProperties("1.13.7.Final", properties);
    // When
    new QuarkusHealthCheckEnricher(context).create(PlatformMode.kubernetes, klb);

    // Then
    assertLivenessReadinessProbes(klb, tuple("HTTP", "/q/health/liveness", "HTTP", "/q/health/ready"));
  }

  @Test
  public void createWithNoQuarkusDependency() {
    // Given
    // @formatter:off
    new Expectations() {{
      context.hasDependency("io.quarkus", "quarkus-smallrye-health"); result = false;
    }};
    // @formatter:on
    // When
    new QuarkusHealthCheckEnricher(context).create(PlatformMode.kubernetes, klb);
    // Then
    assertThat(klb.build().getItems())
        .hasSize(1)
        .extracting("spec", DeploymentSpec.class)
        .extracting("template", PodTemplateSpec.class)
        .extracting("spec", PodSpec.class)
        .flatExtracting(PodSpec::getContainers)
        .extracting(
            "livenessProbe", "readinessProbe")
        .containsExactly(tuple(null, null));
  }

  private void assertLivenessReadinessProbes(KubernetesListBuilder kubernetesListBuilder, Tuple... values) {
    assertThat(kubernetesListBuilder.build().getItems())
            .hasSize(1)
            .extracting("spec", DeploymentSpec.class)
            .extracting("template", PodTemplateSpec.class)
            .extracting("spec", PodSpec.class)
            .flatExtracting(PodSpec::getContainers)
            .extracting(
                    "livenessProbe.httpGet.scheme", "livenessProbe.httpGet.path",
                    "readinessProbe.httpGet.scheme", "readinessProbe.httpGet.path")
            .containsExactly(values);
  }

  private Properties createHttpRootPathPropertiesWithValue(String rootPath, String nonApplicationRootPath, String healthRootPath, String livenessPath) {
    Properties properties = new Properties();
    properties.put("quarkus.http.root-path", rootPath);
    if (nonApplicationRootPath != null) {
      properties.put("quarkus.http.non-application-root-path", nonApplicationRootPath);
    }
    properties.put("quarkus.smallrye-health.root-path", healthRootPath);
    properties.put("quarkus.smallrye-health.liveness-path", livenessPath);
    return properties;
  }

  private void setupJavaProjectWithOutputDirectoryQuarkusVersionAndProperties(String version, Properties properties) {
    setupJavaProjectWithOutputDirectory();
    setupJavaProjectWithQuarkusVersion(version);
    setupJavaProjectWithProperties(properties);
  }

  private void setupJavaProjectWithProperties(Properties mockProperties) {
    new Expectations() {{
      javaProject.getProperties();
      result = mockProperties;
    }};
  }

  private void setupJavaProjectWithQuarkusVersion(String quarkusVersion) {
    new Expectations() {{
      javaProject.getDependencies();
      result = Collections.singletonList(Dependency.builder()
              .groupId("io.quarkus")
              .artifactId("quarkus-universe-bom")
              .version(quarkusVersion)
              .build());
    }};
  }

  private void setupJavaProjectWithOutputDirectory() {
    new Expectations() {{
      javaProject.getOutputDirectory().getAbsolutePath();
      result = "/tmp/foo/target/classes";
    }};
  }
}
