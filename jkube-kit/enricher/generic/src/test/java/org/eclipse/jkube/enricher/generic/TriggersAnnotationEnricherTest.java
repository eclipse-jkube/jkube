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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.apps.DaemonSet;
import io.fabric8.kubernetes.api.model.apps.ReplicaSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.batch.Job;
import io.fabric8.openshift.api.model.ImageChangeTrigger;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.eclipse.jkube.kit.enricher.api.model.Configuration;
import mockit.Expectations;
import mockit.Mocked;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;

/**
 * @author nicola
 */
public class TriggersAnnotationEnricherTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Mocked
    private JKubeEnricherContext context;

    @Test
    public void testStatefulSetEnrichment() throws IOException {

        KubernetesListBuilder builder = new KubernetesListBuilder()
                .addNewStatefulSetItem()
                    .withNewSpec()
                        .withNewTemplate()
                            .withNewSpec()
                                .withContainers(createContainers("c1", "is:latest"))
                            .endSpec()
                        .endTemplate()
                    .endSpec()
                .endStatefulSetItem();


        TriggersAnnotationEnricher enricher = new TriggersAnnotationEnricher(context);
        enricher.enrich(PlatformMode.kubernetes, builder);


        StatefulSet res = (StatefulSet) builder.build().getItems().get(0);
        String triggers = res.getMetadata().getAnnotations().get("image.openshift.io/triggers");
        assertNotNull(triggers);

        List<ImageChangeTrigger> triggerList = OBJECT_MAPPER.readValue(triggers, OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, ImageChangeTrigger.class));
        assertEquals(1, triggerList.size());

        ImageChangeTrigger trigger = triggerList.get(0);
        assertEquals("ImageStreamTag", trigger.getFrom().getKind());
        assertEquals("is:latest", trigger.getFrom().getName());
        assertTrue(trigger.getAdditionalProperties().containsKey("fieldPath"));

    }

    @Test
    public void testReplicaSetEnrichment() throws IOException {

        KubernetesListBuilder builder = new KubernetesListBuilder()
                .addNewReplicaSetItem()
                    .withNewSpec()
                        .withNewTemplate()
                            .withNewSpec()
                                .withContainers(createContainers(
                                    "c1", "is",
                                    "c2", "a-docker-user/is:latest"
                                ))
                            .endSpec()
                        .endTemplate()
                    .endSpec()
                .endReplicaSetItem();


        TriggersAnnotationEnricher enricher = new TriggersAnnotationEnricher(context);
        enricher.enrich(PlatformMode.kubernetes, builder);


        ReplicaSet res = (ReplicaSet) builder.build().getItems().get(0);
        String triggers = res.getMetadata().getAnnotations().get("image.openshift.io/triggers");
        assertNotNull(triggers);

        List<ImageChangeTrigger> triggerList = OBJECT_MAPPER.readValue(triggers, OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, ImageChangeTrigger.class));
        assertEquals(1, triggerList.size());

        ImageChangeTrigger trigger = triggerList.get(0);
        assertEquals("ImageStreamTag", trigger.getFrom().getKind());
        assertEquals("is:latest", trigger.getFrom().getName());
        assertTrue(trigger.getAdditionalProperties().containsKey("fieldPath"));
    }

    @Test
    public void testDaemonSetEnrichment() throws IOException {

        KubernetesListBuilder builder = new KubernetesListBuilder()
                .addNewDaemonSetItem()
                    .withNewMetadata()
                        .addToAnnotations("annkey", "annvalue")
                    .endMetadata()
                    .withNewSpec()
                        .withNewTemplate()
                            .withNewSpec()
                                .withContainers(createContainers(
                                    "c1", "iss:1.1.0",
                                    "c2", "docker.io/a-docker-user/is:latest"
                                ))
                            .endSpec()
                        .endTemplate()
                    .endSpec()
                .endDaemonSetItem();


        TriggersAnnotationEnricher enricher = new TriggersAnnotationEnricher(context);
        enricher.enrich(PlatformMode.kubernetes, builder);


        DaemonSet res = (DaemonSet) builder.build().getItems().get(0);
        String triggers = res.getMetadata().getAnnotations().get("image.openshift.io/triggers");
        assertNotNull(triggers);

        List<ImageChangeTrigger> triggerList = OBJECT_MAPPER.readValue(triggers, OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, ImageChangeTrigger.class));
        assertEquals(1, triggerList.size());

        ImageChangeTrigger trigger = triggerList.get(0);
        assertEquals("ImageStreamTag", trigger.getFrom().getKind());
        assertEquals("iss:1.1.0", trigger.getFrom().getName());
        assertTrue(trigger.getAdditionalProperties().containsKey("fieldPath"));

        assertEquals("annvalue", res.getMetadata().getAnnotations().get("annkey"));
    }

    @Test
    public void testConditionalStatefulSetEnrichment() throws IOException {

        final Properties props = new Properties();
        props.put("jkube.enricher.jkube-triggers-annotation.containers", "c2, c3, anotherc");
        new Expectations() {{
            context.getConfiguration(); result = Configuration.builder().properties(props).build();
        }};

        KubernetesListBuilder builder = new KubernetesListBuilder()
                .addNewStatefulSetItem()
                    .withNewSpec()
                        .withNewTemplate()
                            .withNewSpec()
                                .withContainers(createContainers(
                                    "c1", "is1:latest",
                                    "c2", "is2:latest",
                                    "c3", "is3:latest"
                                ))
                            .endSpec()
                        .endTemplate()
                    .endSpec()
                .endStatefulSetItem();


        TriggersAnnotationEnricher enricher = new TriggersAnnotationEnricher(context);
        enricher.enrich(PlatformMode.kubernetes, builder);


        StatefulSet res = (StatefulSet) builder.build().getItems().get(0);
        String triggers = res.getMetadata().getAnnotations().get("image.openshift.io/triggers");
        assertNotNull(triggers);

        List<ImageChangeTrigger> triggerList = OBJECT_MAPPER.readValue(triggers, OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, ImageChangeTrigger.class));
        assertEquals(2, triggerList.size());

        ImageChangeTrigger trigger1 = triggerList.get(0);
        assertEquals("ImageStreamTag", trigger1.getFrom().getKind());
        assertEquals("is2:latest", trigger1.getFrom().getName());
        assertTrue(trigger1.getAdditionalProperties().containsKey("fieldPath"));

        ImageChangeTrigger trigger2 = triggerList.get(1);
        assertEquals("ImageStreamTag", trigger2.getFrom().getKind());
        assertEquals("is3:latest", trigger2.getFrom().getName());
        assertTrue(trigger2.getAdditionalProperties().containsKey("fieldPath"));
    }

    @Test
    public void testNoEnrichment() {

        KubernetesListBuilder builder = new KubernetesListBuilder()
                .addNewJobItem()
                    .withNewMetadata()
                        .addToAnnotations("dummy", "annotation")
                    .endMetadata()
                    .withNewSpec()
                        .withNewTemplate()
                            .withNewSpec()
                                .withContainers(createContainers(
                                    "c1", "is1:latest",
                                    "c2", "is2:latest"
                                ))
                            .endSpec()
                        .endTemplate()
                    .endSpec()
                .endJobItem();


        TriggersAnnotationEnricher enricher = new TriggersAnnotationEnricher(context);
        enricher.enrich(PlatformMode.kubernetes, builder);


        Job res = (Job) builder.build().getItems().get(0);
        String triggers = res.getMetadata().getAnnotations().get("image.openshift.io/triggers");
        assertNull(triggers);
    }


    private List<Container> createContainers(String... nameImage) {
        assertEquals(0, nameImage.length % 2);
        List<Container> containers = new ArrayList<>();
        for (int i=0; i<nameImage.length; i+=2) {
            Container container = new ContainerBuilder()
                    .withName(nameImage[i])
                    .withImage(nameImage[i+1])
                    .build();
            containers.add(container);
        }

        return containers;
    }

}
