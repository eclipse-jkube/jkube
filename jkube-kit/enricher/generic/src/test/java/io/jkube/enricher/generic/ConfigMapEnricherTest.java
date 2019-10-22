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
package io.jkube.enricher.generic;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.jkube.kit.config.resource.PlatformMode;
import io.jkube.kit.config.resource.ResourceConfig;
import io.jkube.maven.enricher.api.MavenEnricherContext;
import io.jkube.maven.enricher.api.model.Configuration;
import mockit.Expectations;
import mockit.Mocked;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class ConfigMapEnricherTest {

    @Mocked
    private MavenEnricherContext context;

    @Test
    public void should_materialize_file_content_from_annotation() {

        // Given

        new Expectations() {
            {{
                context.getConfiguration();
                result = new Configuration.Builder()
                    .resource(new ResourceConfig())
                    .build();
            }}

        };

        final ConfigMapEnricher configMapEnricher =
            new ConfigMapEnricher(context);
        final KubernetesListBuilder builder = new KubernetesListBuilder();
        builder.addToConfigMapItems(createBaseConfigMap());

        // When
        configMapEnricher.create(PlatformMode.kubernetes, builder);

        // Then
        final ConfigMap configMap = (ConfigMap) builder.buildFirstItem();

        final Map<String, String> data = configMap.getData();
        assertThat(data)
            .containsKey("test-application.properties");

        final Map<String, String> annotations = configMap.getMetadata().getAnnotations();
        assertThat(annotations)
            .isEmpty();
    }

    private ConfigMap createBaseConfigMap() {
        ObjectMetaBuilder metaBuilder = new ObjectMetaBuilder()
            .withNamespace("default");

        Map<String, String> annotations = new HashMap<>();
        annotations.put(ConfigMapEnricher.PREFIX_ANNOTATION + "test-application.properties",
            "src/test/resources/test-application.properties");
        metaBuilder = metaBuilder.withAnnotations(annotations);

        Map<String, String> data = new HashMap<>();
        return new ConfigMapBuilder()
            .withData(data)
            .withMetadata(metaBuilder.build())
            .build();
    }
}
