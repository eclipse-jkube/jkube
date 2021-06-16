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
package org.eclipse.jkube.generator.webapp;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.kit.common.Plugin;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.image.build.JKubeBuildStrategy;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;

import mockit.Expectations;
import mockit.Mocked;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class WebAppGeneratorTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Mocked
  private GeneratorContext generatorContext;

  @Test
  public void isApplicableHasMavenWarPluginShouldReturnTrue(@Mocked Plugin plugin) {
    // Given
    // @formatter:off
    new Expectations() {{
      plugin.getGroupId(); result = "org.apache.maven.plugins";
      plugin.getArtifactId(); result = "maven-war-plugin";
      generatorContext.getProject().getPlugins(); result = Collections.singletonList(plugin);
    }};
    // @formatter:on
    // When
    final boolean result = new WebAppGenerator(generatorContext).isApplicable(Collections.emptyList());
    // Then
    assertThat(result).isTrue();
  }

  @Test(expected = IllegalArgumentException.class)
  public void customizeDoesNotSupportS2iBuildShouldThrowException() {
    // Given
    final Properties projectProperties = new Properties();
    projectProperties.put("jkube.generator.from", "image-to-trigger-custom-app-server-handler");
    // @formatter:off
    new Expectations() {{
      generatorContext.getRuntimeMode(); result = RuntimeMode.OPENSHIFT;
      generatorContext.getStrategy(); result = JKubeBuildStrategy.s2i;
      generatorContext.getProject().getProperties(); result = projectProperties;
    }};
    // @formatter:on
    // When
    new WebAppGenerator(generatorContext).customize(Collections.emptyList(), false);
    // Then - Exception thrown
    fail();
  }

  @Test
  public void customizeWithDefaultHandlerShouldAddImageConfiguration() throws IOException {
    // Given
    final List<ImageConfiguration> originalImageConfigurations = new ArrayList<>();
    final File buildDirectory = temporaryFolder.newFolder("build");
    final File artifactFile = new File(buildDirectory, "artifact.war");
    assertTrue(artifactFile.createNewFile());
    // @formatter:off
    new Expectations() {{
      generatorContext.getProject().getBuildDirectory(); result = buildDirectory;
      generatorContext.getProject().getBuildFinalName(); result = "artifact";
      generatorContext.getProject().getPackaging(); result = "war";
      generatorContext.getProject().getVersion(); result = "1.33.7-SNAPSHOT";
    }};
    // @formatter:on
    // When
    final List<ImageConfiguration> result = new WebAppGenerator(generatorContext)
        .customize(originalImageConfigurations, false);
    // Then
    assertThat(result)
        .isSameAs(originalImageConfigurations)
        .hasSize(1)
        .first()
        .hasFieldOrPropertyWithValue("name", "%g/%a:%l")
        .hasFieldOrPropertyWithValue("alias", "webapp")
        .extracting(ImageConfiguration::getBuildConfiguration)
        .hasFieldOrPropertyWithValue("tags", Collections.singletonList("latest"))
        .hasFieldOrPropertyWithValue("ports", Collections.singletonList("8080"))
        .hasFieldOrPropertyWithValue("env", Collections.singletonMap("DEPLOY_DIR", "/deployments"))
        .extracting(BuildConfiguration::getAssembly)
        .hasFieldOrPropertyWithValue("excludeFinalOutputArtifact", true)
        .extracting("inline.files").asList().extracting("destName")
        .containsExactly("ROOT.war");
  }

  @Test
  public void customizeWithOverriddenPropertiesShouldAddImageConfiguration() throws IOException {
    // Given
    final List<ImageConfiguration> originalImageConfigurations = new ArrayList<>();
    final File buildDirectory = temporaryFolder.newFolder("build");
    final File artifactFile = new File(buildDirectory, "artifact.war");
    assertTrue(artifactFile.createNewFile());
    final Properties projectProperties = new Properties();
    projectProperties.put("jkube.generator.webapp.targetDir", "/other-dir");
    projectProperties.put("jkube.generator.webapp.user", "root");
    projectProperties.put("jkube.generator.webapp.cmd", "sleep 3600");
    projectProperties.put("jkube.generator.webapp.path", "/some-context");
    projectProperties.put("jkube.generator.webapp.ports", "8082,80");
    projectProperties.put("jkube.generator.webapp.supportsS2iBuild", "true");
    projectProperties.put("jkube.generator.from", "image-to-trigger-custom-app-server-handler");
    // @formatter:off
    new Expectations() {{
      generatorContext.getProject().getBuildDirectory(); result = buildDirectory;
      generatorContext.getProject().getBuildFinalName(); result = "artifact";
      generatorContext.getProject().getPackaging(); result = "war";
      generatorContext.getProject().getVersion(); result = "1.33.7-SNAPSHOT";
      generatorContext.getRuntimeMode(); result = RuntimeMode.OPENSHIFT;
      generatorContext.getStrategy(); result = JKubeBuildStrategy.s2i;
      generatorContext.getProject().getProperties(); result = projectProperties;
    }};
    // @formatter:on
    // When
    final List<ImageConfiguration> result = new WebAppGenerator(generatorContext)
        .customize(originalImageConfigurations, false);
    // Then
    assertThat(result)
        .isSameAs(originalImageConfigurations)
        .hasSize(1)
        .first()
        .hasFieldOrPropertyWithValue("name", "%a:%l")
        .hasFieldOrPropertyWithValue("alias", "webapp")
        .extracting(ImageConfiguration::getBuildConfiguration)
        .hasFieldOrPropertyWithValue("tags", Collections.singletonList("latest"))
        .hasFieldOrPropertyWithValue("ports", Arrays.asList("8082", "80"))
        .hasFieldOrPropertyWithValue("env", Collections.singletonMap("DEPLOY_DIR", "/other-dir"))
        .hasFieldOrPropertyWithValue("cmd.shell", "sleep 3600")
        .extracting(BuildConfiguration::getAssembly)
        .hasFieldOrPropertyWithValue("excludeFinalOutputArtifact", true)
        .hasFieldOrPropertyWithValue("user", "root")
        .extracting("inline.files").asList().extracting("destName")
        .containsExactly("some-context.war");
  }
}
