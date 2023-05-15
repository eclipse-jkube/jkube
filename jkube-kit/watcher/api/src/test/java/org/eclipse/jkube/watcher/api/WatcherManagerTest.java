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
package org.eclipse.jkube.watcher.api;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;

import io.fabric8.kubernetes.api.model.HasMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WatcherManagerTest {

  private KitLogger logger;

  private WatcherContext watcherContext;

  @BeforeEach
  void setUp() {
    JKubeServiceHub jKubeServiceHub = mock(JKubeServiceHub.class, RETURNS_DEEP_STUBS);
    logger = mock(KitLogger.class);
    final ProcessorConfig processorConfig = new ProcessorConfig();
    processorConfig.setIncludes(Collections.singletonList("fake-watcher"));
    watcherContext = WatcherContext.builder()
        .config(processorConfig)
        .jKubeServiceHub(jKubeServiceHub)
        .logger(logger)
        .build();
  }

  @Test
  void watch_withTestWatcher_shouldMutateImages() throws Exception {
    // Given
    final List<ImageConfiguration> images = Collections.singletonList(new ImageConfiguration());
    // When
    WatcherManager.watch(images, null, Collections.emptyList(), watcherContext);
    // Then
    assertThat(images)
        .hasSize(1)
        .extracting(ImageConfiguration::getName)
        .contains("processed-by-test");
    verify(logger,times(1)).info("Running watcher %s", "fake-watcher");
  }

  // Loaded from META-INF/jkube/watcher-default
  public static final class TestWatcher implements Watcher {

    public TestWatcher(WatcherContext ignored) {
    }

    @Override
    public String getName() {
      return "fake-watcher";
    }

    @Override
    public boolean isApplicable(List<ImageConfiguration> configs, Collection<HasMetadata> resources, PlatformMode mode) {
      return true;
    }

    @Override
    public void watch(List<ImageConfiguration> configs, String namespace, Collection<HasMetadata> resources, PlatformMode mode) {
      configs.forEach(ic -> ic.setName("processed-by-test"));
    }
  }
}
