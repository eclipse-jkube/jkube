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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jkube.kit.config.resource.ConfigMapEntry;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.eclipse.jkube.kit.enricher.api.model.Configuration;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConfigMapEnricherTest {
    private JKubeEnricherContext context;

    @BeforeEach
    void setUpExpectations() {
        context = mock(JKubeEnricherContext.class, RETURNS_DEEP_STUBS);
    }

    @Test
    void should_materialize_file_content_from_deprecated_annotation() {
        final ConfigMap baseConfigMap = createAnnotationConfigMap("test-application.properties",
                "src/test/resources/test-application.properties", "maven.jkube.io/cm/");
        final KubernetesListBuilder builder = new KubernetesListBuilder().addToConfigMapItems(baseConfigMap);
        new ConfigMapEnricher(context).create(PlatformMode.kubernetes, builder);

        final ConfigMap configMap = (ConfigMap) builder.buildFirstItem();
        assertThat(configMap)
            .satisfies(m -> assertThat(m.getData())
                .containsEntry("test-application.properties", readFileContentsAsString("src/test/resources/test-application.properties"))
            )
            .satisfies(m -> assertThat(m.getMetadata().getAnnotations()).isEmpty());
    }

    @Test
    void should_materialize_dir_content_from_deprecated_annotation() {
        final ConfigMap baseConfigMap = createAnnotationConfigMap("test-dir", "src/test/resources/test-dir", "maven.jkube.io/cm/");
        final KubernetesListBuilder builder = new KubernetesListBuilder().addToConfigMapItems(baseConfigMap);
        new ConfigMapEnricher(context).create(PlatformMode.kubernetes, builder);

        final ConfigMap configMap = (ConfigMap) builder.buildFirstItem();
        assertThat(configMap)
            .satisfies(m -> assertThat(m.getData())
                .containsEntry("test-application.properties", readFileContentsAsString("src/test/resources/test-dir/test-application.properties"))
                .doesNotContainKey("test-dir-empty")
            )
            .satisfies(m -> assertThat(m.getBinaryData())
                .containsEntry("test.bin", "wA==")
            )
            .satisfies(m -> assertThat(m.getMetadata().getAnnotations()).isEmpty());
    }

    @Test
    void should_materialize_binary_file_content_from_deprecated_annotation() {
        final ConfigMap baseConfigMap = createAnnotationConfigMap("test.bin", "src/test/resources/test.bin", "maven.jkube.io/cm/");
        final KubernetesListBuilder builder = new KubernetesListBuilder().addToConfigMapItems(baseConfigMap);
        new ConfigMapEnricher(context).create(PlatformMode.kubernetes, builder);

        final ConfigMap configMap = (ConfigMap) builder.buildFirstItem();
        assertThat(configMap)
            .satisfies(m -> assertThat(m.getData()).isEmpty())
            .satisfies(m -> assertThat(m.getBinaryData())
                .containsEntry("test.bin", "wA==")
            )
            .satisfies(m -> assertThat(m.getMetadata().getAnnotations()).isEmpty());
    }

    @Test
    void should_materialize_file_content_from_annotation() {
        final ConfigMap baseConfigMap = createAnnotationConfigMap("test-application.properties",
            "src/test/resources/test-application.properties", "jkube.eclipse.org/cm/");
        final KubernetesListBuilder builder = new KubernetesListBuilder().addToConfigMapItems(baseConfigMap);
        new ConfigMapEnricher(context).create(PlatformMode.kubernetes, builder);

        final ConfigMap configMap = (ConfigMap) builder.buildFirstItem();
        assertThat(configMap)
            .satisfies(m -> assertThat(m.getData())
                .containsEntry("test-application.properties", readFileContentsAsString("src/test/resources/test-application.properties"))
            )
            .satisfies(m -> assertThat(m.getMetadata().getAnnotations()).isEmpty());
    }

    @Test
    void should_materialize_dir_content_from_annotation() {
        final ConfigMap baseConfigMap = createAnnotationConfigMap("test-dir", "src/test/resources/test-dir", "jkube.eclipse.org/cm/");
        final KubernetesListBuilder builder = new KubernetesListBuilder().addToConfigMapItems(baseConfigMap);
        new ConfigMapEnricher(context).create(PlatformMode.kubernetes, builder);

        final ConfigMap configMap = (ConfigMap) builder.buildFirstItem();
        assertThat(configMap)
            .satisfies(m -> assertThat(m.getData())
                .containsEntry("test-application.properties", readFileContentsAsString("src/test/resources/test-dir/test-application.properties"))
                .doesNotContainKey("test-dir-empty")
            )
            .satisfies(m -> assertThat(m.getBinaryData())
                .containsEntry("test.bin", "wA==")
            )
            .satisfies(m -> assertThat(m.getMetadata().getAnnotations()).isEmpty());
    }

    @Test
    void should_materialize_binary_file_content_from_annotation() {
        final ConfigMap baseConfigMap = createAnnotationConfigMap("test.bin", "src/test/resources/test.bin", "jkube.eclipse.org/cm/");
        final KubernetesListBuilder builder = new KubernetesListBuilder().addToConfigMapItems(baseConfigMap);
        new ConfigMapEnricher(context).create(PlatformMode.kubernetes, builder);

        final ConfigMap configMap = (ConfigMap) builder.buildFirstItem();
        assertThat(configMap)
            .satisfies(m -> assertThat(m.getData()).isEmpty())
            .satisfies(m -> assertThat(m.getBinaryData())
                .containsEntry("test.bin", "wA==")
            )
            .satisfies(m -> assertThat(m.getMetadata().getAnnotations()).isEmpty());
    }

    @Test
    void should_materialize_file_content_from_xml() throws Exception {
        final org.eclipse.jkube.kit.config.resource.ConfigMap baseConfigMap = createXmlConfigMap(
                "src/test/resources/test-application.properties");
        final ResourceConfig config = ResourceConfig.builder().configMap(baseConfigMap).build();
        when(context.getConfiguration()).thenReturn(Configuration.builder().resource(config).build());
        final KubernetesListBuilder builder = new KubernetesListBuilder();
        new ConfigMapEnricher(context).create(PlatformMode.kubernetes, builder);

        final ConfigMap configMap = (ConfigMap) builder.buildFirstItem();

        final Map<String, String> data = configMap.getData();
        assertThat(data).containsEntry("test-application.properties",
                readFileContentsAsString("src/test/resources/test-application.properties"));
    }

    @Test
    void should_materialize_binary_file_content_from_xml() {
        final org.eclipse.jkube.kit.config.resource.ConfigMap baseConfigMap = createXmlConfigMap("src/test/resources/test.bin");
        final ResourceConfig config = ResourceConfig.builder().configMap(baseConfigMap).build();
        when(context.getConfiguration()).thenReturn(Configuration.builder().resource(config).build());
        final KubernetesListBuilder builder = new KubernetesListBuilder();
        new ConfigMapEnricher(context).create(PlatformMode.kubernetes, builder);
        final ConfigMap configMap = (ConfigMap) builder.buildFirstItem();
        assertThat(configMap)
            .satisfies(m -> assertThat(m.getData()).isNullOrEmpty())
            .satisfies(m -> assertThat(m.getBinaryData()).containsEntry("test.bin", "wA=="));
    }

    private org.eclipse.jkube.kit.config.resource.ConfigMap createXmlConfigMap(final String file) {
        final ConfigMapEntry configMapEntry = new ConfigMapEntry();
        configMapEntry.setFile(file);
        final org.eclipse.jkube.kit.config.resource.ConfigMap configMap = new org.eclipse.jkube.kit.config.resource.ConfigMap();
        configMap.addEntry(configMapEntry);
        return configMap;
    }

    private ConfigMap createAnnotationConfigMap(final String key, final String file, final String annotationPrefix) {
        ObjectMetaBuilder metaBuilder = new ObjectMetaBuilder().withName("some-config-map").withNamespace("default");

        Map<String, String> annotations = new HashMap<>();
        annotations.put(annotationPrefix + key, file);
        metaBuilder = metaBuilder.withAnnotations(annotations);

        Map<String, String> data = new HashMap<>();
        return new ConfigMapBuilder().withData(data).withMetadata(metaBuilder.build()).build();
    }

    private String readFileContentsAsString(final String filePath) throws IOException {
        return new String(readFileContentAsBytes(filePath));
    }

    private byte[] readFileContentAsBytes(final String filePath) throws IOException {
        return Files.readAllBytes(Paths.get(filePath));
    }
}
