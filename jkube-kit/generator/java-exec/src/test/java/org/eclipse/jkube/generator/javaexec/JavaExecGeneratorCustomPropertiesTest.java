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
package org.eclipse.jkube.generator.javaexec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;

import mockit.Expectations;
import mockit.Mocked;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class JavaExecGeneratorCustomPropertiesTest {

  @Mocked
  @SuppressWarnings("unused")
  private GeneratorContext generatorContext;

  @Test
  public void customizeWithOverriddenPropertiesShouldAddImageConfiguration() {
    // Given
    final List<ImageConfiguration> originalImageConfigurations = new ArrayList<>();
    final Properties projectProperties = new Properties();
    projectProperties.put("jkube.generator.java-exec.mainClass", "com.example.Main");
    projectProperties.put("jkube.generator.java-exec.webPort", "8082");
    projectProperties.put("jkube.generator.java-exec.jolokiaPort", "8780");
    projectProperties.put("jkube.generator.java-exec.targetDir", "/other-dir");
    projectProperties.put("jkube.generator.from", "custom-image");
    // @formatter:off
    new Expectations() {{
      generatorContext.getProject().getVersion(); result = "1.33.7-SNAPSHOT";
      generatorContext.getProject().getProperties(); result = projectProperties;
    }};
    // @formatter:on
    // When
    final List<ImageConfiguration> result = new JavaExecGenerator(generatorContext)
        .customize(originalImageConfigurations, false);
    // Then
    assertThat(result)
        .hasSize(1)
        .first()
        .hasFieldOrPropertyWithValue("name", "%g/%a:%l")
        .hasFieldOrPropertyWithValue("alias", "java-exec")
        .extracting(ImageConfiguration::getBuildConfiguration)
        .hasFieldOrPropertyWithValue("from", "custom-image")
        .hasFieldOrPropertyWithValue("tags", Collections.singletonList("latest"))
        .hasFieldOrPropertyWithValue("env", new HashMap<String, String>(){{
          put("JAVA_APP_DIR", "/other-dir");
          put("JAVA_MAIN_CLASS", "com.example.Main");
        }})
        .hasFieldOrPropertyWithValue("ports", Arrays.asList("8082", "8780", "9779"))
        .extracting(BuildConfiguration::getAssembly)
        .hasFieldOrPropertyWithValue("excludeFinalOutputArtifact", false);
  }
}
