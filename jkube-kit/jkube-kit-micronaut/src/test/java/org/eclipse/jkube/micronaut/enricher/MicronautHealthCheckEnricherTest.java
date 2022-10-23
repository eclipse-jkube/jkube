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

import java.io.File;
import java.util.Collections;

import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.Plugin;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpec;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MicronautHealthCheckEnricherTest {

  private JKubeEnricherContext context;
  private JavaProject project;
  private KubernetesListBuilder klb;

  @BeforeEach
  void setUp() {
    project = JavaProject.builder()
            .outputDirectory(new File("target"))
            .build();
    klb = new KubernetesListBuilder();
    klb.addToItems(new ServiceBuilder()
            .withNewMetadata().withName("make-it-real").endMetadata()
            .build());
    context = JKubeEnricherContext.builder()
            .log(new KitLogger.SilentLogger())
            .processorConfig(new ProcessorConfig())
            .project(project)
            .build();
  }

  @Test
  void createWithNoDeployment() {
    // When
    new MicronautHealthCheckEnricher(context).create(PlatformMode.kubernetes, klb);
    // Then
    assertThat(klb.build().getItems())
            .hasSize(1)
            .extracting("kind")
            .containsOnly("Service");
  }

  @Test
  void createWithDeploymentAndNoPlugin() {
    // Given
    klb.addToItems(emptyDeployment());
    // When
    new MicronautHealthCheckEnricher(context).create(PlatformMode.kubernetes, klb);
    // Then
    assertThat(klb.build().getItems())
            .hasSize(2)
            .element(1, InstanceOfAssertFactories.type(Deployment.class))
            .extracting(Deployment::getSpec)
            .extracting(DeploymentSpec::getTemplate)
            .extracting(PodTemplateSpec::getSpec)
            .extracting(PodSpec::getContainers)
            .asList()
            .element(0, InstanceOfAssertFactories.type(Container.class))
            .hasFieldOrPropertyWithValue("livenessProbe", null)
            .hasFieldOrPropertyWithValue("readinessProbe", null);
  }
  @Test
  void createWithDeploymentAndPluginAndNoHealth() {
    // Given
    klb.addToItems(emptyDeployment());
    withMicronautMavenPlugin();
    // When
    new MicronautHealthCheckEnricher(context).create(PlatformMode.kubernetes, klb);
    // Then
    assertThat(klb.build().getItems())
            .hasSize(2)
            .element(1, InstanceOfAssertFactories.type(Deployment.class))
            .extracting(Deployment::getSpec)
            .extracting(DeploymentSpec::getTemplate)
            .extracting(PodTemplateSpec::getSpec)
            .extracting(PodSpec::getContainers)
            .asList()
            .element(0, InstanceOfAssertFactories.type(Container.class))
            .hasFieldOrPropertyWithValue("livenessProbe", null)
            .hasFieldOrPropertyWithValue("readinessProbe", null);
  }

  @Test
  void createWithDeploymentAndPluginAndHealth() throws Exception {
    // Given
    klb.addToItems(emptyDeployment());
    withHealthEnabled();
    withMicronautMavenPlugin();
    // When
    new MicronautHealthCheckEnricher(context).create(PlatformMode.kubernetes, klb);
    // Then
    assertThat(klb.build().getItems())
            .hasSize(2)
            .element(1, InstanceOfAssertFactories.type(Deployment.class))
            .extracting("spec.template.spec.containers")
            .asList()
            .element(0, InstanceOfAssertFactories.type(Container.class))
            .extracting(
                    "livenessProbe.httpGet.path",
                    "readinessProbe.httpGet.path",
                    "readinessProbe.httpGet.port.IntVal"
            )
            .contains("/health", "/health", null);
  }

  @Test
  void createWithDeploymentAndGradlePluginAndHealth() throws Exception {
    // Given
    klb.addToItems(emptyDeployment());
    withHealthEnabled();
    withMicronautGradlePlugin();
    // When
    new MicronautHealthCheckEnricher(context).create(PlatformMode.kubernetes, klb);
    // Then
    assertThat(klb.build().getItems())
            .hasSize(2)
            .element(1, InstanceOfAssertFactories.type(Deployment.class))
            .extracting("spec.template.spec.containers")
            .asList()
            .element(0, InstanceOfAssertFactories.type(Container.class))
            .extracting(
                    "livenessProbe.httpGet.path",
                    "readinessProbe.httpGet.path",
                    "readinessProbe.httpGet.port.IntVal"
            )
            .contains("/health", "/health", null);
  }

  @Test
  void createWithDeploymentAndPluginAndImageConfig() throws Exception {
    // Given
    klb.addToItems(emptyDeployment());
    withHealthEnabled();
    withMicronautMavenPlugin();
    context = context.toBuilder()
            .image(ImageConfiguration.builder().build(BuildConfiguration.builder().port("1337").build()).build())
            .image(ImageConfiguration.builder().build(BuildConfiguration.builder().port("8082").build()).build())
            .build();
    // When
    new MicronautHealthCheckEnricher(context).create(PlatformMode.kubernetes, klb);
    // Then
    assertThat(klb.build().getItems())
            .hasSize(2)
            .element(1, InstanceOfAssertFactories.type(Deployment.class))
            .extracting("spec.template.spec.containers")
            .asList()
            .element(0, InstanceOfAssertFactories.type(Container.class))
            .extracting(
                    "livenessProbe.httpGet.path",
                    "readinessProbe.httpGet.path",
                    "readinessProbe.httpGet.port.IntVal"
            )
            .contains("/health", "/health", 1337);
  }

  @SuppressWarnings({"ConstantConditions"})
  private void withHealthEnabled() throws Exception {
    project.setCompileClassPathElements(Collections.singletonList(
            MicronautHealthCheckEnricherTest.class.getResource("/health-check-enricher-test").toURI().getPath()
    ));
  }

  private void withMicronautMavenPlugin() {
    context.getProject().setPlugins(Collections.singletonList(
            Plugin.builder().groupId("io.micronaut.build").artifactId("micronaut-maven-plugin").build()));
  }

  private void withMicronautGradlePlugin() {
    context.getProject().setPlugins(Collections.singletonList(
            Plugin.builder().groupId("io.micronaut.application").artifactId("io.micronaut.application.gradle.plugin").build()));
  }

  private static DeploymentBuilder emptyDeployment() {
    return new DeploymentBuilder()
            .editOrNewSpec()
            .editOrNewTemplate()
            .editOrNewSpec()
            .addNewContainer()
            .endContainer()
            .endSpec()
            .endTemplate()
            .endSpec();
  }

}