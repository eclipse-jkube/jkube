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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class WebAppGeneratorTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Mock(answer = RETURNS_DEEP_STUBS)
  private GeneratorContext generatorContext;

  @Mock
  Plugin plugin;

  @Test
  public void isApplicableHasMavenWarPluginShouldReturnTrue() {
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
  public void isApplicableHasGradleWarPluginShouldReturnTrue() {
    // Given
    when(generatorContext.getProject().getGradlePlugins()).thenReturn(Collections.singletonList("org.gradle.api.plugins.WarPlugin"));

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
    when(generatorContext.getRuntimeMode()).thenReturn(RuntimeMode.OPENSHIFT);
    when(generatorContext.getStrategy()).thenReturn(JKubeBuildStrategy.s2i);
    when(generatorContext.getProject().getProperties()).thenReturn(projectProperties);

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

    when(generatorContext.getProject().getBuildDirectory()).thenReturn(buildDirectory);
    when(generatorContext.getProject().getBuildFinalName()).thenReturn("artifact");
    when(generatorContext.getProject().getPackaging()).thenReturn("war");
    when(generatorContext.getProject().getVersion()).thenReturn("1.33.7-SNAPSHOT");

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
        .hasFieldOrPropertyWithValue("env", new HashMap<String, String>() {
          {
            put("DEPLOY_DIR", "/deployments");
            put("TOMCAT_WEBAPPS_DIR", "webapps-javaee");
          }
        })
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

    when( generatorContext.getProject().getBuildDirectory()).thenReturn(buildDirectory);
    when( generatorContext.getProject().getBuildFinalName()).thenReturn("artifact");
    when( generatorContext.getProject().getPackaging()).thenReturn("war");
    when( generatorContext.getProject().getVersion()).thenReturn("1.33.7-SNAPSHOT");
    when( generatorContext.getRuntimeMode()).thenReturn(RuntimeMode.OPENSHIFT);
    when( generatorContext.getStrategy()).thenReturn(JKubeBuildStrategy.s2i);
    when( generatorContext.getProject().getProperties()).thenReturn(projectProperties);
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

  @Test
  public void customize_withFromTargetDirConfiguredAndNoCmd_shouldNotInitializeInvalidCmdArgument() throws IOException {
    // Given
    final List<ImageConfiguration> originalImageConfigurations = new ArrayList<>();
    final File buildDirectory = temporaryFolder.newFolder("build");
    final File artifactFile = new File(buildDirectory, "artifact.war");
    assertTrue(artifactFile.createNewFile());
    final Properties projectProperties = new Properties();
    projectProperties.put("jkube.generator.webapp.targetDir", "/usr/local/tomcat/webapps");
    projectProperties.put("jkube.generator.webapp.from", "tomcat:jdk11-openjdk-slim");

    when(generatorContext.getProject().getBuildDirectory()).thenReturn(buildDirectory);
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
        .hasSize(1)
        .first()
        .hasFieldOrPropertyWithValue("name", "%g/%a:%l")
        .extracting(ImageConfiguration::getBuildConfiguration)
        .hasFieldOrPropertyWithValue("from", "tomcat:jdk11-openjdk-slim")
        .hasFieldOrPropertyWithValue("env", Collections.singletonMap("DEPLOY_DIR", "/usr/local/tomcat/webapps"))
        .hasFieldOrPropertyWithValue("cmd", null);
  }

  @Test
  public void extractEnvVariables_withMultipleEnvVariable_shouldExtractThemAll() {
    // Given
    String envConfig = "GALLEON_PROVISION_LAYERS=web-server,ejb-lite,jsf,jpa,h2-driver\n" + //
        "\t\t\t\t\t\t\t IMAGE_STREAM_NAMESPACE=myproject";
    // when
    Map<String, String> extractedVariables = WebAppGenerator.extractEnvVariables(envConfig);
    // then
    assertEquals("web-server,ejb-lite,jsf,jpa,h2-driver", extractedVariables.get("GALLEON_PROVISION_LAYERS"));
    assertEquals("myproject", extractedVariables.get("IMAGE_STREAM_NAMESPACE"));
  }

  @Test
  public void extractEnvVariables_withSingleEnvVariable_shouldExtractIt() {
    // Given
    String envConfig = "GALLEON_PROVISION_LAYERS=web-server,ejb-lite,jsf,jpa,h2-driver";
    // when
    Map<String, String> extractedVariables = WebAppGenerator.extractEnvVariables(envConfig);
    // then
    assertEquals("web-server,ejb-lite,jsf,jpa,h2-driver", extractedVariables.get("GALLEON_PROVISION_LAYERS"));
  }

  @Test
  public void extractEnvVariables_withMultipleEnvVariableWithSpacesSemicolonAndCommas_shouldExtractThemAll() {
    // Given
    String envConfig = "ENV_WITH_SPACES=This is an environment variable with spaces\n" + //
        "               ENV_WITH_SEMICOLON=/path;/other/path\n" + //
        "               ENV_WITH_COMMAS=layer1,layer2";
    // when
    Map<String, String> extractedVariables = WebAppGenerator.extractEnvVariables(envConfig);
    // then
    assertEquals("This is an environment variable with spaces", extractedVariables.get("ENV_WITH_SPACES"));
    assertEquals("/path;/other/path", extractedVariables.get("ENV_WITH_SEMICOLON"));
    assertEquals("layer1,layer2", extractedVariables.get("ENV_WITH_COMMAS"));
  }

  @Test
  public void extractEnvVariables_withEnvNull_shouldReturnAnEmptyMap() {
    // Given
    String envConfig = null;
    // when
    Map<String, String> extractedVariables = WebAppGenerator.extractEnvVariables(envConfig);
    // then
    assertTrue(extractedVariables.isEmpty());
  }

}
