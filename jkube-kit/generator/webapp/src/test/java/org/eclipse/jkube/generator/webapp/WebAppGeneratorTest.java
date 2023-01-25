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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.kit.common.Plugin;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.image.build.JKubeBuildStrategy;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WebAppGeneratorTest {

  @TempDir
  Path temporaryFolder;

  private GeneratorContext generatorContext;
  private Plugin plugin;

  @BeforeEach
  void setUp() {
    generatorContext = mock(GeneratorContext.class, RETURNS_DEEP_STUBS);
    plugin = mock(Plugin.class);
  }

  @Test
  void isApplicable_withMavenWarPlugin_shouldReturnTrue() {
    // Given
    when(plugin.getGroupId()).thenReturn("org.apache.maven.plugins");
    when(plugin.getArtifactId()).thenReturn("maven-war-plugin");
    when(generatorContext.getProject().getPlugins()).thenReturn(Collections.singletonList(plugin));

    // When
    final boolean result = new WebAppGenerator(generatorContext).isApplicable(Collections.emptyList());
    // Then
    assertThat(result).isTrue();
  }

  @Test
  void isApplicable_withGradleWarPlugin_shouldReturnTrue() {
    // Given
    when(generatorContext.getProject().getGradlePlugins()).thenReturn(Collections.singletonList("org.gradle.api.plugins.WarPlugin"));

    // When
    final boolean result = new WebAppGenerator(generatorContext).isApplicable(Collections.emptyList());
    // Then
    assertThat(result).isTrue();
  }

  @Nested
  @DisplayName("customize (assembly)")
  class CustomizeAssembly {
    @Test
    @DisplayName("with no support of s2i, should throw exception")
    void withNoSupportS2iBuild_shouldThrowException() {
      // Given
      final Properties projectProperties = new Properties();
      projectProperties.put("jkube.generator.from", "image-to-trigger-custom-app-server-handler");
      when(generatorContext.getRuntimeMode()).thenReturn(RuntimeMode.OPENSHIFT);
      when(generatorContext.getStrategy()).thenReturn(JKubeBuildStrategy.s2i);
      when(generatorContext.getProject().getProperties()).thenReturn(projectProperties);

      // When & Then
      assertThatIllegalArgumentException()
          .isThrownBy(() -> new WebAppGenerator(generatorContext).customize(Collections.emptyList(), false))
          .withMessageStartingWith("S2I not yet supported for the webapp-generator");
    }

    @Test
    @DisplayName("with default handler, should add image configuration")
    void withDefaultHandler_shouldAddImageConfiguration() throws IOException {
      // Given
      final List<ImageConfiguration> originalImageConfigurations = new ArrayList<>();
      final Path buildDirectory = Files.createDirectory(temporaryFolder.resolve("build"));
      Files.createFile(buildDirectory.resolve("artifact.war"));

      when(generatorContext.getProject().getBuildDirectory()).thenReturn(buildDirectory.toFile());
      when(generatorContext.getProject().getBuildFinalName()).thenReturn("artifact");
      when(generatorContext.getProject().getPackaging()).thenReturn("war");
      when(generatorContext.getProject().getVersion()).thenReturn("1.33.7-SNAPSHOT");

      // When
      final List<ImageConfiguration> result = new WebAppGenerator(generatorContext)
          .customize(originalImageConfigurations, false);
      // Then
      assertThat(result)
          .isSameAs(originalImageConfigurations)
          .singleElement()
          .hasFieldOrPropertyWithValue("name", "%g/%a:%l")
          .hasFieldOrPropertyWithValue("alias", "webapp")
          .extracting(ImageConfiguration::getBuildConfiguration)
          .hasFieldOrPropertyWithValue("tags", Collections.singletonList("latest"))
          .hasFieldOrPropertyWithValue("ports", Collections.singletonList("8080"))
          .hasFieldOrPropertyWithValue("env", new HashMap<String, String>() {{
              put("DEPLOY_DIR", "/deployments");
              put("TOMCAT_WEBAPPS_DIR", "webapps-javaee");
            }})
          .extracting(BuildConfiguration::getAssembly)
          .hasFieldOrPropertyWithValue("excludeFinalOutputArtifact", true)
          .extracting("inline.files").asList().extracting("destName")
          .containsExactly("ROOT.war");
    }

    @Test
    @DisplayName("with overridden properties, should add image configuration")
    void withOverriddenProperties_shouldAddImageConfiguration() throws IOException {
      // Given
      final List<ImageConfiguration> originalImageConfigurations = new ArrayList<>();
      final Path buildDirectory = Files.createDirectory(temporaryFolder.resolve("build"));
      Files.createFile(buildDirectory.resolve("artifact.war"));
      final Properties projectProperties = new Properties();
      projectProperties.put("jkube.generator.webapp.targetDir", "/other-dir");
      projectProperties.put("jkube.generator.webapp.user", "root");
      projectProperties.put("jkube.generator.webapp.cmd", "sleep 3600");
      projectProperties.put("jkube.generator.webapp.path", "/some-context");
      projectProperties.put("jkube.generator.webapp.ports", "8082,80");
      projectProperties.put("jkube.generator.webapp.supportsS2iBuild", "true");
      projectProperties.put("jkube.generator.from", "image-to-trigger-custom-app-server-handler");

      when(generatorContext.getProject().getBuildDirectory()).thenReturn(buildDirectory.toFile());
      when(generatorContext.getProject().getBuildFinalName()).thenReturn("artifact");
      when(generatorContext.getProject().getPackaging()).thenReturn("war");
      when(generatorContext.getProject().getVersion()).thenReturn("1.33.7-SNAPSHOT");
      when(generatorContext.getRuntimeMode()).thenReturn(RuntimeMode.OPENSHIFT);
      when(generatorContext.getStrategy()).thenReturn(JKubeBuildStrategy.s2i);
      when(generatorContext.getProject().getProperties()).thenReturn(projectProperties);
      // When
      final List<ImageConfiguration> result = new WebAppGenerator(generatorContext)
          .customize(originalImageConfigurations, false);
      // Then
      assertThat(result)
          .isSameAs(originalImageConfigurations)
          .singleElement()
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

    @Test
    @DisplayName("with configured from, target directory and no command, should not initialize invalid command argument")
    void withFromTargetDirConfiguredAndNoCmd_shouldNotInitializeInvalidCmdArgument() throws IOException {
      // Given
      final List<ImageConfiguration> originalImageConfigurations = new ArrayList<>();
      final Path buildDirectory = Files.createDirectory(temporaryFolder.resolve("build"));
      Files.createFile(buildDirectory.resolve("artifact.war"));
      final Properties projectProperties = new Properties();
      projectProperties.put("jkube.generator.webapp.targetDir", "/usr/local/tomcat/webapps");
      projectProperties.put("jkube.generator.webapp.from", "tomcat:jdk11-openjdk-slim");

      when(generatorContext.getProject().getBuildDirectory()).thenReturn(buildDirectory.toFile());
      when(generatorContext.getProject().getBuildFinalName()).thenReturn("artifact");
      when(generatorContext.getProject().getPackaging()).thenReturn("war");
      when(generatorContext.getProject().getVersion()).thenReturn("1.33.7-SNAPSHOT");
      when(generatorContext.getProject().getProperties()).thenReturn(projectProperties);
      // When
      final List<ImageConfiguration> result = new WebAppGenerator(generatorContext)
          .customize(originalImageConfigurations, false);
      // Then
      assertThat(result)
          .isSameAs(originalImageConfigurations)
          .singleElement()
          .hasFieldOrPropertyWithValue("name", "%g/%a:%l")
          .extracting(ImageConfiguration::getBuildConfiguration)
          .hasFieldOrPropertyWithValue("from", "tomcat:jdk11-openjdk-slim")
          .hasFieldOrPropertyWithValue("env", Collections.singletonMap("DEPLOY_DIR", "/usr/local/tomcat/webapps"))
          .hasFieldOrPropertyWithValue("cmd", null);
    }
  }

  @Nested
  @DisplayName("extract env variables")
  class ExtractEnvVariables {

    @Test
    @DisplayName("with multiple env variables, should extract all of them")
    void withMultipleEnvVariables_shouldExtractThemAll() {
      // Given
      String envConfig = "GALLEON_PROVISION_LAYERS=web-server,ejb-lite,jsf,jpa,h2-driver\n" + //
          "\t\t\t\t\t\t\t IMAGE_STREAM_NAMESPACE=myproject";
      // when
      Map<String, String> extractedVariables = WebAppGenerator.extractEnvVariables(envConfig);
      // then
      assertThat(extractedVariables)
          .containsEntry("GALLEON_PROVISION_LAYERS", "web-server,ejb-lite,jsf,jpa,h2-driver")
          .containsEntry("IMAGE_STREAM_NAMESPACE", "myproject");
    }

    @Test
    @DisplayName("with single env variable, should extract it")
    void withSingleEnvVariable_shouldExtractIt() {
      // Given
      String envConfig = "GALLEON_PROVISION_LAYERS=web-server,ejb-lite,jsf,jpa,h2-driver";
      // when
      Map<String, String> extractedVariables = WebAppGenerator.extractEnvVariables(envConfig);
      // then
      assertThat(extractedVariables).containsEntry("GALLEON_PROVISION_LAYERS", "web-server,ejb-lite,jsf,jpa,h2-driver");
    }

    @Test
    @DisplayName("with multiple env variables with spaces, semicolon and comma, should extract all of them")
    void withMultipleEnvVariablesWithSpacesSemicolonAndCommas_shouldExtractThemAll() {
      // Given
      String envConfig = "ENV_WITH_SPACES=This is an environment variable with spaces\n" + //
          "               ENV_WITH_SEMICOLON=/path;/other/path\n" + //
          "               ENV_WITH_COMMAS=layer1,layer2";
      // when
      Map<String, String> extractedVariables = WebAppGenerator.extractEnvVariables(envConfig);
      // then
      assertThat(extractedVariables)
          .containsEntry("ENV_WITH_SPACES", "This is an environment variable with spaces")
          .containsEntry("ENV_WITH_SEMICOLON", "/path;/other/path")
          .containsEntry("ENV_WITH_COMMAS", "layer1,layer2");
    }

    @Test
    @DisplayName("with null env, should be empty")
    void withNullEnv_shouldReturnAnEmptyMap() {
      // Given
      String envConfig = null;
      // when
      Map<String, String> extractedVariables = WebAppGenerator.extractEnvVariables(envConfig);
      // then
      assertThat(extractedVariables).isEmpty();
    }
  }
}
