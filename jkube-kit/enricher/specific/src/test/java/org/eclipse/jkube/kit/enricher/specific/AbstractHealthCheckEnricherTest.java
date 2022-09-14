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
package org.eclipse.jkube.kit.enricher.specific;

import io.fabric8.kubernetes.api.builder.TypedVisitor;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.Probe;
import io.fabric8.kubernetes.api.model.ProbeBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.eclipse.jkube.kit.enricher.api.EnricherContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.spy;

/**
 * Tests that the enrichment is performed on the right containers.
 *
 * @author Nicola
 */
class AbstractHealthCheckEnricherTest {

    private KitLogger log;

    @BeforeEach
    void setUp() {
        log = spy(new KitLogger.SilentLogger());
    }

    @Test
    void enrichSingleContainer() {
        KubernetesListBuilder list = new KubernetesListBuilder().addToItems(new DeploymentBuilder()
            .withNewSpec()
            .withNewTemplate()
            .withNewSpec()
            .addNewContainer()
            .withName("app")
            .withImage("app:latest")
            .endContainer()
            .endSpec()
            .endTemplate()
            .endSpec()
            .build());

        createEnricher(new Properties(), Collections.emptyMap()).create(PlatformMode.kubernetes, list);

        final AtomicInteger containerFound = new AtomicInteger(0);
        list.accept(new TypedVisitor<ContainerBuilder>() {
            @Override
            public void visit(ContainerBuilder containerBuilder) {
                Container container = containerBuilder.build();
                assertThat(container.getLivenessProbe()).isNotNull();
                assertThat(container.getReadinessProbe()).isNotNull();
                assertThat(container.getStartupProbe()).isNotNull();
                containerFound.incrementAndGet();
            }
        });

        assertThat(containerFound.get()).isEqualTo(1);
    }

    @Test
    void enrichContainerWithSidecar() {
        KubernetesListBuilder list = new KubernetesListBuilder().addToItems(new DeploymentBuilder()
            .withNewSpec()
                .withNewTemplate()
                    .withNewSpec()
                        .addNewContainer()
                            .withName("app")
                            .withImage("app:latest")
                        .endContainer()
                        .addNewContainer()
                            .withName("sidecar")
                            .withImage("sidecar:latest")
                        .endContainer()
                    .endSpec()
                .endTemplate()
            .endSpec()
            .build());

        createEnricher(new Properties(), Collections.singletonMap("FABRIC8_GENERATED_CONTAINERS", "app")).create(PlatformMode.kubernetes, list);

        final AtomicInteger containerFound = new AtomicInteger(0);
        list.accept(new TypedVisitor<ContainerBuilder>() {
            @Override
            public void visit(ContainerBuilder containerBuilder) {
                Container container = containerBuilder.build();
                if (container.getName().equals("app")) {
                    assertThat(container.getLivenessProbe()).isNotNull();
                    assertThat(container.getReadinessProbe()).isNotNull();
                    assertThat(container.getStartupProbe()).isNotNull();
                    containerFound.incrementAndGet();
                } else if (container.getName().equals("sidecar")) {
                    assertThat(container)
                            .hasFieldOrPropertyWithValue("livenessProbe", null)
                            .hasFieldOrPropertyWithValue("readinessProbe", null)
                            .hasFieldOrPropertyWithValue("startupProbe", null);
                    containerFound.incrementAndGet();
                }
            }
        });

        assertThat(containerFound.get()).isEqualTo(2);
    }

    @Test
    void enrichSpecificContainers() {
        final Properties properties = new Properties();
        properties.put(AbstractHealthCheckEnricher.ENRICH_CONTAINERS, "app2,app3");

        KubernetesListBuilder list = new KubernetesListBuilder().addToItems(new DeploymentBuilder()
            .withNewSpec()
                .withNewTemplate()
                    .withNewSpec()
                        .addNewContainer()
                            .withName("app")
                            .withImage("app:latest")
                        .endContainer()
                        .addNewContainer()
                            .withName("app2")
                            .withImage("app2:latest")
                        .endContainer()
                        .addNewContainer()
                            .withName("app3")
                            .withImage("app3:latest")
                        .endContainer()
                    .endSpec()
                .endTemplate()
            .endSpec()
            .build());

        createEnricher(properties, Collections.emptyMap()).create(PlatformMode.kubernetes, list);

        final AtomicInteger containerFound = new AtomicInteger(0);
        list.accept(new TypedVisitor<ContainerBuilder>() {
            @Override
            public void visit(ContainerBuilder containerBuilder) {
                Container container = containerBuilder.build();
                switch (container.getName()) {
                    case "app":
                        assertThat(container)
                                .hasFieldOrPropertyWithValue("livenessProbe", null)
                                .hasFieldOrPropertyWithValue("readinessProbe", null)
                                .hasFieldOrPropertyWithValue("startupProbe", null);
                        containerFound.incrementAndGet();
                        break;
                    case "app2":
                    case "app3":
                        assertThat(container.getLivenessProbe()).isNotNull();
                        assertThat(container.getReadinessProbe()).isNotNull();
                        assertThat(container.getStartupProbe()).isNotNull();
                        containerFound.incrementAndGet();
                        break;
                }
            }
        });

        assertThat(containerFound.get()).isEqualTo(3);
    }

    @Test
    void enrichAllContainers() {
        final Properties properties = new Properties();
        properties.put(AbstractHealthCheckEnricher.ENRICH_ALL_CONTAINERS, "true");

        KubernetesListBuilder list = new KubernetesListBuilder().addToItems(new DeploymentBuilder()
            .withNewSpec()
                .withNewTemplate()
                    .withNewSpec()
                        .addNewContainer()
                            .withName("app")
                            .withImage("app:latest")
                        .endContainer()
                        .addNewContainer()
                            .withName("app2")
                            .withImage("app2:latest")
                        .endContainer()
                    .endSpec()
                .endTemplate()
            .endSpec()
            .build());

        createEnricher(properties, Collections.emptyMap()).create(PlatformMode.kubernetes,list);

        final AtomicInteger containerFound = new AtomicInteger(0);
        list.accept(new TypedVisitor<ContainerBuilder>() {
            @Override
            public void visit(ContainerBuilder containerBuilder) {
                Container container = containerBuilder.build();
                if (container.getName().equals("app")) {
                    assertThat(container.getLivenessProbe()).isNotNull();
                    assertThat(container.getReadinessProbe()).isNotNull();
                    assertThat(container.getStartupProbe()).isNotNull();
                    containerFound.incrementAndGet();
                } else if (container.getName().equals("app2")) {
                    assertThat(container.getLivenessProbe()).isNotNull();
                    assertThat(container.getReadinessProbe()).isNotNull();
                    assertThat(container.getStartupProbe()).isNotNull();
                    containerFound.incrementAndGet();
                }
            }
        });

        assertThat(containerFound.get()).isEqualTo(2);
    }

    protected AbstractHealthCheckEnricher createEnricher(Properties properties, Map<String, String> pi) {

        JavaProject project = JavaProject.builder().properties(new Properties()).build();
        project.getProperties().putAll(properties);

        final JKubeEnricherContext.JKubeEnricherContextBuilder enricherContextBuilder = JKubeEnricherContext.builder()
                .project(project)
                .log(log);
        if(pi != null && !pi.isEmpty()) {
            enricherContextBuilder.processingInstructions(pi);
        }
        EnricherContext context = enricherContextBuilder.build();

        return new AbstractHealthCheckEnricher(context, "basic") {
            @Override
            protected Probe getLivenessProbe() {
                return getReadinessProbe();
            }

            @Override
            protected Probe getStartupProbe() {
                return getReadinessProbe();
            }

            @Override
            protected Probe getReadinessProbe() {
                return new ProbeBuilder()
                        .withNewHttpGet()
                        .withHost("localhost")
                        .withNewPort(8080)
                        .endHttpGet()
                        .build();
            }
        };
    }

}