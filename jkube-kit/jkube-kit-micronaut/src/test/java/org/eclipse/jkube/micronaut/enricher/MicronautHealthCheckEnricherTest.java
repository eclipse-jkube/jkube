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


import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.eclipse.jkube.micronaut.MicronautUtils;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpec;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class MicronautHealthCheckEnricherTest {

  private JKubeEnricherContext context;
  private MockedStatic<MicronautUtils> micronautUtils;
  private Properties properties;
  private ProcessorConfig processorConfig;
  private KubernetesListBuilder klb;
  private MicronautHealthCheckEnricher micronautHealthCheckEnricher;

  @Before
  public void setUp() {
    context = mock(JKubeEnricherContext.class,RETURNS_DEEP_STUBS);
    micronautUtils = Mockito.mockStatic(MicronautUtils.class);
    properties = new Properties();
    processorConfig = new ProcessorConfig();
    klb = new KubernetesListBuilder();
    klb.addToItems(new ServiceBuilder()
        .withNewMetadata().withName("make-it-real").endMetadata()
        .build());
    when(context.getProperties()).thenReturn(properties);
    when(context.getConfiguration().getProcessorConfig()).thenReturn(processorConfig);
    micronautHealthCheckEnricher = new MicronautHealthCheckEnricher(context);
  }
  @After
  public void tearDown() {
    micronautUtils.close();
  }

  @Test
  public void createWithNoDeployment() {
    // When
    micronautHealthCheckEnricher.create(PlatformMode.kubernetes, klb);
    // Then
    assertThat(klb.build().getItems())
        .hasSize(1)
        .extracting("kind")
        .containsOnly("Service");
  }

  @Test
  public void createWithDeploymentAndNoPlugin() {
    // Given
    klb.addToItems(emptyDeployment());
    // When
    micronautHealthCheckEnricher.create(PlatformMode.kubernetes, klb);
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
  public void createWithDeploymentAndPluginAndNoHealth() {
    // Given
    klb.addToItems(emptyDeployment());
    withMicronautMavenPlugin();
    // When
    micronautHealthCheckEnricher.create(PlatformMode.kubernetes, klb);
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
  public void createWithDeploymentAndPluginAndHealth() {
    // Given
    klb.addToItems(emptyDeployment());
    withHealthEnabled();
    withMicronautMavenPlugin();
    // When
    micronautHealthCheckEnricher.create(PlatformMode.kubernetes, klb);
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
  public void createWithDeploymentAndPluginAndImageConfig() {
    // Given
    klb.addToItems(emptyDeployment());
    withHealthEnabled();
    withMicronautMavenPlugin();
    when(context.getConfiguration().getImages()).thenReturn(Arrays.asList(
            ImageConfiguration.builder().build(BuildConfiguration.builder().port("1337").build()).build(),
            ImageConfiguration.builder().build(BuildConfiguration.builder().port("8082").build()).build()
    ));
    // When
    micronautHealthCheckEnricher.create(PlatformMode.kubernetes, klb);
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


  private void withHealthEnabled() {
    micronautUtils.when(() -> MicronautUtils.isHealthEnabled(any())).thenReturn(true);
  }
  private void withMicronautMavenPlugin() {
    when(context.hasPlugin("io.micronaut.build", "micronaut-maven-plugin")).thenReturn(true);
    when(context.getProject().getCompileClassPathElements()).thenReturn(Collections.emptyList());
    when(context.getProject().getOutputDirectory().getAbsolutePath()).thenReturn("");
  }

  private static DeploymentBuilder emptyDeployment() {
    // @formatter:off
    return new DeploymentBuilder()
        .editOrNewSpec()
          .editOrNewTemplate()
            .editOrNewSpec()
              .addNewContainer()
              .endContainer()
            .endSpec()
          .endTemplate()
        .endSpec();
    // @formatter:off
  }

}
