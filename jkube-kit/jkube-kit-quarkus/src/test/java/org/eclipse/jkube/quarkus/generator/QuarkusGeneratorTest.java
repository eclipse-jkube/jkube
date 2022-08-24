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
package org.eclipse.jkube.quarkus.generator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

import org.eclipse.jkube.generator.api.DefaultImageLookup;
import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.kit.common.Assembly;
import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.Plugin;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.image.build.JKubeBuildStrategy;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;

import mockit.Expectations;
import mockit.Mocked;
import org.apache.commons.io.FileUtils;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author jzuriaga
 */
class QuarkusGeneratorTest {

  private static final String BASE_JAVA_IMAGE = "java:latest";
  private static final String BASE_NATIVE_IMAGE = "fedora:latest";

  @TempDir
  Path temporaryFolder;

  @Mocked
  private DefaultImageLookup defaultImageLookup;

  @Mocked
  private GeneratorContext ctx;

  @Mocked
  private JavaProject project;

  private File baseDir;
  private ProcessorConfig config;
  private Properties projectProps;

  @BeforeEach
  void setUp() throws IOException {
    config = new ProcessorConfig();
    projectProps = new Properties();
    projectProps.put("jkube.generator.name", "quarkus");
    baseDir = Files.createDirectory(temporaryFolder.resolve("target")).toFile();
    // @formatter:off
    new Expectations() {{
      project.getVersion(); result = "0.0.1-SNAPSHOT"; minTimes = 0;
      project.getBaseDirectory(); result = baseDir; minTimes = 0;
      project.getBuildDirectory(); result = baseDir.getAbsolutePath(); minTimes = 0;
      project.getProperties(); result = projectProps;
      project.getCompileClassPathElements(); result = Collections.emptyList(); minTimes = 0;
      project.getOutputDirectory(); result = baseDir;
      ctx.getProject(); result = project;
      ctx.getConfig(); result = config;
      ctx.getStrategy(); result = JKubeBuildStrategy.s2i; minTimes = 0;
      defaultImageLookup.getImageName("java.upstream.docker"); result = "quarkus/docker";
      defaultImageLookup.getImageName("java.upstream.s2i"); result = "quarkus/s2i";
    }};
    // @formatter:on
  }

  @Test
  void isApplicable_withNoDependencies_shouldReturnFalse() {
    // When
    final boolean result = new QuarkusGenerator(ctx).isApplicable(new ArrayList<>());
    // Then
    assertThat(result).isFalse();
  }

  @ParameterizedTest(name = "With  ''{0}''  groupID and  ''{1}''  artifactID should be true")
  @MethodSource("isApplicableTestData")
  void isApplicable(String groupID, String artifactID) {
    // Given
    // @formatter:off
    new Expectations() {{
      project.getPlugins(); result = Collections.singletonList(Plugin.builder()
        .groupId(groupID)
        .artifactId(artifactID)
        .build());
    }};
    // @formatter:on
    // When
    final boolean result = new QuarkusGenerator(ctx).isApplicable(new ArrayList<>());
    // Then
    assertThat(result).isTrue();
  }

  public static Stream<Arguments> isApplicableTestData() {
    return Stream.of(
            Arguments.of("io.quarkus", "quarkus-maven-plugin"),
            Arguments.of("io.quarkus.platform", "quarkus-maven-plugin"),
            Arguments.of("com.redhat.quarkus.platform", "quarkus-maven-plugin"),
            Arguments.of("io.quarkus", "io.quarkus.gradle.plugin")
    );
  }

  @Test
  void customize_inOpenShift_shouldReturnS2iFrom() {
    // Given
    in(RuntimeMode.OPENSHIFT);
    // When
    final List<ImageConfiguration> result = new QuarkusGenerator(ctx).customize(new ArrayList<>(), true);
    // Then
    assertBuildFrom(result, "quarkus/s2i");
  }

  @Test
  void customize_inKubernetes_shouldReturnDockerFrom() {
    // Given
    in(RuntimeMode.KUBERNETES);
    // When
    final List<ImageConfiguration> result = new QuarkusGenerator(ctx).customize(new ArrayList<>(), true);
    // Then
    assertBuildFrom(result, "quarkus/docker");
  }

  @Test
  void customize_inOpenShift_shouldReturnNativeS2iFrom() throws IOException {
    // Given
    in(RuntimeMode.OPENSHIFT);
    setNativeConfig(false);
    // When
    final List<ImageConfiguration> resultImages = new QuarkusGenerator(ctx).customize(new ArrayList<>(), true);
    // Then
    assertBuildFrom(resultImages, "quay.io/quarkus/ubi-quarkus-native-binary-s2i:1.0");
  }

  @Test
  void customize_inKubernetes_shouldReturnNativeUbiFrom() throws IOException {
    // Given
    in(RuntimeMode.KUBERNETES);
    setNativeConfig(false);
    // When
    final List<ImageConfiguration> resultImages = new QuarkusGenerator(ctx).customize(new ArrayList<>(), true);
    // Then
    assertBuildFrom(resultImages, "registry.access.redhat.com/ubi8/ubi-minimal:8.6");
  }

  @Test
  void customize_withConfiguredImage_shouldReturnConfigured() {
    config.getConfig().put("quarkus", Collections.singletonMap("from", BASE_JAVA_IMAGE));
    QuarkusGenerator generator = new QuarkusGenerator(ctx);
    List<ImageConfiguration> existingImages = new ArrayList<>();

    List<ImageConfiguration> resultImages = generator.customize(existingImages, true);

    assertBuildFrom(resultImages, BASE_JAVA_IMAGE);
  }

  @Test
  void customize_withConfiguredNativeImage_shouldReturnConfiguredNative() throws IOException {
    setNativeConfig(false);
    config.getConfig().put("quarkus", Collections.singletonMap("from", BASE_NATIVE_IMAGE));

    QuarkusGenerator generator = new QuarkusGenerator(ctx);
    List<ImageConfiguration> existingImages = new ArrayList<>();

    List<ImageConfiguration> resultImages = generator.customize(existingImages, true);

    assertBuildFrom(resultImages, BASE_NATIVE_IMAGE);
  }

  @Test
  void customize_withConfiguredInProperties_shouldReturnConfigured() {
    projectProps.put("jkube.generator.quarkus.from", BASE_JAVA_IMAGE);

    QuarkusGenerator generator = new QuarkusGenerator(ctx);
    List<ImageConfiguration> existingImages = new ArrayList<>();

    List<ImageConfiguration> resultImages = generator.customize(existingImages, true);

    assertBuildFrom(resultImages, BASE_JAVA_IMAGE);
  }


  @Test
  void customize_withConfiguredNativeInProperties_shouldReturnConfiguredNative() throws IOException {
    setNativeConfig(false);
    projectProps.put("jkube.generator.quarkus.from", BASE_NATIVE_IMAGE);

    QuarkusGenerator generator = new QuarkusGenerator(ctx);
    List<ImageConfiguration> existingImages = new ArrayList<>();

    final List<ImageConfiguration> resultImages = generator.customize(existingImages, true);

    assertBuildFrom(resultImages, BASE_NATIVE_IMAGE);
  }

  @Test
  void isFatJar_withDefaults_shouldBeFalse() {
    // When
    final boolean result = new QuarkusGenerator(ctx).isFatJar();
    // Then
    assertThat(result).isFalse();
  }

  @Test
  void assembly_withFastJarInTarget_shouldReturnFastJarAssemblyInImage() throws IOException {
    // Given
    withFastJarInTarget(baseDir);
    // When
    final List<ImageConfiguration> resultImages = new QuarkusGenerator(ctx)
        .customize(new ArrayList<>(), false);
    // Then
    assertThat(resultImages)
        .isNotNull()
        .singleElement()
        .extracting(ImageConfiguration::getBuild)
        .extracting(BuildConfiguration::getAssembly)
        .hasFieldOrPropertyWithValue("targetDir", "/deployments")
        .hasFieldOrPropertyWithValue("excludeFinalOutputArtifact", true)
        .extracting(AssemblyConfiguration::getLayers)
        .asList().hasSize(2)
        .satisfies(layers -> assertThat(layers).first().asInstanceOf(InstanceOfAssertFactories.type(Assembly.class))
            .hasFieldOrPropertyWithValue("id", "lib")
            .extracting(Assembly::getFileSets)
            .asList().singleElement()
            .hasFieldOrPropertyWithValue("outputDirectory", new File("."))
            .extracting("includes").asList()
            .containsExactly("lib"))
        .satisfies(layers -> assertThat(layers).element(1).asInstanceOf(InstanceOfAssertFactories.type(Assembly.class))
            .hasFieldOrPropertyWithValue("id", "fast-jar")
            .extracting(Assembly::getFileSets)
            .asList().singleElement()
            .hasFieldOrPropertyWithValue("outputDirectory", new File("."))
            .hasFieldOrPropertyWithValue("excludes", Arrays.asList("lib/**/*", "lib/*"))
            .extracting("includes").asList()
            .containsExactly("quarkus-run.jar", "*", "**/*"));
  }

  @Test
  void assembly_manualNativeSettings_shouldReturnNativeAssemblyInImage() throws IOException {
    // Given
    setNativeConfig(false);
    // When
    final List<ImageConfiguration> resultImages = new QuarkusGenerator(ctx)
        .customize(new ArrayList<>(), false);
    // Then
    assertThat(resultImages)
        .isNotNull()
        .singleElement()
        .extracting(ImageConfiguration::getBuild)
        .extracting(BuildConfiguration::getAssembly)
        .hasFieldOrPropertyWithValue("targetDir", "/")
        .extracting(AssemblyConfiguration::getLayers)
        .asList().singleElement().asInstanceOf(InstanceOfAssertFactories.type(Assembly.class))
        .extracting(Assembly::getFileSets)
        .asList()
        .hasSize(1)
        .flatExtracting("includes")
        .containsExactly("sample-runner");
  }

  @Test
  void assembly_withNativeBinaryInTarget_shouldReturnNativeAssemblyInImage() throws IOException {
    // Given
    withNativeBinaryInTarget(baseDir);
    // When
    final List<ImageConfiguration> resultImages = new QuarkusGenerator(ctx)
        .customize(new ArrayList<>(), false);
    // Then
    assertThat(resultImages)
        .isNotNull()
        .singleElement()
        .extracting(ImageConfiguration::getBuild)
        .extracting(BuildConfiguration::getAssembly)
        .hasFieldOrPropertyWithValue("targetDir", "/")
        .extracting(AssemblyConfiguration::getLayers)
        .asList().singleElement().asInstanceOf(InstanceOfAssertFactories.type(Assembly.class))
        .extracting(Assembly::getFileSets)
        .asList()
        .hasSize(1)
        .flatExtracting("includes")
        .containsExactly("sample-runner");
  }

  @Test
  void assembly_withLegacyJarInTarget_shouldReturnDefaultAssemblyInImage() throws IOException {
    // Given
    withLegacyJarInTarget();
    // When
    final List<ImageConfiguration> resultImages = new QuarkusGenerator(ctx)
        .customize(new ArrayList<>(), false);
    // Then
    assertThat(resultImages)
        .isNotNull()
        .singleElement()
        .extracting(ImageConfiguration::getBuild)
        .extracting(BuildConfiguration::getAssembly)
        .hasFieldOrPropertyWithValue("targetDir", "/deployments")
        .hasFieldOrPropertyWithValue("excludeFinalOutputArtifact", true)
        .extracting(AssemblyConfiguration::getLayers)
        .asList().hasSize(2)
        .satisfies(layers -> assertThat(layers).first().asInstanceOf(InstanceOfAssertFactories.type(Assembly.class))
            .hasFieldOrPropertyWithValue("id", "lib")
            .extracting(Assembly::getFileSets)
            .asList()
            .hasSize(1)
            .flatExtracting("includes")
            .containsExactly("lib"))
        .satisfies(layers -> assertThat(layers).element(1).asInstanceOf(InstanceOfAssertFactories.type(Assembly.class))
            .hasFieldOrPropertyWithValue("id", "artifact")
            .extracting(Assembly::getFileSets)
            .asList()
            .hasSize(1)
            .flatExtracting("includes")
            .containsExactly("sample-legacy-runner.jar"));
  }

  @Test
  void assembly_withConfiguredPackagingAndNoJar_shouldThrowException() {
    // Given
    projectProps.put("quarkus.package.type", "legacy-jar");
    final QuarkusGenerator quarkusGenerator = new QuarkusGenerator(ctx);
    final List<ImageConfiguration> list = new ArrayList<>();
    // When & Then
    assertThatIllegalStateException()
        .isThrownBy(() -> quarkusGenerator.customize(list, false))
        .withMessageContaining("Can't find single file with suffix '-runner.jar'");
  }

  @Test
  void assembly_withUberJarInTarget_shouldReturnAssemblyWithSingleJar() throws IOException {
    // Given
//    projectProps.put("quarkus.package.type", "uber-jar");
    withUberJarInTarget();
    // When
    final List<ImageConfiguration> resultImages = new QuarkusGenerator(ctx)
        .customize(new ArrayList<>(), false);
    // Then
    assertThat(resultImages)
        .isNotNull()
        .singleElement()
        .extracting(ImageConfiguration::getBuild)
        .extracting(BuildConfiguration::getAssembly)
        .hasFieldOrPropertyWithValue("targetDir", "/deployments")
        .extracting(AssemblyConfiguration::getLayers)
        .asList().singleElement().asInstanceOf(InstanceOfAssertFactories.type(Assembly.class))
        .extracting(Assembly::getFileSets)
        .asList()
        .hasSize(1)
        .flatExtracting("includes")
        .containsExactly("sample-runner.jar");
  }

  @Test
  void assembly_withManualConfigAndFastJarAndLegacyInTarget_shouldReturnAssemblyForQuarkusAppInImage() throws IOException {
    // Given
    projectProps.put("quarkus.package.type", "fast-jar");
    withFastJarInTarget(baseDir);
    withLegacyJarInTarget();
    // When
    final List<ImageConfiguration> resultImages = new QuarkusGenerator(ctx)
        .customize(new ArrayList<>(), false);
    // Then
    assertThat(resultImages)
        .isNotNull()
        .singleElement()
        .extracting(ImageConfiguration::getBuild)
        .extracting(BuildConfiguration::getAssembly)
        .hasFieldOrPropertyWithValue("targetDir", "/deployments")
        .hasFieldOrPropertyWithValue("excludeFinalOutputArtifact", true)
        .extracting(AssemblyConfiguration::getLayers)
        .asList().hasSize(2)
        .satisfies(layers -> assertThat(layers).first().asInstanceOf(InstanceOfAssertFactories.type(Assembly.class))
            .hasFieldOrPropertyWithValue("id", "lib")
            .extracting(Assembly::getFileSets)
            .asList().singleElement()
            .hasFieldOrPropertyWithValue("outputDirectory", new File("."))
            .extracting("includes").asList()
            .containsExactly("lib"))
        .satisfies(layers -> assertThat(layers).element(1).asInstanceOf(InstanceOfAssertFactories.type(Assembly.class))
            .hasFieldOrPropertyWithValue("id", "fast-jar")
            .extracting(Assembly::getFileSets)
            .asList().singleElement()
            .hasFieldOrPropertyWithValue("outputDirectory", new File("."))
            .hasFieldOrPropertyWithValue("excludes", Arrays.asList("lib/**/*", "lib/*"))
            .extracting("includes").asList()
            .containsExactly("quarkus-run.jar", "*", "**/*"));
  }

  @Test
  void assembly_withManualFastJarConfigAndLegacyInTarget_shouldThrowException() throws IOException {
    // Given
    projectProps.put("quarkus.package.type", "fast-jar");
    withLegacyJarInTarget();
    final QuarkusGenerator qg = new QuarkusGenerator(ctx);
    final List<ImageConfiguration> configs = Collections.emptyList();
    // When & Then
    assertThatIllegalStateException()
        .isThrownBy(() -> qg.customize(configs, false))
        .withMessageContaining("The quarkus-app directory required in Quarkus Fast Jar mode was not found");
  }

  private void assertBuildFrom (List<ImageConfiguration> resultImages, String baseImage) {
    assertThat(resultImages)
        .isNotNull()
        .hasSize(1)
        .extracting("buildConfiguration.from")
        .containsExactly(baseImage);
  }

  protected static void withNativeBinaryInTarget(File targetDir) throws IOException {
    new File(targetDir, "sample-runner").createNewFile();
  }

  private void withUberJarInTarget() throws IOException {
    new File(baseDir, "sample-runner.jar").createNewFile();
  }

  private void withLegacyJarInTarget() throws IOException {
    new File(baseDir, "sample-legacy-runner.jar").createNewFile();
    final File lib = new File(baseDir, "lib");
    FileUtils.forceMkdir(lib);
    new File(lib, "dependency.jar");
  }

  protected static void withFastJarInTarget(File targetDir) throws IOException {
    final File quarkusApp = new File(targetDir, "quarkus-app");
    FileUtils.forceMkdir(quarkusApp);
    FileUtils.forceMkdir(new File(quarkusApp, "app"));
    FileUtils.forceMkdir(new File(quarkusApp, "lib"));
    FileUtils.forceMkdir(new File(quarkusApp, "quarkus"));
    new File(quarkusApp, "quarkus-run.jar").createNewFile();
  }

  private void in(RuntimeMode runtimeMode) {
    // @formatter:off
    new Expectations() {{
      ctx.getRuntimeMode(); result = runtimeMode;
    }};
    // @formatter:on
  }

  private void setNativeConfig (boolean fastJAR) throws IOException {
    createFakeNativeImage(fastJAR);
    projectProps.put("jkube.generator.quarkus.nativeImage", "true");
  }

  private void createFakeNativeImage (boolean fastJAR) throws IOException {
    File runnerExec = new File(
        fastJAR ? Files.createDirectories(temporaryFolder.resolve("target").resolve("quarkus-app")).toFile() : baseDir,
        "sample-runner");
    runnerExec.createNewFile();
  }

}
