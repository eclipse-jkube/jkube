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

import org.eclipse.jkube.kit.common.JavaProject;
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
import mockit.Expectations;
import mockit.Mocked;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class MicronautHealthCheckEnricherTest {

  @Mocked
  private JKubeEnricherContext context;

  @Mocked
  private MicronautUtils micronautUtils;

  private Properties properties;
  private ProcessorConfig processorConfig;
  private KubernetesListBuilder klb;
  private MicronautHealthCheckEnricher micronautHealthCheckEnricher;

  @Before
  public void setUp() {
    properties = new Properties();
    processorConfig = new ProcessorConfig();
    klb = new KubernetesListBuilder();
    klb.addToItems(new ServiceBuilder()
        .withNewMetadata().withName("make-it-real").endMetadata()
        .build());
    // @formatter:off
    new Expectations() {{
      context.getProperties(); result = properties;
      context.getConfiguration().getProcessorConfig(); result = processorConfig;
    }};
    // @formatter:on
    micronautHealthCheckEnricher = new MicronautHealthCheckEnricher(context);
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
    withMicronautPlugin();
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
    withMicronautPlugin();
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
    withMicronautPlugin();
    // @formatter:off
    new Expectations() {{
      context.getConfiguration().getImages(); result = Arrays.asList(
          ImageConfiguration.builder().build(BuildConfiguration.builder().port("1337").build()).build(),
          ImageConfiguration.builder().build(BuildConfiguration.builder().port("8082").build()).build()
      );
    }};
    // @formatter:on
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

  @SuppressWarnings({"AccessStaticViaInstance", "ConstantConditions"})
  private void withHealthEnabled() {
    // @formatter:off
    new Expectations() {{
      micronautUtils.isHealthEnabled((Properties)any); result = true;
    }};
    // @formatter:on
  }
  private void withMicronautPlugin() {
    // @formatter:off
    new Expectations() {{
      micronautUtils.hasMicronautPlugin((JavaProject) any); result = true;
      context.getProject().getCompileClassPathElements(); result = Collections.emptyList();
      context.getProject().getOutputDirectory().getAbsolutePath(); result = "";
    }};
    // @formatter:on
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
