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
package org.eclipse.jkube.kit.build.api.assembly;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.jkube.kit.common.Assembly;
import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.common.AssemblyFile;
import org.eclipse.jkube.kit.common.AssemblyFileEntry;
import org.eclipse.jkube.kit.common.AssemblyFileSet;
import org.eclipse.jkube.kit.config.image.build.Arguments;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;

import mockit.Expectations;
import mockit.Injectable;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jkube.kit.build.api.assembly.AssemblyConfigurationUtils.createDockerFileBuilder;
import static org.eclipse.jkube.kit.build.api.assembly.AssemblyConfigurationUtils.getAssemblyConfigurationOrCreateDefault;
import static org.eclipse.jkube.kit.build.api.assembly.AssemblyConfigurationUtils.getJKubeAssemblyFileSets;
import static org.eclipse.jkube.kit.build.api.assembly.AssemblyConfigurationUtils.getJKubeAssemblyFiles;

public class AssemblyConfigurationUtilsTest {

  @Test
  public void getAssemblyConfigurationOrCreateDefaultNoConfigurationShouldReturnDefault(
          @Injectable final BuildConfiguration buildConfiguration) {

    // Given
    // @formatter:off
    new Expectations() {{
      buildConfiguration.getAssembly(); result = null;
    }};
    // @formatter:on
    // When
    final AssemblyConfiguration result = getAssemblyConfigurationOrCreateDefault(buildConfiguration);
    // Then
    assertThat(result)
        .hasFieldOrPropertyWithValue("name", "maven")
        .hasFieldOrPropertyWithValue("targetDir", "/maven")
        .hasFieldOrPropertyWithValue("user", null);
  }

  @Test
  public void getAssemblyConfigurationOrCreateDefaultWithConfigurationShouldReturnConfiguration(
          @Injectable final BuildConfiguration buildConfiguration) {

    // Given
    final AssemblyConfiguration configuration = AssemblyConfiguration.builder().user("OtherUser").name("ImageName").build();
    // @formatter:off
    new Expectations() {{
      buildConfiguration.getAssembly(); result = configuration;
    }};
    // @formatter:on
    // When
    final AssemblyConfiguration result = getAssemblyConfigurationOrCreateDefault(buildConfiguration);
    // Then
    assertThat(result)
        .hasFieldOrPropertyWithValue("name", "ImageName")
        .hasFieldOrPropertyWithValue("targetDir", "/ImageName")
        .hasFieldOrPropertyWithValue("user", "OtherUser");
  }

  @Test
  public void getJKubeAssemblyFileSetsNullShouldReturnEmptyList() {
    // When
    final List<AssemblyFileSet> result = getJKubeAssemblyFileSets(null);
    // Then
    assertThat(result).isNotNull().isEmpty();
  }

  @Test
  public void getJKubeAssemblyFileSetsNullFileSetsShouldReturnEmptyList() {
    // Given
    final Assembly assembly = new Assembly();
    // When
    final List<AssemblyFileSet> result = getJKubeAssemblyFileSets(assembly);
    // Then
    assertThat(result).isNotNull().isEmpty();
  }

  @Test
  public void getJKubeAssemblyFileSetsNotNullShouldReturnFileSets(
      @Injectable Assembly assembly, @Injectable AssemblyFileSet fileSet) {

    // Given
    // @formatter:off
    new Expectations() {{
      assembly.getFileSets(); result = Collections.singletonList(fileSet);
      fileSet.getDirectory(); result = "1337";
    }};
    // @formatter:on
    // When
    final List<AssemblyFileSet> result = getJKubeAssemblyFileSets(assembly);
    // Then
    assertThat(result)
        .isNotNull()
        .hasSize(1).first()
        .hasFieldOrPropertyWithValue("directory.name", "1337");
  }

  @Test
  public void getJKubeAssemblyFilesNullShouldReturnEmptyList() {
    // When
    final List<AssemblyFile> result = getJKubeAssemblyFiles(null);
    // Then
    assertThat(result).isNotNull().isEmpty();
  }

  @Test
  public void getJKubeAssemblyFilesNullFilesShouldReturnEmptyList() {
    // Given
    final Assembly assembly = new Assembly();
    // When
    final List<AssemblyFile> result = getJKubeAssemblyFiles(assembly);
    // Then
    assertThat(result).isNotNull().isEmpty();
  }

  @Test
  public void getJKubeAssemblyFilesNotNullShouldReturnFiles(
      @Injectable Assembly assembly, @Injectable AssemblyFile file) {

    // Given
    // @formatter:off
    new Expectations() {{
      assembly.getFiles(); result = Collections.singletonList(file);
      file.getSource(); result = new File("1337");
    }};
    // @formatter:on
    // When
    final List<AssemblyFile> result = getJKubeAssemblyFiles(assembly);
    // Then
    assertThat(result)
        .isNotNull()
        .hasSize(1).first()
        .hasFieldOrPropertyWithValue("source.name", "1337");
  }

  @Test
  public void createDockerFileBuilder_withEmptyBuildConfigurationNoAssembly_shouldReturnOnlyBase() {
    // Given
    final BuildConfiguration buildConfig = BuildConfiguration.builder().build();
    // When
    final String result = createDockerFileBuilder(buildConfig, null, null).content();
    // Then
    assertThat(result)
        .doesNotContain("COPY", "VOLUME")
        .isEqualTo("FROM busybox\n");
  }

  @Test
  public void createDockerFileBuilder_withNoAssembly_shouldReturnTransformedContent() {
    // Given
    final BuildConfiguration buildConfig = BuildConfiguration.builder()
        .putEnv("ENV_VAR", "VALUE")
        .label("LABEL", "LABEL_VALUE")
        .port("8080")
        .user("1000")
        .volume("VOLUME")
        .runCmd("chown -R 1000:1000 /opt")
        .maintainer("Aitana")
        .cmd(Arguments.builder().execArgument("sh").execArgument("-c").execArgument("server").build())
        .build();
    // When
    final String result = createDockerFileBuilder(buildConfig, null, null).content();
    // Then
    assertThat(result)
        .isEqualTo("FROM busybox\n" +
            "MAINTAINER Aitana\n" +
            "ENV ENV_VAR=VALUE\n" +
            "LABEL LABEL=LABEL_VALUE\n" +
            "EXPOSE 8080\n" +
            "RUN chown -R 1000:1000 /opt\n" +
            "VOLUME [\"VOLUME\"]\n" +
            "CMD [\"sh\",\"-c\",\"server\"]\n"+
            "USER 1000\n");
  }

  @Test
  public void createDockerFileBuilder_withAssemblyAndFiles_shouldReturnTransformedContent() {
    // Given
    final BuildConfiguration buildConfig = BuildConfiguration.builder()
        .putEnv("ENV_VAR", "VALUE")
        .label("LABEL", "LABEL_VALUE")
        .port("8080")
        .user("1000")
        .volume("VOLUME")
        .runCmd("chown -R 1000:1000 /opt")
        .maintainer("Alex")
        .cmd(Arguments.builder().execArgument("sh").execArgument("-c").execArgument("server").build())
        .build();
    final AssemblyConfiguration assemblyConfiguration = AssemblyConfiguration.builder()
        .targetDir("/deployments")
        .layer(Assembly.builder().id("layer-with-id").build())
        .layer(Assembly.builder().build())
        .build();
    final Map<Assembly, List<AssemblyFileEntry>> layers = assemblyConfiguration.getLayers().stream().collect(
        Collectors.toMap(Function.identity(), a ->Collections.singletonList(
            new AssemblyFileEntry(new File(""), new File(""), null))));
    // When
    final String result = createDockerFileBuilder(buildConfig, assemblyConfiguration, layers).content();
    // Then
    assertThat(result)
        .isEqualTo("FROM busybox\n" +
            "MAINTAINER Alex\n" +
            "ENV ENV_VAR=VALUE\n" +
            "LABEL LABEL=LABEL_VALUE\n" +
            "EXPOSE 8080\n" +
            "COPY /layer-with-id/deployments /deployments/\n" +
            "COPY /deployments /deployments/\n" +
            "RUN chown -R 1000:1000 /opt\n" +
            "VOLUME [\"/deployments\"]\n" +
            "VOLUME [\"VOLUME\"]\n" +
            "CMD [\"sh\",\"-c\",\"server\"]\n" +
            "USER 1000\n");
  }

  @Test
  public void createDockerFileBuilder_withAssemblyAndFilesInSingleLayer_shouldReturnTransformedContent() {
    // Given
    final BuildConfiguration buildConfig = BuildConfiguration.builder()
        .user("1000")
        .maintainer("Alex")
        .build();
    final AssemblyConfiguration assemblyConfiguration = AssemblyConfiguration.builder()
        .targetDir("/deployments")
        .layer(Assembly.builder().id("layer-with-id").build())
        .layer(new Assembly())
        .build();
    final Map<Assembly, List<AssemblyFileEntry>> layers = assemblyConfiguration.getLayers().stream().collect(
        Collectors.toMap(Function.identity(), a ->Collections.singletonList(
            new AssemblyFileEntry(new File(""), new File(""), null))));
    layers.put(new Assembly(), Collections.emptyList());
    // When
    final String result = createDockerFileBuilder(buildConfig, assemblyConfiguration, layers).content();
    // Then
    assertThat(result)
        .isEqualTo("FROM busybox\n" +
            "MAINTAINER Alex\n" +
            "COPY /layer-with-id/deployments /deployments/\n" +
            "VOLUME [\"/deployments\"]\n" +
            "USER 1000\n");
  }
}
