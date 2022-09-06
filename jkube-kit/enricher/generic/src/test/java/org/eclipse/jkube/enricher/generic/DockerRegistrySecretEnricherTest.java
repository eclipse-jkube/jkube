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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.eclipse.jkube.kit.enricher.api.model.Configuration;
import org.eclipse.jkube.kit.enricher.api.util.SecretConstants;
import org.apache.commons.codec.binary.Base64;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static junit.framework.TestCase.assertEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author yuwzho
 */
public class DockerRegistrySecretEnricherTest {
    private JKubeEnricherContext context;

    private final String ANNOTATION = "jkube.eclipse.org/dockerServerId";

    @Before
    public void setupExpectations() {
        context = mock(JKubeEnricherContext.class,RETURNS_DEEP_STUBS);
        when(context.getConfiguration()).thenReturn(Configuration.builder()
                .secretConfigLookup(
                        id -> {
                            Map<String, Object> ret = new HashMap<>();
                            ret.put("username", "username");
                            ret.put("password", "password");
                            return Optional.of(ret);
                        })
                .build());
        when(context.getLog()).thenReturn(new KitLogger.SilentLogger());
    }

    @Test
    public void testDockerRegistry() {
        DockerRegistrySecretEnricher enricher = new DockerRegistrySecretEnricher(context);
        KubernetesListBuilder builder = new KubernetesListBuilder();
        Secret secretEnriched = createBaseSecret(true, ANNOTATION);
        builder.addToSecretItems(secretEnriched);
        enricher.create(PlatformMode.kubernetes, builder);

        secretEnriched = (Secret) builder.buildItem(0);
        Map<String, String> enrichedData = secretEnriched.getData();
        assertThat(enrichedData).hasSize(1);
        String data = enrichedData.get(SecretConstants.DOCKER_DATA_KEY);
        assertThat(data).isNotNull();
        JsonObject auths = (JsonObject) JsonParser.parseString(new String(Base64.decodeBase64(data)));
        assertThat(auths.size()).isEqualTo(1);
        JsonObject auth = auths.getAsJsonObject("docker.io");
        assertThat(auth.size()).isEqualTo(2);

        assertThat(auth.get("username").getAsString()).isEqualTo("username");
        assertThat(auth.get("password").getAsString()).isEqualTo("password");
    }

    @Test
    public void testDockerRegistryWithDeprecatedAnnotation() {
        DockerRegistrySecretEnricher enricher = new DockerRegistrySecretEnricher(context);
        KubernetesListBuilder builder = new KubernetesListBuilder();
        Secret secretEnriched = createBaseSecret(true, "maven.jkube.io/dockerServerId");
        builder.addToSecretItems(secretEnriched);
        enricher.create(PlatformMode.kubernetes, builder);

        secretEnriched = (Secret) builder.buildItem(0);
        Map<String, String> enrichedData = secretEnriched.getData();
        assertThat(enrichedData).hasSize(1);
        String data = enrichedData.get(SecretConstants.DOCKER_DATA_KEY);
        assertThat(data).isNotNull();
        JsonObject auths = (JsonObject) JsonParser.parseString(new String(Base64.decodeBase64(data)));
        assertThat(auths.size()).isEqualTo(1);
        JsonObject auth = auths.getAsJsonObject("docker.io");
        assertThat(auth.size()).isEqualTo(2);

        assertThat(auth.get("username").getAsString()).isEqualTo("username");
        assertThat(auth.get("password").getAsString()).isEqualTo("password");
    }

    @Test
    public void testDockerRegistryWithBadKind() {
        DockerRegistrySecretEnricher enricher = new DockerRegistrySecretEnricher(context);
        KubernetesListBuilder builder = new KubernetesListBuilder();
        Secret secret = createBaseSecret(true, ANNOTATION);
        secret.setKind("Secrets");
        builder.addToSecretItems(createBaseSecret(true, ANNOTATION));
        KubernetesList expected = builder.build();

        enricher.create(PlatformMode.kubernetes, builder);
        assertEquals(expected, builder.build());
    }

    @Test
    public void testDockerRegistryWithBadAnnotation() {
        DockerRegistrySecretEnricher enricher = new DockerRegistrySecretEnricher(context);
        KubernetesListBuilder builder = new KubernetesListBuilder();
        Secret secret = createBaseSecret(true, ANNOTATION);
        secret.getMetadata().getAnnotations().put(ANNOTATION, "docker1.io");
        builder.addToSecretItems(createBaseSecret(true, ANNOTATION));

        KubernetesList expected = builder.build();

        enricher.create(PlatformMode.kubernetes, builder);
        assertEquals(expected, builder.build());
    }

    private Secret createBaseSecret(boolean withAnnotation, String annotationValue) {
        ObjectMetaBuilder metaBuilder = new ObjectMetaBuilder()
                .withNamespace("default");

        if (withAnnotation) {
            Map<String, String> annotations = new HashMap<>();
            annotations.put(annotationValue, "docker.io");
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
