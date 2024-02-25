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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.apps.DaemonSet;
import io.fabric8.kubernetes.api.model.apps.DaemonSetBuilder;
import io.fabric8.kubernetes.api.model.apps.ReplicaSet;
import io.fabric8.kubernetes.api.model.apps.ReplicaSetBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.openshift.api.model.ImageChangeTrigger;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author nicola
 */
class TriggersAnnotationEnricherTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private JKubeEnricherContext context;
    @BeforeEach
    void setUp() {
        context = mock(JKubeEnricherContext.class,RETURNS_DEEP_STUBS);
    }

    @Test
    void testEnrichmentNotPerformedInNonOpenShiftMode() throws IOException {
        KubernetesListBuilder builder = new KubernetesListBuilder()
            .addToItems(new StatefulSetBuilder()
                .withNewMetadata()
                    .addToAnnotations("annkey", "annvalue")
                .endMetadata()
                .withNewSpec()
                    .withNewTemplate()
                        .withNewSpec()
                            .withContainers(createContainers("c1", "is:latest"))
                        .endSpec()
                    .endTemplate()
                .endSpec()
                .build());

        TriggersAnnotationEnricher enricher = new TriggersAnnotationEnricher(context);
        enricher.enrich(PlatformMode.kubernetes, builder);

        StatefulSet res = (StatefulSet) builder.build().getItems().get(0);
        assertThat(res.getMetadata().getAnnotations())
            .containsEntry("annkey", "annvalue")
            .doesNotContainKey("image.openshift.io/triggers")
            .hasSize(1); // Only the original annotation should be present
    }

    @Test
    void testEnrichmentPerformedInOpenShiftMode() throws IOException {
        KubernetesListBuilder builder = new KubernetesListBuilder()
            .addToItems(new StatefulSetBuilder()
                .withNewMetadata()
                    .addToAnnotations("annkey", "annvalue")
                .endMetadata()
                .withNewSpec()
                    .withNewTemplate()
                        .withNewSpec()
                            .withContainers(createContainers("c1", "is:latest"))
                        .endSpec()
                    .endTemplate()
                .endSpec()
                .build());

        TriggersAnnotationEnricher enricher = new TriggersAnnotationEnricher(context);
        enricher.enrich(PlatformMode.openshift, builder);

        StatefulSet res = (StatefulSet) builder.build().getItems().get(0);
        assertThat(res.getMetadata().getAnnotations())
            .containsEntry("annkey", "annvalue") // Existing annotation should be preserved
            .containsKey("image.openshift.io/triggers") // New annotation should be added
            .hasSize(2); // Total number of annotations should be 2
    }
    @Test
    void statefulSetEnrichment() throws IOException {

        KubernetesListBuilder builder = new KubernetesListBuilder()
                .addToItems(new StatefulSetBuilder()
                    .withNewSpec()
                        .withNewTemplate()
                            .withNewSpec()
                                .withContainers(createContainers("c1", "is:latest"))
                            .endSpec()
                        .endTemplate()
                    .endSpec()
                    .build());

        TriggersAnnotationEnricher enricher = new TriggersAnnotationEnricher(context);
        enricher.enrich(PlatformMode.openshift, builder);

        StatefulSet res = (StatefulSet) builder.build().getItems().get(0);
        String triggers = res.getMetadata().getAnnotations().get("image.openshift.io/triggers");
        assertThat(triggers).isNotNull();

        List<ImageChangeTrigger> triggerList = OBJECT_MAPPER.readValue(triggers, OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, ImageChangeTrigger.class));
        assertThat(triggerList).singleElement()
            .hasFieldOrPropertyWithValue("from.kind", "ImageStreamTag")
            .hasFieldOrPropertyWithValue("from.name", "is:latest")
            .extracting(ImageChangeTrigger::getAdditionalProperties)
            .asInstanceOf(InstanceOfAssertFactories.MAP)
            .containsKey("fieldPath");
    }

    @Test
    void replicaSetEnrichment() throws IOException {

        KubernetesListBuilder builder = new KubernetesListBuilder().addToItems(new ReplicaSetBuilder()
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
            .build());

        TriggersAnnotationEnricher enricher = new TriggersAnnotationEnricher(context);
        enricher.enrich(PlatformMode.openshift, builder);

        ReplicaSet res = (ReplicaSet) builder.build().getItems().get(0);
        String triggers = res.getMetadata().getAnnotations().get("image.openshift.io/triggers");
        assertThat(triggers).isNotNull();

        List<ImageChangeTrigger> triggerList = OBJECT_MAPPER.readValue(triggers, OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, ImageChangeTrigger.class));
        assertThat(triggerList).singleElement()
            .hasFieldOrPropertyWithValue("from.kind", "ImageStreamTag")
            .hasFieldOrPropertyWithValue("from.name", "is:latest")
            .extracting(ImageChangeTrigger::getAdditionalProperties)
            .asInstanceOf(InstanceOfAssertFactories.MAP)
            .containsKey("fieldPath");
    }

    @Test
    void daemonSetEnrichment() throws IOException {

        KubernetesListBuilder builder = new KubernetesListBuilder()
            .addToItems(new DaemonSetBuilder()
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
                .build());

        TriggersAnnotationEnricher enricher = new TriggersAnnotationEnricher(context);
        enricher.enrich(PlatformMode.openshift, builder);

        DaemonSet res = (DaemonSet) builder.build().getItems().get(0);
        String triggers = res.getMetadata().getAnnotations().get("image.openshift.io/triggers");
        assertThat(triggers).isNotNull();
        assertThat(res.getMetadata().getAnnotations())
            .containsEntry("annkey", "annvalue");

        List<ImageChangeTrigger> triggerList = OBJECT_MAPPER.readValue(triggers, OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, ImageChangeTrigger.class));
        assertThat(triggerList).singleElement()
            .hasFieldOrPropertyWithValue("from.kind", "ImageStreamTag")
            .hasFieldOrPropertyWithValue("from.name", "iss:1.1.0")
            .extracting(ImageChangeTrigger::getAdditionalProperties)
            .asInstanceOf(InstanceOfAssertFactories.MAP)
            .containsKey("fieldPath");
    }

    @Test
    void conditionalStatefulSetEnrichment() throws IOException {

        final Properties props = new Properties();
        props.put("jkube.enricher.jkube-triggers-annotation.containers", "c2, c3, anotherc");
        when(context.getProperties()).thenReturn(props);

        KubernetesListBuilder builder = new KubernetesListBuilder().addToItems(new StatefulSetBuilder()
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
            .build());

        TriggersAnnotationEnricher enricher = new TriggersAnnotationEnricher(context);
        enricher.enrich(PlatformMode.openshift, builder);

        StatefulSet res = (StatefulSet) builder.build().getItems().get(0);
        String triggers = res.getMetadata().getAnnotations().get("image.openshift.io/triggers");
        assertThat(triggers).isNotNull();

        List<ImageChangeTrigger> triggerList = OBJECT_MAPPER.readValue(triggers, OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, ImageChangeTrigger.class));
        assertThat(triggerList).hasSize(2)
            .satisfies(list -> assertThat(list).first()
                .hasFieldOrPropertyWithValue("from.kind", "ImageStreamTag")
                .hasFieldOrPropertyWithValue("from.name", "is2:latest")
                .extracting(ImageChangeTrigger::getAdditionalProperties)
                .asInstanceOf(InstanceOfAssertFactories.MAP)
                .containsKey("fieldPath")
            )
            .satisfies(list -> assertThat(list).last()
                .hasFieldOrPropertyWithValue("from.kind", "ImageStreamTag")
                .hasFieldOrPropertyWithValue("from.name", "is3:latest")
                .extracting(ImageChangeTrigger::getAdditionalProperties)
                .asInstanceOf(InstanceOfAssertFactories.MAP)
                .containsKey("fieldPath")
            );
    }
    @Test
    void noEnrichment() {

        KubernetesListBuilder builder = new KubernetesListBuilder().addToItems(new JobBuilder()
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
            .build());

        TriggersAnnotationEnricher enricher = new TriggersAnnotationEnricher(context);
        enricher.enrich(PlatformMode.kubernetes, builder);

        Job res = (Job) builder.build().getItems().get(0);
        String triggers = res.getMetadata().getAnnotations().get("image.openshift.io/triggers");
        assertThat(triggers).isNull();
    }

    private List<Container> createContainers(String... nameImage) {
        assertThat(nameImage.length % 2).isZero();
        List<Container> containers = new ArrayList<>();
        for (int i = 0; i < nameImage.length; i += 2) {
          Container container = new ContainerBuilder()
              .withName(nameImage[i])
              .withImage(nameImage[i + 1])
              .build();
          containers.add(container);
        }
        return containers;
    }



}
