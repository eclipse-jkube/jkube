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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.TreeMap;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.PodTemplate;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.eclipse.jkube.maven.enricher.api.MavenEnricherContext;
import org.eclipse.jkube.maven.enricher.api.model.Configuration;
import mockit.Expectations;
import mockit.Mocked;
import org.apache.maven.project.MavenProject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AutoTLSEnricherTest {

    @Mocked
    private MavenEnricherContext context;
    @Mocked
    MavenProject project;

    // *******************************
    // Tests
    // *******************************

    private static final class SecretNameTestConfig {
        private final PlatformMode mode;
        private final String tlsSecretNameConfig;
        private final String tlsSecretName;

        private SecretNameTestConfig(PlatformMode mode, String tlsSecretNameConfig, String tlsSecretName) {
            this.mode = mode;
            this.tlsSecretNameConfig = tlsSecretNameConfig;
            this.tlsSecretName = tlsSecretName;
        }
    }

    private static final class AdaptTestConfig {
        private final PlatformMode mode;
        private final String initContainerNameConfig;
        private final String initContainerName;
        private final String initContainerImageConfig;
        private final String initContainerImage;
        private final String tlsSecretVolumeNameConfig;
        private final String tlsSecretVolumeName;
        private final String jksVolumeNameConfig;
        private final String jksVolumeName;

        private AdaptTestConfig(PlatformMode mode, String initContainerNameConfig, String initContainerName,
                                String initContainerImageConfig, String initContainerImage, String tlsSecretVolumeNameConfig,
                                String tlsSecretVolumeName, String jksVolumeNameConfig, String jksVolumeName) {
            this.mode = mode;
            this.initContainerNameConfig = initContainerNameConfig;
            this.initContainerName = initContainerName;
            this.initContainerImageConfig = initContainerImageConfig;
            this.initContainerImage = initContainerImage;
            this.tlsSecretVolumeNameConfig = tlsSecretVolumeNameConfig;
            this.tlsSecretVolumeName = tlsSecretVolumeName;
            this.jksVolumeNameConfig = jksVolumeNameConfig;
            this.jksVolumeName = jksVolumeName;
        }
    }

    @Test
    public void testAdapt() throws Exception {
        final AdaptTestConfig[] data = new AdaptTestConfig[] {
                new AdaptTestConfig(PlatformMode.kubernetes, null, null, null, null, null, null, null, null),
                new AdaptTestConfig(PlatformMode.openshift, null, "tls-jks-converter", null,
                        "jimmidyson/pemtokeystore:v0.1.0", null, "tls-pem", null, "tls-jks"),
                new AdaptTestConfig(PlatformMode.openshift, null, "tls-jks-converter", null,
                        "jimmidyson/pemtokeystore:v0.1.0", "tls-a", "tls-a", null, "tls-jks"),
                new AdaptTestConfig(PlatformMode.openshift, null, "tls-jks-converter", null,
                        "jimmidyson/pemtokeystore:v0.1.0", null, "tls-pem", "jks-b", "jks-b"),
                new AdaptTestConfig(PlatformMode.openshift, "test-container-name", "test-container-name", "image/123",
                        "image/123", "tls-a", "tls-a", "jks-b", "jks-b") };

        for (final AdaptTestConfig tc : data) {
            TreeMap configMap = new TreeMap() {
                {
                    put(AutoTLSEnricher.Config.pemToJKSInitContainerName.name(), tc.initContainerNameConfig);
                    put(AutoTLSEnricher.Config.pemToJKSInitContainerImage.name(), tc.initContainerImageConfig);
                    put(AutoTLSEnricher.Config.tlsSecretVolumeName.name(), tc.tlsSecretVolumeNameConfig);
                    put(AutoTLSEnricher.Config.jksVolumeName.name(), tc.jksVolumeNameConfig);
                }
            };
            final ProcessorConfig config = new ProcessorConfig(null, null,
                    Collections.singletonMap(AutoTLSEnricher.ENRICHER_NAME, configMap));

            final Properties projectProps = new Properties();
            projectProps.put(RuntimeMode.FABRIC8_EFFECTIVE_PLATFORM_MODE, tc.mode.name());

            // Setup mock behaviour
            new Expectations() {
                {
                    Configuration configuration =
                            new Configuration.Builder()
                                    .properties(projectProps)
                                    .processorConfig(config)
                                    .build();
                    context.getConfiguration();
                    result = configuration;
                    project.getArtifactId();
                    result = "projectA";
                    minTimes = 0;
                }
            };

            AutoTLSEnricher enricher = new AutoTLSEnricher(context);
            KubernetesListBuilder klb = new KubernetesListBuilder().addNewPodTemplateItem().withNewMetadata().and()
                    .withNewTemplate().withNewMetadata().and().withNewSpec().and().and().and();
            enricher.enrich(PlatformMode.kubernetes, klb);
            PodTemplate pt = (PodTemplate) klb.getItems().get(0);

            List<Container> initContainers = pt.getTemplate().getSpec().getInitContainers();
            assertEquals(tc.mode == PlatformMode.openshift, !initContainers.isEmpty());

            if (tc.mode == PlatformMode.kubernetes) {
                continue;
            }

            Gson gson = new Gson();
            JsonArray ja = new JsonParser().parse(gson.toJson(initContainers, new TypeToken<Collection<Container>>() {}.getType())).getAsJsonArray();
            assertEquals(1, ja.size());
            JsonObject jo = ja.get(0).getAsJsonObject();
            assertEquals(tc.initContainerName, jo.get("name").getAsString());
            assertEquals(tc.initContainerImage, jo.get("image").getAsString());
            JsonArray mounts = jo.get("volumeMounts").getAsJsonArray();
            assertEquals(2, mounts.size());
            JsonObject mount = mounts.get(0).getAsJsonObject();
            assertEquals(tc.tlsSecretVolumeName, mount.get("name").getAsString());
            mount = mounts.get(1).getAsJsonObject();
            assertEquals(tc.jksVolumeName, mount.get("name").getAsString());
        }
    }
}

