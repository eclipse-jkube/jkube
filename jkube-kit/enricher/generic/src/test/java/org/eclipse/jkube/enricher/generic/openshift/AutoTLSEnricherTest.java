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
package org.eclipse.jkube.enricher.generic.openshift;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.PodTemplate;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.Volume;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.eclipse.jkube.kit.enricher.api.model.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AutoTLSEnricherTest {
    private JKubeEnricherContext context;

    @AllArgsConstructor
    @Builder
    private static final class AdaptTestConfig {
        private final RuntimeMode mode;
        private final String initContainerNameConfig;
        private final String initContainerName;
        private final String initContainerImageConfig;
        private final String initContainerImage;
        private final String tlsSecretVolumeNameConfig;
        private final String tlsSecretVolumeName;
        private final String jksVolumeNameConfig;
        private final String jksVolumeName;
    }

    @BeforeEach
    void setup() {
      context = mock(JKubeEnricherContext.class, RETURNS_DEEP_STUBS);
    }

    @Test
    void adapt() {
        final AdaptTestConfig[] data = new AdaptTestConfig[] {
            AdaptTestConfig.builder().mode(RuntimeMode.KUBERNETES).build(),
            new AdaptTestConfig(RuntimeMode.OPENSHIFT, null, "tls-jks-converter", null,
                    "jimmidyson/pemtokeystore:v0.1.0", null, "tls-pem", null, "tls-jks"),
            new AdaptTestConfig(RuntimeMode.OPENSHIFT, null, "tls-jks-converter", null,
                    "jimmidyson/pemtokeystore:v0.1.0", "tls-a", "tls-a", null, "tls-jks"),
            new AdaptTestConfig(RuntimeMode.OPENSHIFT, null, "tls-jks-converter", null,
                    "jimmidyson/pemtokeystore:v0.1.0", null, "tls-pem", "jks-b", "jks-b"),
            new AdaptTestConfig(RuntimeMode.OPENSHIFT, "test-container-name", "test-container-name", "image/123",
                    "image/123", "tls-a", "tls-a", "jks-b", "jks-b") };

        for (final AdaptTestConfig tc : data) {
            TreeMap<String, Object> configMap = new TreeMap<>();
            configMap.put("pemToJKSInitContainerName", tc.initContainerNameConfig);
            configMap.put("pemToJKSInitContainerImage", tc.initContainerImageConfig);
            configMap.put("tlsSecretVolumeName", tc.tlsSecretVolumeNameConfig);
            configMap.put("jksVolumeName", tc.jksVolumeNameConfig);

            final ProcessorConfig config = new ProcessorConfig(null, null,
                    Collections.singletonMap("jkube-openshift-autotls", configMap));

            final Properties properties = new Properties();
            properties.put(RuntimeMode.JKUBE_EFFECTIVE_PLATFORM_MODE, tc.mode.name());
            Configuration configuration = Configuration.builder()
                    .processorConfig(config)
                    .build();
            when(context.getProperties()).thenReturn(properties);
            when(context.getConfiguration()).thenReturn(configuration);

            AutoTLSEnricher enricher = new AutoTLSEnricher(context);
            KubernetesListBuilder klb = new KubernetesListBuilder()
                    .addNewPodTemplateItem()
                        .withNewMetadata().and()
                            .withNewTemplate()
                            .withNewMetadata()
                            .and()
                            .withNewSpec()
                            .and().and().and()
                        .addNewServiceItem()
                    .and();
            enricher.enrich(PlatformMode.kubernetes, klb);
            PodTemplate pt = (PodTemplate) klb.buildItems().get(0);
            Service service = (Service) klb.buildItems().get(1);
            ObjectMeta om = service.getMetadata();

            List<Container> initContainers = pt.getTemplate().getSpec().getInitContainers();
            assertThat(!initContainers.isEmpty()).isEqualTo(tc.mode == RuntimeMode.OPENSHIFT);
            if (tc.mode == RuntimeMode.KUBERNETES) {
                continue;
            }

            //Test metadata annotation
            Map<String, String> generatedAnnotation = om.getAnnotations();
            assertThat(generatedAnnotation).containsKey(AutoTLSEnricher.AUTOTLS_ANNOTATION_KEY)
                    .containsValue(context.getGav().getArtifactId() + "-tls");

            //Test Pod template
            Gson gson = new Gson();
            JsonArray ja = JsonParser.parseString(gson.toJson(initContainers, new TypeToken<Collection<Container>>() {}.getType())).getAsJsonArray();
            assertThat(ja).hasSize(1);
            JsonObject jo = ja.get(0).getAsJsonObject();
            assertThat(jo)
                .returns(tc.initContainerName, obj -> obj.get("name").getAsString())
                .returns(tc.initContainerImage, obj -> obj.get("image").getAsString());
            //Test volumes are created
            List<Volume> volumes = pt.getTemplate().getSpec().getVolumes();
            assertThat(volumes).hasSize(2);
            List<String> volumeNames = volumes.stream().map(Volume::getName).collect(Collectors.toList());
            assertThat(volumeNames).contains(tc.tlsSecretVolumeName, tc.jksVolumeName);
            //Test volume mounts are created
            JsonArray mounts = jo.get("volumeMounts").getAsJsonArray();
            assertThat(mounts).hasSize(2);
            JsonObject mount = mounts.get(0).getAsJsonObject();
            assertThat(mount.get("name").getAsString()).isEqualTo(tc.tlsSecretVolumeName);
            mount = mounts.get(1).getAsJsonObject();
            assertThat(mount.get("name").getAsString()).isEqualTo(tc.jksVolumeName);
        }
    }
}

