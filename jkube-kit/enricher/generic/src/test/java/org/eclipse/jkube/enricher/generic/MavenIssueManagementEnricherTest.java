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
package org.eclipse.jkube.enricher.generic;

import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.config.resource.JKubeAnnotations;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author kameshs
 */
class MavenIssueManagementEnricherTest {
    private JKubeEnricherContext context;
    @BeforeEach
    void setUp() {
        context = mock(JKubeEnricherContext.class,RETURNS_DEEP_STUBS);
    }

    @Test
    void mavenIssueManagementAll() {
      final JavaProject project = JavaProject.builder()
          .issueManagementSystem("GitHub")
          .issueManagementUrl("https://github.com/reactiverse/vertx-maven-plugin/issues/")
          .build();
        // Setup mock behaviour
        when(context.getProject()).thenReturn(project);
        MavenIssueManagementEnricher enricher = new MavenIssueManagementEnricher(context);
        KubernetesListBuilder builder = new KubernetesListBuilder().withItems(new DeploymentBuilder().withNewMetadata().withName("foo").endMetadata().build());
        // When
        enricher.create(PlatformMode.kubernetes, builder);
        // Then
        Map<String, String> scmAnnotations = builder.buildFirstItem().getMetadata().getAnnotations();
        assertThat(scmAnnotations).isNotNull()
            .hasSize(2)
            .containsEntry(JKubeAnnotations.ISSUE_SYSTEM.value(), "GitHub")
            .containsEntry(JKubeAnnotations.ISSUE_TRACKER_URL.value(), "https://github.com/reactiverse/vertx-maven-plugin/issues/");
    }

    @DisplayName("maven issue management")
    @ParameterizedTest(name = "with ''{0}'' issue management system and ''{1}'' issue management url, should be empty")
    @MethodSource("mavenIssueManagementData")
    void create_mavenIssueManagement(String managementSystem, String managementUrl) {
      // Given
      final JavaProject project = JavaProject.builder()
          .issueManagementSystem(managementSystem)
          .issueManagementUrl(managementUrl)
          .build();
      // Setup mock behaviour
      when(context.getProject()).thenReturn(project);
      MavenIssueManagementEnricher enricher = new MavenIssueManagementEnricher(context);
      KubernetesListBuilder builder = new KubernetesListBuilder()
          .withItems(new DeploymentBuilder().withNewMetadata().withName("foo").endMetadata().build());
      // When
      enricher.create(PlatformMode.kubernetes, builder);
      // Then
      Map<String, String> scmAnnotations = builder.buildFirstItem().getMetadata().getAnnotations();
      assertThat(scmAnnotations).isNotNull().isEmpty();
    }

    static Stream<Arguments> mavenIssueManagementData() {
      return Stream.of(
          arguments("GitHub", null),
          arguments(null, "https://github.com/fabric8org.eclipse.jkube-maven-plugin/issues/"),
          arguments(null, null));
    }

}
