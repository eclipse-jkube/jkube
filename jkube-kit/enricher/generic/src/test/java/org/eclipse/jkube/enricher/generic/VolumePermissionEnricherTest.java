/*
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

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodTemplate;
import io.fabric8.kubernetes.api.model.PodTemplateBuilder;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.eclipse.jkube.kit.enricher.api.model.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

class VolumePermissionEnricherTest {
    private JKubeEnricherContext context;

    // *******************************
    // Tests
    // *******************************
    @BeforeEach
    void setUp() {
        context = mock(JKubeEnricherContext.class,RETURNS_DEEP_STUBS);
    }

    private static final class TestConfig {
        private final String permission;
        private final String initContainerName;
        private final String[] volumeNames;
        private final String imageName;

        private TestConfig(String imageName,String permission, String initContainerName, String... volumeNames) {
            this.imageName=imageName;
            this.permission = permission;
            this.initContainerName = initContainerName;
            this.volumeNames = volumeNames;
        }
    }

    @Test
    void alreadyExistingInitContainer() {
        final ProcessorConfig config = mock(ProcessorConfig.class);
        when(context.getConfiguration()).thenReturn(Configuration.builder().processorConfig(config).build());
        PodTemplateBuilder ptb = createEmptyPodTemplate();
        addVolume(ptb, "VolumeA");

        Container initContainer = new ContainerBuilder()
                .withName(VolumePermissionEnricher.ENRICHER_NAME)
                .withVolumeMounts(new VolumeMountBuilder().withName("vol-blub").withMountPath("blub").build())
                .build();
        ptb.editTemplate().editSpec().withInitContainers(Collections.singletonList(initContainer)).endSpec().endTemplate();
        KubernetesListBuilder klb = new KubernetesListBuilder().addToPodTemplateItems(ptb.build());

        VolumePermissionEnricher enricher = new VolumePermissionEnricher(context);
        enricher.enrich(PlatformMode.kubernetes,klb);

        assertThat(klb.buildItems())
            .singleElement(InstanceOfAssertFactories.type(PodTemplate.class))
            .extracting(PodTemplate::getTemplate)
            .extracting(PodTemplateSpec::getSpec)
            .extracting(PodSpec::getInitContainers)
            .asList()
            .singleElement(InstanceOfAssertFactories.type(Container.class))
            .hasFieldOrPropertyWithValue("resources", null)
            .extracting(Container::getVolumeMounts)
            .asList()
            .singleElement(InstanceOfAssertFactories.type(VolumeMount.class))
            .extracting(VolumeMount::getMountPath)
            .isEqualTo("blub");
    }

    @Test
    void adapt() {
        final TestConfig[] data = new TestConfig[]{
            new TestConfig("busybox",null, null),
            new TestConfig("busybox1",null, null),
            new TestConfig("busybox",null, VolumePermissionEnricher.ENRICHER_NAME, "volumeA"),
            new TestConfig("busybox",null, VolumePermissionEnricher.ENRICHER_NAME, "volumeA", "volumeB")
        };

        for (final TestConfig tc : data) {
            final ProcessorConfig config = new ProcessorConfig(null, null,
                    Collections.singletonMap(VolumePermissionEnricher.ENRICHER_NAME,
                        Collections.singletonMap("permission", tc.permission)));

            // Setup mock behaviour
            when(context.getConfiguration()).thenReturn(Configuration.builder().processorConfig(config).build());

            VolumePermissionEnricher enricher = new VolumePermissionEnricher(context);

            PodTemplateBuilder ptb = createEmptyPodTemplate();

            for (String vn : tc.volumeNames) {
                ptb = addVolume(ptb, vn);
            }

            KubernetesListBuilder klb = new KubernetesListBuilder().addToPodTemplateItems(ptb.build());

            enricher.enrich(PlatformMode.kubernetes,klb);

            PodTemplate pt = (PodTemplate) klb.buildItem(0);

            List<Container> initContainers = pt.getTemplate().getSpec().getInitContainers();
            boolean shouldHaveInitContainer = tc.volumeNames.length > 0;
            assertThat(shouldHaveInitContainer).isNotEqualTo(initContainers.isEmpty());
            if (!shouldHaveInitContainer) {
                continue;
            }

            assertThat(pt)
                .extracting(PodTemplate::getTemplate)
                .extracting(PodTemplateSpec::getSpec)
                .extracting(PodSpec::getInitContainers)
                .asList()
                .singleElement(InstanceOfAssertFactories.type(Container.class))
                .hasFieldOrPropertyWithValue("image", tc.imageName)
                .hasFieldOrPropertyWithValue("name", tc.initContainerName)
                .hasFieldOrPropertyWithValue("command", getExpectedCommand(tc))
                .hasFieldOrPropertyWithValue("resources.limits", Collections.emptyMap())
                .hasFieldOrPropertyWithValue("resources.requests", Collections.emptyMap());
        }
    }

    @Test
    void enrich_withResourcesEnabledInConfiguration_shouldAddRequestsLimitsToVolumeInitContainer() {
        // Given
        Properties properties = new Properties();
        Map<String, Quantity> limitMap = new HashMap<>();
        limitMap.put("cpu", new Quantity("500m"));
        limitMap.put("memory", new Quantity("128Mi"));
        Map<String, Quantity> requestsMap = new HashMap<>();
        requestsMap.put("cpu", new Quantity("250m"));
        requestsMap.put("memory", new Quantity("64Mi"));
        properties.put("jkube.enricher.jkube-volume-permission.cpuLimit", "500m");
        properties.put("jkube.enricher.jkube-volume-permission.memoryLimit", "128Mi");
        properties.put("jkube.enricher.jkube-volume-permission.cpuRequest", "250m");
        properties.put("jkube.enricher.jkube-volume-permission.memoryRequest", "64Mi");

        when(context.getProperties()).thenReturn(properties);

        VolumePermissionEnricher enricher = new VolumePermissionEnricher(context);
        KubernetesListBuilder kubernetesListBuilder = new KubernetesListBuilder();
        kubernetesListBuilder.addToPodTemplateItems(addVolume(createEmptyPodTemplate(), "volumeC").build());

        // When
        enricher.enrich(PlatformMode.kubernetes, kubernetesListBuilder);

        // Then
        assertThat(kubernetesListBuilder.buildItems())
            .singleElement(InstanceOfAssertFactories.type(PodTemplate.class))
            .extracting(PodTemplate::getTemplate)
            .extracting(PodTemplateSpec::getSpec)
            .extracting(PodSpec::getInitContainers)
            .asList()
            .singleElement(InstanceOfAssertFactories.type(Container.class))
            .extracting(Container::getResources)
            .hasFieldOrPropertyWithValue("requests", requestsMap)
            .hasFieldOrPropertyWithValue("limits", limitMap);
    }

    public PodTemplateBuilder addVolume(PodTemplateBuilder ptb, String vn) {
        ptb = ptb.editTemplate().
            editSpec().
            addNewVolume().withName(vn).withNewPersistentVolumeClaim().and().and().
            addNewVolume().withName("non-pvc").withNewEmptyDir().and().and().
            and().and();
        ptb = ptb.editTemplate().editSpec().withContainers(
            new ContainerBuilder(ptb.buildTemplate().getSpec().getContainers().get(0))
                .addNewVolumeMount().withName(vn).withMountPath("/tmp/" + vn).and()
                .addNewVolumeMount().withName("non-pvc").withMountPath("/tmp/non-pvc").and()
                .build()
           ).and().and();
        return ptb;
    }

    public PodTemplateBuilder createEmptyPodTemplate() {
        return new PodTemplateBuilder().withNewMetadata().endMetadata()
                                .withNewTemplate()
                                  .withNewMetadata().endMetadata()
                                  .withNewSpec().addNewContainer().endContainer().endSpec()
                                .endTemplate();
    }

    private List<String> getExpectedCommand(TestConfig tc) {
        String permission = StringUtils.isBlank(tc.permission) ? "777" : tc.permission;
        List<String> expectedCommandStr = new ArrayList<>();
        expectedCommandStr.add("chmod");
        expectedCommandStr.add(permission);
        for (String vn : tc.volumeNames) {
            expectedCommandStr.add("/tmp/" + vn);
        }
        return expectedCommandStr;
    }
}
