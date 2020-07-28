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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.eclipse.jkube.generator.api.GeneratorContext;

import mockit.Expectations;
import mockit.Mocked;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;

public class JavaExecGeneratorCustomPropertiesTest {

  @Mocked
  private GeneratorContext generatorContext;

  @Test
  public void customizeWithOverriddenPropertiesShouldAddImageConfiguration() throws IOException {
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
    assertThat(result, hasSize(1));
    final ImageConfiguration imageConfiguration = result.iterator().next();
    assertThat(imageConfiguration.getName(), equalTo("%g/%a:%l"));
    assertThat(imageConfiguration.getAlias(), equalTo("java-exec"));
    assertThat(imageConfiguration.getBuildConfiguration().getFrom(), equalTo("custom-image"));
    assertThat(imageConfiguration.getBuildConfiguration().getTags(), contains("latest"));
    assertThat(imageConfiguration.getBuildConfiguration().getAssembly().isExcludeFinalOutputArtifact(),
        equalTo(false));
    assertThat(imageConfiguration.getBuildConfiguration().getPorts(), contains("8082", "8780", "9779"));
    assertThat(imageConfiguration.getBuildConfiguration().getEnv(), allOf(
        hasEntry("JAVA_APP_DIR", "/other-dir"),
        hasEntry("JAVA_MAIN_CLASS", "com.example.Main")
    ));
  }
}
