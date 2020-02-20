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

import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import org.eclipse.jkube.kit.common.util.Base64Util;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.maven.enricher.api.JkubeEnricherContext;
import org.eclipse.jkube.maven.enricher.api.model.Configuration;
import mockit.Expectations;
import mockit.Mocked;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class FileDataSecretEnricherTest {

    private static final String TEST_APPLICATION_PROPERTIES_PATH = "src/test/resources/test-application.properties";
    private static final String TEST_APPLICATION_PROPERTIES = "test-application.properties";
    @Mocked
    private JkubeEnricherContext context;

    @Test
    public void shouldMaterializeFileContentFromAnnotation() throws IOException {

        // Given

        new Expectations() {
            {{
                context.getConfiguration();
                result = new Configuration.Builder()
                        .resource(new ResourceConfig())
                        .build();
            }}

        };

        final FileDataSecretEnricher fileDataSecretEnricher =
                new FileDataSecretEnricher(context);
        final KubernetesListBuilder builder = new KubernetesListBuilder();
        builder.addToSecretItems(createBaseSecret());

        // When
        fileDataSecretEnricher.create(PlatformMode.kubernetes,builder);

        // Then
        final Secret secret = (Secret) builder.buildFirstItem();

        final Map<String, String> data = secret.getData();
        assertThat(data)
                .containsKey(TEST_APPLICATION_PROPERTIES);

        assertThat(data.get(TEST_APPLICATION_PROPERTIES))
                .isEqualTo(Base64Util
                        .encodeToString(Files.readAllBytes(Paths.get(TEST_APPLICATION_PROPERTIES_PATH))));

        final Map<String, String> annotations = secret.getMetadata().getAnnotations();
        assertThat(annotations)
                .isEmpty();
    }

    private Secret createBaseSecret() {
        ObjectMetaBuilder metaBuilder = new ObjectMetaBuilder()
                .withNamespace("default");

        Map<String, String> annotations = new HashMap<>();
        annotations.put(FileDataSecretEnricher.PREFIX_ANNOTATION + TEST_APPLICATION_PROPERTIES,
                TEST_APPLICATION_PROPERTIES_PATH);
        metaBuilder = metaBuilder.withAnnotations(annotations);

        Map<String, String> data = new HashMap<>();
        return new SecretBuilder()
                .withData(data)
                .withMetadata(metaBuilder.build())
                .build();
    }
}

