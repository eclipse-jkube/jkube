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
package org.eclipse.jkube.kit.enricher.api;

import java.util.Collections;

import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.resource.EnricherManager;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class DefaultEnricherManagerTest {

  KitLogger logger;

  private EnricherManager enricherManager;

  @Before
  public void setUp() throws Exception {
    logger = mock(KitLogger.class);
    final ProcessorConfig processorConfig = new ProcessorConfig();
    processorConfig.setIncludes(Collections.singletonList("fake-enricher"));
    final EnricherContext enricherContext = JKubeEnricherContext.builder()
        .project(JavaProject.builder().build())
        .log(logger)
        .processorConfig(processorConfig)
        .build();
    enricherManager = new DefaultEnricherManager(enricherContext);
  }

  @Test
  public void constructor() {
    verify(logger).verbose("- %s", "fake-enricher",times(1));
  }

  @Test
  public void createDefaultResources_withDefaults_createsResources() {
    // Given
    final KubernetesListBuilder klb = new KubernetesListBuilder();
    // When
    enricherManager.createDefaultResources(PlatformMode.kubernetes, klb);
    // Then
    assertThat(klb.build().getItems())
        .singleElement()
        .isInstanceOf(ConfigMap.class)
        .hasFieldOrPropertyWithValue("metadata.name", "created-by-test");
  }

  @Test
  public void enrich_withDefaults_createsResources() {
    // Given
    final KubernetesListBuilder klb = new KubernetesListBuilder();
    // When
    enricherManager.enrich(PlatformMode.kubernetes, klb);
    // Then
    assertThat(klb.build().getItems())
        .singleElement()
        .isInstanceOf(ConfigMap.class)
        .hasFieldOrPropertyWithValue("metadata.name", "enriched-by-test");
  }

  // Loaded from META-INF/jkube/enricher-default
  public static final class TestEnricher implements Enricher {

    private final JKubeEnricherContext context;

    public TestEnricher(JKubeEnricherContext context) {
      this.context = context;
    }

    @Override
    public String getName() {
      return "fake-enricher";
    }

    @Override
    public void create(PlatformMode platformMode, KubernetesListBuilder builder) {
      builder.addNewConfigMapItem().withNewMetadata().withName("created-by-test").endMetadata().endConfigMapItem();
    }

    @Override
    public void enrich(PlatformMode platformMode, KubernetesListBuilder builder) {
      builder.addNewConfigMapItem().withNewMetadata().withName("enriched-by-test").endMetadata().endConfigMapItem();
    }

    @Override
    public EnricherContext getContext() {
      return context;
    }
  }
}
