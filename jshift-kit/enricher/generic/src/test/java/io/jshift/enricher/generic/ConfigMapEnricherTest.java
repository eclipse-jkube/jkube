/**
 * Copyright 2016 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package io.jshift.enricher.generic;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.jshift.kit.config.resource.PlatformMode;
import io.jshift.kit.config.resource.ResourceConfig;
import io.jshift.maven.enricher.api.MavenEnricherContext;
import io.jshift.maven.enricher.api.model.Configuration;
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
