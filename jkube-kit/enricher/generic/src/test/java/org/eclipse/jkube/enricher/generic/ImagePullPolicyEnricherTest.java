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

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpec;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.junit.Test;

import java.util.Properties;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

public class ImagePullPolicyEnricherTest {

  @Test
  public void enrich_withJkubeImagePullPolicyProperty_shouldOverrideImagePullPolicy() {
    // Given
    Properties properties = new Properties();
    properties.put("jkube.imagePullPolicy", "Never");
    ImagePullPolicyEnricher imagePullPolicyEnricher = new ImagePullPolicyEnricher(createNewJKubeEnricherContextWithProperties(properties));
    KubernetesListBuilder kubernetesListBuilder = new KubernetesListBuilder();
    kubernetesListBuilder.addToItems(createNewDeployment());

    // When
    imagePullPolicyEnricher.enrich(PlatformMode.kubernetes, kubernetesListBuilder);

    // Then
    assertImagePullPolicy(kubernetesListBuilder, "Never");
  }

  @Test
  public void enrich_withDefaults_shouldNotOverrideImagePullPolicy() {
    // Given
    Properties properties = new Properties();
    ImagePullPolicyEnricher imagePullPolicyEnricher = new ImagePullPolicyEnricher(createNewJKubeEnricherContextWithProperties(properties));
    KubernetesListBuilder kubernetesListBuilder = new KubernetesListBuilder();
    kubernetesListBuilder.addToItems(createNewDeployment());

    // When
    imagePullPolicyEnricher.enrich(PlatformMode.kubernetes, kubernetesListBuilder);

    // Then
    assertImagePullPolicy(kubernetesListBuilder, "IfNotPresent");
  }

  private void assertImagePullPolicy(KubernetesListBuilder kubernetesListBuilder, String expectedImagePullPolicy) {
    assertThat(kubernetesListBuilder.buildItems())
        .hasSize(1)
        .first(InstanceOfAssertFactories.type(Deployment.class))
        .extracting(Deployment::getSpec)
        .extracting(DeploymentSpec::getTemplate)
        .extracting(PodTemplateSpec::getSpec)
        .extracting(PodSpec::getContainers)
        .asList()
        .hasSize(1)
        .first(InstanceOfAssertFactories.type(Container.class))
        .extracting(Container::getImagePullPolicy)
        .isEqualTo(expectedImagePullPolicy);
  }

  private Deployment createNewDeployment() {
    return new DeploymentBuilder()
        .withNewSpec()
        .withNewTemplate()
        .withNewSpec()
        .addNewContainer()
        .withImagePullPolicy("IfNotPresent")
        .endContainer()
        .endSpec()
        .endTemplate()
        .endSpec()
        .build();
  }

  private JKubeEnricherContext createNewJKubeEnricherContextWithProperties(Properties properties) {
    return JKubeEnricherContext.builder()
        .log(new KitLogger.SilentLogger())
        .project(JavaProject.builder()
            .properties(properties)
            .build())
        .build();
  }
}
