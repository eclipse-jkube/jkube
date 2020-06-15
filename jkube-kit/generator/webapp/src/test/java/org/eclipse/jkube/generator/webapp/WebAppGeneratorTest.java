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
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.kit.build.service.docker.ImageConfiguration;
import org.eclipse.jkube.kit.common.Plugin;

import mockit.Expectations;
import mockit.Mocked;
import org.eclipse.jkube.kit.config.image.build.OpenShiftBuildStrategy;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertSame;
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
    assertThat(result, equalTo(true));
  }

  @Test(expected = IllegalArgumentException.class)
  public void customizeDoesNotSupportS2iBuildShouldThrowException() {
    // Given
    // @formatter:off
    final Properties projectProperties = new Properties();
    projectProperties.put("jkube.generator.from", "image-to-trigger-custom-app-server-handler");
    new Expectations() {{
      generatorContext.getRuntimeMode(); result = RuntimeMode.OPENSHIFT;
      generatorContext.getStrategy(); result = OpenShiftBuildStrategy.s2i;
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
    assertThat(originalImageConfigurations, sameInstance(result));
    assertThat(result, hasSize(1));
    final ImageConfiguration imageConfiguration = result.iterator().next();
    assertThat(imageConfiguration.getName(), equalTo("%g/%a:%l"));
    assertThat(imageConfiguration.getAlias(), equalTo("webapp"));
    assertThat(imageConfiguration.getBuildConfiguration().getTags(), contains("latest"));
    assertThat(imageConfiguration.getBuildConfiguration().getAssembly().isExcludeFinalOutputArtifact(),
        equalTo(true));
  }
}
