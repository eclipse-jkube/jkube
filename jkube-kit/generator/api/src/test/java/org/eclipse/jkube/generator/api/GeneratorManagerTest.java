/**
 * Copyright (c) 2019 Red Hat, Inc. This program and the accompanying materials are made available
 * under the terms of the Eclipse Public License 2.0 which is available at:
 * <p>
 * https://www.eclipse.org/legal/epl-2.0/
 * <p>
 * SPDX-License-Identifier: EPL-2.0
 * <p>
 * Contributors: Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.jkube.generator.api;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;

import mockit.Mocked;
import mockit.Verifications;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class GeneratorManagerTest {

  @Mocked
  KitLogger logger;

  private GeneratorContext generatorContext;

  @Before
  public void setUp() throws Exception {
    final ProcessorConfig processorConfig = new ProcessorConfig();
    processorConfig.setIncludes(Collections.singletonList("fake-generator"));
    generatorContext = GeneratorContext.builder()
        .config(processorConfig)
        .logger(logger)
        .build();
  }

  @Test
  public void generate_withTestGenerator_shouldProcessImages() {
    // Given
    final List<ImageConfiguration> images = Collections.singletonList(new ImageConfiguration());
    // When
    final List<ImageConfiguration> result = GeneratorManager
        .generate(images, generatorContext, false);
    // Then
    assertThat(result)
        .isNotSameAs(images)
        .hasSize(1)
        .extracting(ImageConfiguration::getName)
        .contains("processed-by-test");
    // @formatter:off
    new Verifications() {{
      logger.info("Running generator %s", "fake-generator");
      times = 1;
    }};
    // @formatter:on
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
    public List<ImageConfiguration> customize(List<ImageConfiguration> existingConfigs,
        boolean prePackagePhase) {
      return existingConfigs.stream()
          .peek(ic -> {
            ic.setName("processed-by-test");
          })
          .collect(Collectors.toList());
    }
  }
}