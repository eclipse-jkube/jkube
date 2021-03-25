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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.eclipse.jkube.generator.api.DefaultImageLookup;
import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.kit.common.Assembly;
import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.image.build.JKubeBuildStrategy;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;

import mockit.Expectations;
import mockit.Mocked;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;
/**
 * @author jzuriaga
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
public class QuarkusGeneratorTest {

  private static final String BASE_JAVA_IMAGE = "java:latest";
  private static final String BASE_NATIVE_IMAGE = "fedora:latest";

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Mocked
  private DefaultImageLookup defaultImageLookup;

  @Mocked
  private GeneratorContext ctx;

  @Mocked
  private JavaProject project;

  private File baseDir;
  private ProcessorConfig config;
  private Properties projectProps;

  @Before
  public void setUp() throws IOException {
    config = new ProcessorConfig();
    projectProps = new Properties();
    projectProps.put("jkube.generator.name", "quarkus");
    baseDir = temporaryFolder.newFolder("target");
    createFakeRunnerJar();
    createFakeNativeImage();
    // @formatter:off
    new Expectations() {{
      project.getVersion(); result = "0.0.1-SNAPSHOT"; minTimes = 0;
      project.getBaseDirectory(); result = baseDir; minTimes = 0;
      project.getBuildDirectory(); result = baseDir.getAbsolutePath();
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
  public void customize_inOpenShift_shouldReturnS2iFrom() {
    // Given
    in(RuntimeMode.OPENSHIFT);
    // When
    final List<ImageConfiguration> result = new QuarkusGenerator(ctx).customize(new ArrayList<>(), true);
    // Then
    assertBuildFrom(result, "quarkus/s2i");
  }

  @Test
  public void customize_inKubernetes_shouldReturnDockerFrom() {
    // Given
    in(RuntimeMode.KUBERNETES);
    // When
    final List<ImageConfiguration> result = new QuarkusGenerator(ctx).customize(new ArrayList<>(), true);
    // Then
    assertBuildFrom(result, "quarkus/docker");
  }

  @Test
  public void customize_inOpenShift_shouldReturnNativeS2iFrom() throws IOException {
    // Given
    in(RuntimeMode.OPENSHIFT);
    setNativeConfig();
    // When
    final List<ImageConfiguration> resultImages = new QuarkusGenerator(ctx).customize(new ArrayList<>(), true);
    // Then
    assertBuildFrom(resultImages, "quay.io/quarkus/ubi-quarkus-native-binary-s2i:1.0");
  }

  @Test
  public void customize_inKubernetes_shouldReturnNativeUbiFrom() throws IOException {
    // Given
    in(RuntimeMode.KUBERNETES);
    setNativeConfig();
    // When
    final List<ImageConfiguration> resultImages = new QuarkusGenerator(ctx).customize(new ArrayList<>(), true);
    // Then
    assertBuildFrom(resultImages, "registry.access.redhat.com/ubi8/ubi-minimal:8.1");
  }

  @Test
  public void customize_withConfiguredImage_shouldReturnConfigured() {
    config.getConfig().put("quarkus", Collections.singletonMap("from", BASE_JAVA_IMAGE));
    QuarkusGenerator generator = new QuarkusGenerator(ctx);
    List<ImageConfiguration> existingImages = new ArrayList<>();

    List<ImageConfiguration> resultImages = generator.customize(existingImages, true);

    assertBuildFrom(resultImages, BASE_JAVA_IMAGE);
  }

  @Test
  public void customize_withConfiguredNativeImage_shouldReturnConfiguredNative() throws IOException {
    setNativeConfig();
    config.getConfig().put("quarkus", Collections.singletonMap("from", BASE_NATIVE_IMAGE));

    QuarkusGenerator generator = new QuarkusGenerator(ctx);
    List<ImageConfiguration> existingImages = new ArrayList<>();

    List<ImageConfiguration> resultImages = generator.customize(existingImages, true);

    assertBuildFrom(resultImages, BASE_NATIVE_IMAGE);
  }

  @Test
  public void customize_withConfiguredInProperties_shouldReturnConfigured() {
    projectProps.put("jkube.generator.quarkus.from", BASE_JAVA_IMAGE);

    QuarkusGenerator generator = new QuarkusGenerator(ctx);
    List<ImageConfiguration> existingImages = new ArrayList<>();

    List<ImageConfiguration> resultImages = generator.customize(existingImages, true);

    assertBuildFrom(resultImages, BASE_JAVA_IMAGE);
  }


  @Test
  public void customize_withConfiguredNativeInProperties_shouldReturnConfiguredNative() throws IOException {
    setNativeConfig();
    projectProps.put("jkube.generator.quarkus.from", BASE_NATIVE_IMAGE);

    QuarkusGenerator generator = new QuarkusGenerator(ctx);
    List<ImageConfiguration> existingImages = new ArrayList<>();

    final List<ImageConfiguration> resultImages = generator.customize(existingImages, true);

    assertBuildFrom(resultImages, BASE_NATIVE_IMAGE);
  }

  @Test
  public void assembly_withDefaults_shouldReturnDefaultAssemblyInImage() {
    // When
    final List<ImageConfiguration> resultImages = new QuarkusGenerator(ctx)
        .customize(new ArrayList<>(), false);
    // Then
    assertThat(resultImages)
        .isNotNull()
        .hasSize(1)
        .element(0)
        .extracting(ImageConfiguration::getBuild)
        .extracting(BuildConfiguration::getAssembly)
        .hasFieldOrPropertyWithValue("targetDir", "/deployments")
        .extracting(AssemblyConfiguration::getInline)
        .extracting(Assembly::getFileSets)
        .asList()
        .hasSize(1)
        .flatExtracting("includes")
        .containsExactlyInAnyOrder("lib", "sample-runner.jar");
  }

  @Test
  public void assembly_withDefaultsInNative_shouldReturnDefaultNativeAssemblyInImage() throws IOException {
    // Given
    setNativeConfig();
    // When
    final List<ImageConfiguration> resultImages = new QuarkusGenerator(ctx)
        .customize(new ArrayList<>(), false);
    // Then
    assertThat(resultImages)
        .isNotNull()
        .hasSize(1)
        .element(0)
        .extracting(ImageConfiguration::getBuild)
        .extracting(BuildConfiguration::getAssembly)
        .hasFieldOrPropertyWithValue("targetDir", "/")
        .extracting(AssemblyConfiguration::getInline)
        .extracting(Assembly::getFileSets)
        .asList()
        .hasSize(1)
        .flatExtracting("includes")
        .containsExactly("sample-runner");
  }

  @Test
  public void isFatJar_withDefaults_shouldBeFalse() {
    // When
    final boolean result = new QuarkusGenerator(ctx).isFatJar();
    // Then
    assertThat(result).isFalse();
  }

  private void assertBuildFrom (List<ImageConfiguration> resultImages, String baseImage) {
    assertThat(resultImages)
        .isNotNull()
        .hasSize(1)
        .extracting("buildConfiguration.from")
        .containsExactly(baseImage);
  }

  private void createFakeRunnerJar () throws IOException {
    File runnerJar = new File(baseDir, "sample-runner.jar");
    runnerJar.createNewFile();
  }

  private void in(RuntimeMode runtimeMode) {
    // @formatter:off
    new Expectations() {{
      ctx.getRuntimeMode(); result = runtimeMode;
    }};
    // @formatter:on
  }

  private void setNativeConfig () throws IOException {
    createFakeNativeImage();
    projectProps.put("jkube.generator.quarkus.nativeImage", "true");
  }

  private void createFakeNativeImage () throws IOException {
    File runnerExec = new File(baseDir, "sample-runner");
    runnerExec.createNewFile();
  }

}