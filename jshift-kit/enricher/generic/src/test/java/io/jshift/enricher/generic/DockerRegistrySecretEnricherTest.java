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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.jshift.kit.config.resource.PlatformMode;
import io.jshift.maven.enricher.api.MavenEnricherContext;
import io.jshift.maven.enricher.api.model.Configuration;
import io.jshift.maven.enricher.api.util.SecretConstants;
import mockit.Expectations;
import mockit.Mocked;
import org.apache.commons.codec.binary.Base64;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static junit.framework.TestCase.assertEquals;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author yuwzho
 */
public class DockerRegistrySecretEnricherTest {

    @Mocked
    private MavenEnricherContext context;

    private String dockerUrl = "docker.io";
    private String annotation = "maven.jshift.io/dockerServerId";

    private void setupExpectations() {
        new Expectations() {
            {{
                context.getConfiguration();
                result = new Configuration.Builder()
                    .secretConfigLookup(
                        id -> {
                            Map<String, Object> ret = new HashMap<>();
                            ret.put("username", "username");
                            ret.put("password", "password");
                            return Optional.of(ret);
                        })
                    .build();
            }}

        };
    }

    @Test
    public void testDockerRegistry() {
        setupExpectations();
        DockerRegistrySecretEnricher enricher = new DockerRegistrySecretEnricher(context);
        KubernetesListBuilder builder = new KubernetesListBuilder();
        Secret secretEnriched = createBaseSecret(true);
        builder.addToSecretItems(secretEnriched);
        enricher.create(PlatformMode.kubernetes, builder);

        secretEnriched = (Secret) builder.buildItem(0);
        Map<String, String> enrichedData = secretEnriched.getData();
        assertThat(enrichedData.size()).isEqualTo(1);
        String data = enrichedData.get(SecretConstants.DOCKER_DATA_KEY);
        assertThat(data).isNotNull();
        JsonObject auths = (JsonObject) new JsonParser().parse(new String(Base64.decodeBase64(data)));
        assertThat(auths.size()).isEqualTo(1);
        JsonObject auth = auths.getAsJsonObject("docker.io");
        assertThat(auth.size()).isEqualTo(2);

        assertThat(auth.get("username").getAsString()).isEqualTo("username");
        assertThat(auth.get("password").getAsString()).isEqualTo("password");
    }

    @Test
    public void testDockerRegistryWithBadKind() {
        setupExpectations();
        DockerRegistrySecretEnricher enricher = new DockerRegistrySecretEnricher(context);
        KubernetesListBuilder builder = new KubernetesListBuilder();
        Secret secret = createBaseSecret(true);
        secret.setKind("Secrets");
        builder.addToSecretItems(createBaseSecret(true));
        KubernetesList expected = builder.build();

        enricher.create(PlatformMode.kubernetes, builder);
        assertEquals(expected, builder.build());
    }

    @Test
    public void testDockerRegistryWithBadAnnotation() {
        DockerRegistrySecretEnricher enricher = new DockerRegistrySecretEnricher(context);
        setupExpectations();
        KubernetesListBuilder builder = new KubernetesListBuilder();
        Secret secret = createBaseSecret(true);
        secret.getMetadata().getAnnotations().put(annotation, "docker1.io");
        builder.addToSecretItems(createBaseSecret(true));

        KubernetesList expected = builder.build();

        enricher.create(PlatformMode.kubernetes, builder);
        assertEquals(expected, builder.build());
    }

    private Secret createBaseSecret(boolean withAnnotation) {
        ObjectMetaBuilder metaBuilder = new ObjectMetaBuilder()
                .withNamespace("default");

        if (withAnnotation) {
            Map<String, String> annotations = new HashMap<>();
            annotations.put(annotation, dockerUrl);
            metaBuilder = metaBuilder.withAnnotations(annotations);
        }

        Map<String, String> data = new HashMap<>();
        return new SecretBuilder()
            .withData(data)
            .withMetadata(metaBuilder.build())
            .withType(SecretConstants.DOCKER_CONFIG_TYPE)
            .build();
    }
}
