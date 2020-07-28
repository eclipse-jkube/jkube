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

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.PodTemplate;
import io.fabric8.kubernetes.api.model.PodTemplateBuilder;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.eclipse.jkube.kit.enricher.api.model.Configuration;
import mockit.Expectations;
import mockit.Mocked;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class VolumePermissionEnricherTest {

    @Mocked
    private JKubeEnricherContext context;

    // *******************************
    // Tests
    // *******************************

    private static final class TestConfig {
        private final String permission;
        private final String initContainerName;
        private final String[] volumeNames;

        private TestConfig(String permission, String initContainerName, String... volumeNames) {
            this.permission = permission;
            this.initContainerName = initContainerName;
            this.volumeNames = volumeNames;
        }
    }

    @Test
    public void alreadyExistingInitContainer(@Mocked final ProcessorConfig config) throws Exception {
        new Expectations() {{
            context.getConfiguration(); result = Configuration.builder().processorConfig(config).build();
        }};

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

        List<Container> initS = ((PodTemplate) klb.build().getItems().get(0)).getTemplate().getSpec().getInitContainers();
        assertNotNull(initS);
        assertEquals(1, initS.size());
        Container actualInitContainer = initS.get(0);
        assertEquals("blub", actualInitContainer.getVolumeMounts().get(0).getMountPath());
    }

    @Test
    public void testAdapt() {
        final TestConfig[] data = new TestConfig[]{
            new TestConfig(null, null),
            new TestConfig(null, VolumePermissionEnricher.ENRICHER_NAME, "volumeA"),
            new TestConfig(null, VolumePermissionEnricher.ENRICHER_NAME, "volumeA", "volumeB")
        };

        for (final TestConfig tc : data) {
            final ProcessorConfig config = new ProcessorConfig(null, null,
                    Collections.singletonMap(VolumePermissionEnricher.ENRICHER_NAME,
                        Collections.singletonMap("permission", tc.permission)));

            // Setup mock behaviour
            new Expectations() {{
                context.getConfiguration(); result = Configuration.builder().processorConfig(config).build();
            }};

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
            assertEquals(shouldHaveInitContainer, !initContainers.isEmpty());
            if (!shouldHaveInitContainer) {
                continue;
            }

            Gson gson = new Gson();
            JsonArray ja = new JsonParser().parse(gson.toJson(initContainers, new TypeToken<Collection<Container>>() {}.getType())).getAsJsonArray();
            assertEquals(1, ja.size());

            JsonObject jo = ja.get(0).getAsJsonObject();
            assertEquals(tc.initContainerName, jo.get("name").getAsString());
            String permission = StringUtils.isBlank(tc.permission) ? "777" : tc.permission;
            JsonArray chmodCmd = new JsonArray();
            chmodCmd.add("chmod");
            chmodCmd.add(permission);
            for (String vn : tc.volumeNames) {
              chmodCmd.add("/tmp/" + vn);
            }
            assertEquals(chmodCmd.toString(), jo.getAsJsonArray("command").toString());
        }
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
}
