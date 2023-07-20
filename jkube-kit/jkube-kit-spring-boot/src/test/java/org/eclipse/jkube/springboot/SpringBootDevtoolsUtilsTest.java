/*
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
package org.eclipse.jkube.springboot;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.eclipse.jkube.generator.javaexec.FatJarDetector;
import org.eclipse.jkube.kit.common.Dependency;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SpringBootDevtoolsUtilsTest {
  private KitLogger kitLogger;

  @TempDir
  private File temporaryFolder;

  @BeforeEach
  void setup() {
    kitLogger = spy(new KitLogger.SilentLogger());
  }

  @Test
  void ensureSpringDevToolSecretToken_whenNoTokenFound_thenAppendTokenAndThrowException() throws IOException {
    // Given
    JavaProject javaProject = createSpringBootJavaProject();

    // When
    boolean result = SpringBootDevtoolsUtils.ensureSpringDevToolSecretToken(javaProject);

    // Then
    assertThat(result).isFalse();
    Path srcApplicationProperties = temporaryFolder.toPath()
        .resolve("src").resolve("main").resolve("resources").resolve("application.properties");
    Path targetApplicationProperties = temporaryFolder.toPath()
        .resolve("target").resolve("classes").resolve("application.properties");
    assertThat(new String(Files.readAllBytes(srcApplicationProperties)))
        .contains("# Remote secret added by jkube-kit-plugin")
        .contains("spring.devtools.remote.secret=");
    assertThat(new String(Files.readAllBytes(targetApplicationProperties)))
        .contains("# Remote secret added by jkube-kit-plugin")
        .contains("spring.devtools.remote.secret=");
  }

  @Test
  void ensureSpringDevToolSecretToken_whenTokenAlreadyPresent_thenDoNothing() {
    // Given
    JavaProject javaProject = createSpringBootJavaProject().toBuilder()
        .compileClassPathElement(Objects.requireNonNull(getClass().getResource("/devtools-application-properties")).getPath())
        .build();

    // When
    boolean result = SpringBootDevtoolsUtils.ensureSpringDevToolSecretToken(javaProject);

    // Then
    assertThat(result).isTrue();
    verify(kitLogger, times(0)).verbose(anyString());
  }

  @Test
  void addDevToolsFilesToFatJar_whenNoFatJar_thenThrowException() {
    // Given
    JavaProject javaProject = createSpringBootJavaProject();

    // When + Then
    assertThatIllegalStateException()
        .isThrownBy(() -> SpringBootDevtoolsUtils.addDevToolsFilesToFatJar(javaProject, null))
        .withMessage("No fat jar built yet. Please ensure that the 'package' phase has run");
  }

  @Test
  void addDevToolsFilesToFatJar_whenFatJarButNoDevtools_thenThrowException() throws IOException {
    // Given
    File outputDir = new File(temporaryFolder, "target");
    File fatJar = new File(outputDir, "fat.jar");
    assertThat(outputDir.mkdir()).isTrue();
    assertThat(fatJar.createNewFile()).isTrue();
    JavaProject javaProject = createSpringBootJavaProject();
    FatJarDetector.Result fatJarDetectorResult = mock(FatJarDetector.Result.class);
    when(fatJarDetectorResult.getArchiveFile()).thenReturn(fatJar);

    // When
    assertThatIllegalStateException()
        .isThrownBy(() -> SpringBootDevtoolsUtils.addDevToolsFilesToFatJar(javaProject, fatJarDetectorResult))
        .withMessageContaining("Cannot find artifact spring-boot-devtools-2.7.2.jar within the resolved resources");
  }

  @Test
  void addDevToolsFilesToFatJar_whenFatJarButNoDevtoolsJarNotExist_thenThrowException() throws IOException {
    // Given
    File outputDir = new File(temporaryFolder, "target");
    File fatJar = new File(outputDir, "fat.jar");
    assertThat(outputDir.mkdir()).isTrue();
    assertThat(fatJar.createNewFile()).isTrue();
    FatJarDetector.Result fatJarDetectorResult = mock(FatJarDetector.Result.class);
    when(fatJarDetectorResult.getArchiveFile()).thenReturn(fatJar);
    JavaProject javaProject = createSpringBootJavaProject().toBuilder()
        .dependency(Dependency.builder()
            .groupId("org.springframework.boot")
            .artifactId("spring-boot-devtools")
            .version("2.7.2")
            .type("jar")
            .file(new File("i-dont-exist.jar"))
            .build())
        .build();

    // When
    assertThatIllegalStateException()
        .isThrownBy(() -> SpringBootDevtoolsUtils.addDevToolsFilesToFatJar(javaProject, fatJarDetectorResult))
        .withMessageContaining("devtools need to be included in repacked archive, please set <excludeDevtools> to false in plugin configuration");
  }

  @Test
  void addDevToolsFilesToFatJar_whenFatJar_thenFatJarUpdatedWithSpringBootDevtoolsJarAndApplicationProperties() throws IOException {
    // Given
    File outputDir = new File(temporaryFolder, "target");
    File outputClassesDir = new File(outputDir, "classes");
    File outputApplicationProperties = new File(outputClassesDir, "application.properties");
    File fatJar = new File(outputDir, "fat.jar");
    File devtoolsJar = new File(temporaryFolder, "spring-boot-devtools-2.7.2.jar");
    assertThat(outputDir.mkdir()).isTrue();
    assertThat(outputClassesDir.mkdir()).isTrue();
    assertThat(fatJar.createNewFile()).isTrue();
    assertThat(fatJar).isEmpty();
    createDummyJar(fatJar);
    assertThat(devtoolsJar.createNewFile()).isTrue();
    assertThat(outputApplicationProperties.createNewFile()).isTrue();
    JavaProject javaProject = createSpringBootJavaProject().toBuilder()
        .dependency(Dependency.builder()
            .groupId("org.springframework.boot")
            .artifactId("spring-boot-devtools")
            .version("2.7.2")
            .type("jar")
            .file(devtoolsJar)
            .build())
        .build();
    FatJarDetector.Result fatJarDetectorResult = mock(FatJarDetector.Result.class);
    when(fatJarDetectorResult.getArchiveFile()).thenReturn(fatJar);

    // When
    SpringBootDevtoolsUtils.addDevToolsFilesToFatJar(javaProject, fatJarDetectorResult);

    // When + Then
    assertThat(fatJar).isNotEmpty();
    try (JarFile jarFile = new JarFile(fatJar)) {
      assertThat(jarFile.getJarEntry("BOOT-INF/lib/spring-boot-devtools-2.7.2.jar")).isNotNull();
      assertThat(jarFile.getJarEntry("BOOT-INF/classes/application.properties")).isNotNull();
    }
  }

  private JavaProject createSpringBootJavaProject() {
    return JavaProject.builder()
        .baseDirectory(temporaryFolder)
        .outputDirectory(temporaryFolder.toPath().resolve("target").toFile())
        .dependency(Dependency.builder()
            .groupId("org.springframework.boot")
            .artifactId("spring-boot-web")
            .version("2.7.2")
            .build())
        .build();
  }

  private void createDummyJar(File jarFile) throws IOException {
    Manifest manifest = new Manifest();
    manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
    manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, "org.example.Foo");
    JarOutputStream jarOutputStream = new JarOutputStream(Files.newOutputStream(jarFile.toPath()), manifest);
    jarOutputStream.closeEntry();
  }
}
