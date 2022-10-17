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
package org.eclipse.jkube.generator.api;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.service.SummaryService;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class GeneratorManagerTest {

  private KitLogger logger;
  private SummaryService summaryService;

  private GeneratorContext generatorContext;

  @BeforeEach
  void setUp() {
    logger = spy(new KitLogger.SilentLogger());
    final ProcessorConfig processorConfig = new ProcessorConfig();
    processorConfig.setIncludes(Collections.singletonList("fake-generator"));
    summaryService = new SummaryService(new File("target"), logger, false);
    generatorContext = GeneratorContext.builder()
        .config(processorConfig)
        .logger(logger)
        .build();
  }

  @Test
  void generate_withTestGenerator_shouldProcessImages() {
    // Given
    final List<ImageConfiguration> images = Collections.singletonList(new ImageConfiguration());
    // When
    final List<ImageConfiguration> result = GeneratorManager
        .generate(images, generatorContext, false, summaryService);
    // Then
    assertThat(result)
        .isNotSameAs(images)
        .hasSize(1)
        .extracting(ImageConfiguration::getName)
        .contains("processed-by-test");
    verify(logger, times(1)).info("Running generator %s", "fake-generator");
  }

  // Loaded from META-INF/jkube/generator-default
  public static final class TestGenerator implements Generator {

    public TestGenerator(GeneratorContext ignored) {
    }

    @Override
    public String getName() {
      return "fake-generator";
    }

    @Override
    public boolean isApplicable(List<ImageConfiguration> configs) {
      return true;
    }

    @Override
    public List<ImageConfiguration> customize(List<ImageConfiguration> existingConfigs, boolean prePackagePhase) {
      return existingConfigs.stream()
          .peek(ic -> ic.setName("processed-by-test"))
          .collect(Collectors.toList());
    }
  }
}