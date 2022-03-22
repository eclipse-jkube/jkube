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
import java.util.Collections;
import java.util.List;
import java.util.Properties;

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
    assertThat(result).hasSize(1);
    final ImageConfiguration imageConfiguration = result.iterator().next();
    assertThat(imageConfiguration.getName()).isEqualTo("%g/%a:%l");
    assertThat(imageConfiguration.getAlias()).isEqualTo("karaf");
    final BuildConfiguration bc = imageConfiguration.getBuildConfiguration();
    assertThat(bc.getTags()).contains("latest");
    assertThat(bc.getPorts()).contains("8181", "8778");
    assertThat(bc.getEnv()).containsEntry("DEPLOYMENTS_DIR", "/deployments");
    assertThat(bc.getEnv()).containsEntry("KARAF_HOME", "/deployments/karaf");
    final AssemblyConfiguration ac = bc.getAssembly();
    assertThat(ac.getName()).isEqualTo("deployments");
    assertThat(ac.isExcludeFinalOutputArtifact()).isFalse();
    assertThat(ac.getLayers()).hasSize(1);
    assertThat(ac.getLayers().iterator().next().getFileSets())
            .hasSize(2)
            .first()
            .hasFieldOrPropertyWithValue("directory", new File(temporaryFolder.getRoot(), "assembly"))
            .hasFieldOrPropertyWithValue("outputDirectory", new File("karaf"))
            .hasFieldOrPropertyWithValue("directoryMode", "0775");

    assertThat(ac.getLayers().iterator().next().getFileSets())
            .last()
            .hasFieldOrPropertyWithValue("directory", temporaryFolder.getRoot().toPath().resolve("assembly").resolve("bin").toFile())
            .hasFieldOrPropertyWithValue("outputDirectory", new File("karaf", "bin"))
            .hasFieldOrPropertyWithValue("fileMode", "0777")
            .hasFieldOrPropertyWithValue("directoryMode", "0775");
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