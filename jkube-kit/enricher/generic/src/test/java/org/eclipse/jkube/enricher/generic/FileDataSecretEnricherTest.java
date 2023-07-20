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

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jkube.kit.common.util.Base64Util;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;

import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

class FileDataSecretEnricherTest {

    private static final String TEST_APPLICATION_PROPERTIES_PATH = "src/test/resources/test-application.properties";
    private static final String TEST_APPLICATION_PROPERTIES = "test-application.properties";
    private JKubeEnricherContext context;

    @BeforeEach
    void setupExpectations() {
        context = mock(JKubeEnricherContext.class,RETURNS_DEEP_STUBS);
    }

    @Test
    void shouldMaterializeFileContentFrom_deprecatedAnnotation() {
        // Given
        final FileDataSecretEnricher fileDataSecretEnricher =
                new FileDataSecretEnricher(context);
        final KubernetesListBuilder builder = new KubernetesListBuilder();
        builder.addToSecretItems(createBaseSecret("maven.jkube.io/secret/"));

        // When
        fileDataSecretEnricher.create(PlatformMode.kubernetes,builder);

        // Then
        final Secret secret = (Secret) builder.buildFirstItem();
        assertThat(secret)
            .satisfies(s -> assertThat(s.getData())
                .containsEntry(TEST_APPLICATION_PROPERTIES, Base64Util.encodeToString(Files.readAllBytes(Paths.get(TEST_APPLICATION_PROPERTIES_PATH))))
            )
            .satisfies(s -> assertThat(s.getMetadata().getAnnotations()).isEmpty());
    }


    @Test
    void shouldMaterializeFileContentFrom_Annotation() {
        // Given
        final FileDataSecretEnricher fileDataSecretEnricher = new FileDataSecretEnricher(context);
        final KubernetesListBuilder builder = new KubernetesListBuilder();
        builder.addToSecretItems(createBaseSecret("jkube.eclipse.org/secret/"));

        // When
        fileDataSecretEnricher.create(PlatformMode.kubernetes,builder);

        // Then
        final Secret secret = (Secret) builder.buildFirstItem();
        assertThat(secret)
            .satisfies(s -> assertThat(s.getData())
                .containsEntry(TEST_APPLICATION_PROPERTIES, Base64Util.encodeToString(Files.readAllBytes(Paths.get(TEST_APPLICATION_PROPERTIES_PATH))))
            )
            .satisfies(s -> assertThat(s.getMetadata().getAnnotations()).isEmpty());
      }

    private Secret createBaseSecret(String annotationPrefix) {
        ObjectMetaBuilder metaBuilder = new ObjectMetaBuilder()
                .withNamespace("default");

        Map<String, String> annotations = new HashMap<>();
        annotations.put(annotationPrefix + TEST_APPLICATION_PROPERTIES,
                TEST_APPLICATION_PROPERTIES_PATH);
        metaBuilder = metaBuilder.withAnnotations(annotations);

        Map<String, String> data = new HashMap<>();
        return new SecretBuilder()
                .withData(data)
                .withMetadata(metaBuilder.build())
                .build();
    }
}

