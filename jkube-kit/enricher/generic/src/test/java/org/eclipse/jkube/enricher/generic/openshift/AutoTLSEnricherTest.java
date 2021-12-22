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

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.PodTemplate;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeMount;
import lombok.AllArgsConstructor;
import lombok.Builder;
import mockit.Expectations;
import mockit.Mocked;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.eclipse.jkube.kit.enricher.api.model.Configuration;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class AutoTLSEnricherTest {

    @Mocked
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

    @Test
    public void testAdapt() {
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

            // @formatter:off
            new Expectations() {{
                Configuration configuration = Configuration.builder()
                    .processorConfig(config)
                    .build();
                context.getProperties(); result = properties;
                context.getConfiguration(); result = configuration;
            }};
            // @formatter:on

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
            assertEquals(tc.mode == RuntimeMode.OPENSHIFT,!initContainers.isEmpty());
            if (tc.mode == RuntimeMode.KUBERNETES) {
                continue;
            }

            //Test metadata annotation
            Map<String, String> generatedAnnotation = om.getAnnotations();
            Assert.assertTrue(generatedAnnotation.containsKey(AutoTLSEnricher.AUTOTLS_ANNOTATION_KEY));
            Assert.assertTrue(generatedAnnotation.containsValue(context.getGav().getArtifactId() + "-tls"));

            //Test Pod template
            assertEquals(1, initContainers.size());
            Container initContainer = initContainers.get(0);
            assertEquals(tc.initContainerName, initContainer.getName());
            assertEquals(tc.initContainerImage, initContainer.getImage());
            //Test volumes are created
            List<Volume> volumes = pt.getTemplate().getSpec().getVolumes();
            assertEquals(2, volumes.size());
            List<String> volumeNames = volumes.stream().map(Volume::getName).collect(Collectors.toList());
            Assert.assertTrue(volumeNames.contains(tc.tlsSecretVolumeName));
            Assert.assertTrue(volumeNames.contains(tc.jksVolumeName));
            //Test volume mounts are created
            List<VolumeMount> volumeMounts = initContainer.getVolumeMounts();
            assertEquals(2, volumeMounts.size());
            VolumeMount mount = volumeMounts.get(0);
            assertEquals(tc.tlsSecretVolumeName, mount.getName());
            mount = volumeMounts.get(1);
            assertEquals(tc.jksVolumeName, mount.getName());
        }
    }
}

