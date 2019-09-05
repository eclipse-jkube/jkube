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

import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.jshift.kit.common.util.Base64Util;
import io.jshift.kit.config.resource.PlatformMode;
import io.jshift.kit.config.resource.ResourceConfig;
import io.jshift.maven.enricher.api.MavenEnricherContext;
import io.jshift.maven.enricher.api.model.Configuration;
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
    private MavenEnricherContext context;

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

