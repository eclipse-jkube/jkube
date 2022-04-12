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
package org.eclipse.jkube.generator.karaf;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.common.Plugin;
import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;

import mockit.Expectations;
import mockit.Mocked;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.tuple;

public class KarafGeneratorTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();
  @Mocked
  private GeneratorContext generatorContext;

  @Test
  public void isApplicableHasKarafMavenPluginShouldReturnTrue(@Mocked Plugin plugin) {
    // Given
    // @formatter:off
    new Expectations() {{
      plugin.getGroupId();
      result = "org.apache.karaf.or.any.other.groupid";
      minTimes = 0;
      plugin.getArtifactId();
      result = "karaf-maven-plugin";
      generatorContext.getProject().getPlugins();
      result = Collections.singletonList(plugin);
    }};
    // @formatter:on
    // When
    final boolean result = new KarafGenerator(generatorContext).isApplicable(Collections.emptyList());
    // Then
    assertThat(result).isTrue();
  }

  @Test
  public void isApplicableHasNotKarafMavenPluginShouldReturnFalse(@Mocked Plugin plugin) {
    // Given
    // @formatter:off
    new Expectations() {{
      plugin.getGroupId();
      result = "org.apache.karaf.tooling";
      minTimes = 0;
      plugin.getArtifactId();
      result = "not-karaf-maven-plugin";
      generatorContext.getProject().getPlugins();
      result = Collections.singletonList(plugin);
    }};
    // @formatter:on
    // When
    final boolean result = new KarafGenerator(generatorContext).isApplicable(Collections.emptyList());
    // Then
    assertThat(result).isFalse();
  }

  @Test
  public void customizeWithKarafMavenPluginShouldAddImageConfiguration(@Mocked Plugin plugin) {
    // Given
    final List<ImageConfiguration> originalImageConfigurations = new ArrayList<>();
    // @formatter:off
    new Expectations() {{
      plugin.getGroupId(); result = "org.apache.karaf.tooling"; minTimes = 0;
      plugin.getArtifactId(); result = "karaf-maven-plugin"; minTimes = 0;
      generatorContext.getProject().getPlugins(); result = Collections.singletonList(plugin); minTimes = 0;
      generatorContext.getProject().getBuildDirectory(); result = temporaryFolder.getRoot();
      generatorContext.getProject().getVersion(); result = "1.33.7-SNAPSHOT";
      generatorContext.getConfig(); result = new ProcessorConfig();
    }};
    // @formatter:on
    // When
    final List<ImageConfiguration> result = new KarafGenerator(generatorContext)
        .customize(originalImageConfigurations, false);
    // Then
    assertThat(originalImageConfigurations).isSameAs(result);
    assertThat(result)
        .singleElement()
        .hasFieldOrPropertyWithValue("name", "%g/%a:%l")
        .hasFieldOrPropertyWithValue("alias", "karaf")
        .extracting(ImageConfiguration::getBuildConfiguration)
        .hasFieldOrPropertyWithValue("tags", Collections.singletonList("latest"))
        .hasFieldOrPropertyWithValue("ports", Arrays.asList("8181", "8778"))
        .extracting(BuildConfiguration::getEnv)
        .asInstanceOf(InstanceOfAssertFactories.MAP)
        .containsOnly(
            entry("DEPLOYMENTS_DIR", "/deployments"),
            entry("KARAF_HOME", "/deployments/karaf")
        );
    assertThat(result.iterator().next().getBuildConfiguration().getAssembly())
        .hasFieldOrPropertyWithValue("name", "deployments")
        .hasFieldOrPropertyWithValue("excludeFinalOutputArtifact", false)
        .extracting(AssemblyConfiguration::getLayers).asList()
        .singleElement()
        .extracting("fileSets").asList()
        .extracting("directory", "outputDirectory", "directoryMode", "fileMode")
        .containsExactly(
            tuple(
                new File(temporaryFolder.getRoot(), "assembly"),
                new File("karaf"),
                "0775",
                null),
            tuple(
                temporaryFolder.getRoot().toPath().resolve("assembly").resolve("bin").toFile(),
                new File("karaf", "bin"),
                "0775",
                "0777")
        );
  }

  @Test
  public void customizeWithKarafMavenPluginAndCustomConfigShouldAddImageConfiguration(@Mocked Plugin plugin) {
    // Given
    final List<ImageConfiguration> originalImageConfigurations = new ArrayList<>();
    Properties props = new Properties();
    props.put("jkube.generator.karaf.baseDir", "/other-dir");
    props.put("jkube.generator.karaf.webPort", "8080");
    // @formatter:off
    new Expectations() {{
      generatorContext.getProject().getBuildDirectory(); result = temporaryFolder.getRoot();
      generatorContext.getProject().getVersion(); result = "1.33.7-SNAPSHOT";
      generatorContext.getProject().getProperties(); result = props;
    }};
    // @formatter:on
    // When
    final List<ImageConfiguration> result = new KarafGenerator(generatorContext)
        .customize(originalImageConfigurations, false);
    // Then
    assertThat(result).hasSize(1);
    final ImageConfiguration imageConfiguration = result.iterator().next();
    assertThat(imageConfiguration.getBuildConfiguration().getPorts()).contains("8080", "8778");
    assertThat(imageConfiguration.getBuildConfiguration().getEnv()).containsEntry("DEPLOYMENTS_DIR","/other-dir");
  }
}
