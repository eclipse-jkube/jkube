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
package org.eclipse.jkube.enricher.generic;

import java.util.Collections;
import java.util.Properties;

import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerPortBuilder;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import mockit.Expectations;
import mockit.Mocked;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DebugEnricherTest {

  @Mocked
  private JKubeEnricherContext context;

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
    }};
    // @formatter:on
  }

  @Test
  public void debugDisabledShouldDoNothing() {
    // When
    new DebugEnricher(context).create(null, klb);
    // Then
    assertThat(getContainer().getEnv()).isEmpty();
  }

  @Test
  public void debugEnabledInFallbackPropertyShouldEnableDebug() {
    // Given
    properties.put("jkube.debug.enabled", "true");
    // When
    new DebugEnricher(context).create(null, klb);
    // Then
    assertThat(getContainer().getEnv())
        .contains(new EnvVarBuilder().withName("JAVA_ENABLE_DEBUG").withValue("true").build());
    assertThat(getContainer().getPorts())
        .containsExactly(new ContainerPortBuilder().withName("debug").withContainerPort(5005).build());
  }

  @Test
  public void debugEnabledInEnricherPropertyShouldEnableDebug() {
    // Given
    properties.put("jkube.enricher.jkube-debug.enabled", "true");
    // When
    new DebugEnricher(context).create(null, klb);
    // Then
    assertThat(getContainer().getEnv())
        .contains(new EnvVarBuilder().withName("JAVA_ENABLE_DEBUG").withValue("true").build());
    assertThat(getContainer().getPorts())
        .containsExactly(new ContainerPortBuilder().withName("debug").withContainerPort(5005).build());
  }

  @Test
  public void debugEnabledInEnricherConfigShouldEnableDebug() {
    // Given
    processorConfig.getConfig().put("jkube-debug", Collections.singletonMap("enabled", "true"));
    // When
    new DebugEnricher(context).create(null, klb);
    // Then
    assertThat(getContainer().getEnv())
        .contains(new EnvVarBuilder().withName("JAVA_ENABLE_DEBUG").withValue("true").build());
    assertThat(getContainer().getPorts())
        .containsExactly(new ContainerPortBuilder().withName("debug").withContainerPort(5005).build());
  }

  private Container getContainer() {
    return ((Deployment) klb.build().getItems().iterator().next())
        .getSpec().getTemplate().getSpec().getContainers().iterator()
        .next();
  }
}