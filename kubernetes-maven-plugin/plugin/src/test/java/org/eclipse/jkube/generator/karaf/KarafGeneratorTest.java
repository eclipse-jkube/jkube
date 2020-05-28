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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.kit.build.service.docker.ImageConfiguration;
import org.eclipse.jkube.kit.common.Plugin;
import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;

import mockit.Expectations;
import mockit.Mocked;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.sameInstance;


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
      plugin.getGroupId(); result = "org.apache.karaf.or.any.other.groupid"; minTimes = 0;
      plugin.getArtifactId(); result = "karaf-maven-plugin";
      generatorContext.getProject().getPlugins(); result = Collections.singletonList(plugin);
    }};
    // @formatter:on
    // When
    final boolean result = new KarafGenerator(generatorContext).isApplicable(Collections.emptyList());
    // Then
    assertThat(result, equalTo(true));
  }

  @Test
  public void isApplicableHasNotKarafMavenPluginShouldReturnFalse(@Mocked Plugin plugin) {
    // Given
    // @formatter:off
    new Expectations() {{
      plugin.getGroupId(); result = "org.apache.karaf.tooling"; minTimes = 0;
      plugin.getArtifactId(); result = "not-karaf-maven-plugin";
      generatorContext.getProject().getPlugins(); result = Collections.singletonList(plugin);
    }};
    // @formatter:on
    // When
    final boolean result = new KarafGenerator(generatorContext).isApplicable(Collections.emptyList());
    // Then
    assertThat(result, equalTo(false));
  }

  @Test
  public void customizeWithKarafMavenPluginShouldAddImageConfiguration(@Mocked Plugin plugin) throws IOException {
    // Given
    final List<ImageConfiguration> originalImageConfigurations = new ArrayList<>();
    // @formatter:off
    new Expectations() {{
      plugin.getGroupId(); result = "org.apache.karaf.tooling"; minTimes = 0;
      plugin.getArtifactId(); result = "karaf-maven-plugin"; minTimes = 0;
      generatorContext.getProject().getPlugins(); result = Collections.singletonList(plugin);
      generatorContext.getProject().getBuildDirectory(); result = temporaryFolder.getRoot();
      generatorContext.getProject().getVersion(); result = "1.33.7-SNAPSHOT";
    }};
    // @formatter:on
    // When
    final List<ImageConfiguration> result = new KarafGenerator(generatorContext)
        .customize(originalImageConfigurations, false);
    // Then
    assertThat(originalImageConfigurations, sameInstance(result));
    assertThat(result, hasSize(1));
    final ImageConfiguration imageConfiguration = result.iterator().next();
    assertThat(imageConfiguration.getName(), equalTo("%g/%a:%l"));
    assertThat(imageConfiguration.getAlias(), equalTo("karaf"));
    final BuildConfiguration bc = imageConfiguration.getBuildConfiguration();
    assertThat(bc.getTags(), contains("latest"));
    assertThat(bc.getPorts(), contains("8181"));
    assertThat(bc.getEnv(), hasEntry("DEPLOYMENTS_DIR", "/deployments"));
    assertThat(bc.getEnv(), hasEntry("KARAF_HOME", "/deployments/karaf"));
    final AssemblyConfiguration ac = bc.getAssemblyConfiguration();
    assertThat(ac.getName(), equalTo("deployments"));
    assertThat(ac.isExcludeFinalOutputArtifact(), equalTo(false));
    assertThat(ac.getInline().getFileSets(), contains(
        allOf(
            hasProperty("directory", equalTo(new File(temporaryFolder.getRoot(), "assembly"))),
            hasProperty("outputDirectory", equalTo(new File("karaf"))),
            hasProperty("directoryMode", equalTo("0775"))
        ),
        allOf(
            hasProperty("directory", equalTo(temporaryFolder.getRoot().toPath().resolve("assembly").resolve("bin").toFile())),
            hasProperty("outputDirectory", equalTo(new File("karaf", "bin"))),
            hasProperty("fileMode", equalTo("0777")),
            hasProperty("directoryMode", equalTo("0775"))
        )
    ));
  }
}