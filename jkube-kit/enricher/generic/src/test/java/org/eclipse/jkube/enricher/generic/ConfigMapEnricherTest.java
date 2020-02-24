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

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import org.eclipse.jkube.kit.config.resource.ConfigMapEntry;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.maven.enricher.api.MavenEnricherContext;
import org.eclipse.jkube.maven.enricher.api.model.Configuration;
import mockit.Expectations;
import mockit.Mocked;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.TestCase.assertNull;
import static org.assertj.core.api.Assertions.assertThat;

public class ConfigMapEnricherTest {

    @Mocked
    private MavenEnricherContext context;

    @Test
    public void should_materialize_file_content_from_annotation() throws Exception {
        final ConfigMap baseConfigMap = createAnnotationConfigMap("test-application.properties", "src/test/resources/test-application.properties");
        final KubernetesListBuilder builder = new KubernetesListBuilder()
                .addToConfigMapItems(baseConfigMap);
        new ConfigMapEnricher(context).create(PlatformMode.kubernetes, builder);

        final ConfigMap configMap = (ConfigMap) builder.buildFirstItem();

        final Map<String, String> data = configMap.getData();
        assertThat(data)
                .containsEntry("test-application.properties", readFileContentsAsString("src/test/resources/test-application.properties"));

        final Map<String, String> annotations = configMap.getMetadata().getAnnotations();
        assertThat(annotations)
                .isEmpty();
    }

    @Test
    public void should_materialize_binary_file_content_from_annotation() throws Exception {
        final ConfigMap baseConfigMap = createAnnotationConfigMap("test.bin", "src/test/resources/test.bin");
        final KubernetesListBuilder builder = new KubernetesListBuilder()
                .addToConfigMapItems(baseConfigMap);
        new ConfigMapEnricher(context).create(PlatformMode.kubernetes, builder);

        final ConfigMap configMap = (ConfigMap) builder.buildFirstItem();

        final Map<String, String> data = configMap.getData();
        assertThat(data)
                .isEmpty();

        final Map<String, String> binaryData = configMap.getBinaryData();
        assertThat(binaryData)
                .containsEntry("test.bin", "wA==");

        final Map<String, String> annotations = configMap.getMetadata().getAnnotations();
        assertThat(annotations)
                .isEmpty();
    }

    @Test
    public void should_materialize_file_content_from_xml() throws Exception {
        final org.eclipse.jkube.kit.config.resource.ConfigMap baseConfigMap = createXmlConfigMap("src/test/resources/test-application.properties");
        final ResourceConfig config = new ResourceConfig.Builder()
                .withConfigMap(baseConfigMap)
                .build();
        new Expectations() {{
            context.getConfiguration();
            result = new Configuration.Builder().resource(config).build();
        }};

        final KubernetesListBuilder builder = new KubernetesListBuilder();
        new ConfigMapEnricher(context).create(PlatformMode.kubernetes, builder);

        final ConfigMap configMap = (ConfigMap) builder.buildFirstItem();

        final Map<String, String> data = configMap.getData();
        assertThat(data)
                .containsEntry("test-application.properties", readFileContentsAsString("src/test/resources/test-application.properties"));
    }

    @Test
    public void should_materialize_binary_file_content_from_xml() throws Exception {
        final org.eclipse.jkube.kit.config.resource.ConfigMap baseConfigMap = createXmlConfigMap("src/test/resources/test.bin");
        final ResourceConfig config = new ResourceConfig.Builder()
                .withConfigMap(baseConfigMap)
                .build();
        new Expectations() {{
            context.getConfiguration();
            result = new Configuration.Builder().resource(config).build();
        }};

        final KubernetesListBuilder builder = new KubernetesListBuilder();
        new ConfigMapEnricher(context).create(PlatformMode.kubernetes, builder);
        final ConfigMap configMap = (ConfigMap) builder.buildFirstItem();

        final Map<String, String> data = configMap.getData();
        assertNull(data);

        final Map<String, String> binaryData = configMap.getBinaryData();
        assertThat(binaryData)
                .containsEntry("test.bin", "wA==");
    }

    private org.eclipse.jkube.kit.config.resource.ConfigMap createXmlConfigMap(final String file) {
        final ConfigMapEntry configMapEntry = new ConfigMapEntry();
        configMapEntry.setFile(file);
        final org.eclipse.jkube.kit.config.resource.ConfigMap configMap = new org.eclipse.jkube.kit.config.resource.ConfigMap();
        configMap.addEntry(configMapEntry);
        return configMap;
    }

    private ConfigMap createAnnotationConfigMap(final String key, final String file) {
        ObjectMetaBuilder metaBuilder = new ObjectMetaBuilder()
                .withNamespace("default");

        Map<String, String> annotations = new HashMap<>();
        annotations.put(ConfigMapEnricher.PREFIX_ANNOTATION + key, file);
        metaBuilder = metaBuilder.withAnnotations(annotations);

        Map<String, String> data = new HashMap<>();
        return new ConfigMapBuilder()
                .withData(data)
                .withMetadata(metaBuilder.build())
                .build();
    }

    private String readFileContentsAsString(final String filePath) throws URISyntaxException, IOException {
        return new String(readFileContentAsBytes(filePath));
    }

    private byte[] readFileContentAsBytes(final String filePath) throws IOException, URISyntaxException {
        return Files.readAllBytes(Paths.get(filePath));
    }
}
