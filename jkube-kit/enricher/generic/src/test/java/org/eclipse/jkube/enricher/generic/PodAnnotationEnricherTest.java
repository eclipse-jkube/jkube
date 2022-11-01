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

import io.fabric8.kubernetes.api.builder.VisitableBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.ReplicaSet;
import io.fabric8.kubernetes.api.model.apps.ReplicaSetBuilder;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.api.model.DeploymentConfigBuilder;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.enricher.api.EnricherContext;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Properties;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class PodAnnotationEnricherTest {
  private KubernetesListBuilder klb;
  private PodAnnotationEnricher podAnnotationEnricher;

  @BeforeEach
  void setUp() {
    Properties properties = new Properties();
    klb = new KubernetesListBuilder();
    EnricherContext context = JKubeEnricherContext.builder()
        .project(JavaProject.builder()
            .properties(properties)
            .build())
        .build();
    podAnnotationEnricher = new PodAnnotationEnricher(context);
  }

  @DisplayName("enrich resource")
  @ParameterizedTest(name = "with ''{0}'', should add annotations to pod template spec")
  @MethodSource("data")
  void enrich(String controllerKind, VisitableBuilder<? extends HasMetadata, ?> item, Class<? extends KubernetesListBuilder> clazz) {
    // Given
    klb.addToItems(item);
    // When
    podAnnotationEnricher.enrich(PlatformMode.kubernetes, klb);
    // Then
    assertThat(klb.build().getItems())
        .singleElement(InstanceOfAssertFactories.type(clazz))
        .extracting("spec.template.metadata.annotations")
        .hasFieldOrPropertyWithValue("key1", "value1")
        .hasFieldOrPropertyWithValue("key2", "value2");
  }

  static Stream<Arguments> data() {
    return Stream.of(
        arguments("Deployment", new DeploymentBuilder()
            .withMetadata(createResourceMetadata())
            .withNewSpec()
            .withNewTemplate().withMetadata(createPodTemplateSpecMetadata()).endTemplate()
            .endSpec(), Deployment.class),
        arguments("DeploymentConfig", new DeploymentConfigBuilder()
            .withMetadata(createResourceMetadata())
            .withNewSpec()
            .withNewTemplate().withMetadata(createPodTemplateSpecMetadata()).endTemplate()
            .endSpec(), DeploymentConfig.class),
        arguments("ReplicaSet", new ReplicaSetBuilder()
            .withMetadata(createResourceMetadata())
            .withNewSpec()
            .withNewTemplate().withMetadata(createPodTemplateSpecMetadata()).endTemplate()
            .endSpec(), ReplicaSet.class)
    );
  }

  private static ObjectMeta createResourceMetadata() {
    return new ObjectMetaBuilder()
        .addToAnnotations("key1", "value1")
        .build();
  }

  private static ObjectMeta createPodTemplateSpecMetadata() {
    return new ObjectMetaBuilder()
        .addToAnnotations("key2", "value2")
        .build();
  }
}
