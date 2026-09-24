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
package org.eclipse.jkube.springboot.generator;

import org.eclipse.jkube.generator.api.GeneratorConfig;
import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.kit.common.Assembly;
import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.common.AssemblyFileSet;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.springboot.SpringBootLayeredJar;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Function;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LayeredJarGenerator")
class LayeredJarGeneratorTest {

  @TempDir
  private Path tempDir;

  private File targetDir;
  private GeneratorContext generatorContext;
  private GeneratorConfig generatorConfig;

  @BeforeEach
  void setUp() throws IOException {
    File projectBaseDir = tempDir.toFile();
    targetDir = Files.createDirectory(tempDir.resolve("target")).toFile();

    Properties properties = new Properties();
    JavaProject project = JavaProject.builder()
      .baseDirectory(projectBaseDir)
      .buildDirectory(targetDir)
      .buildPackageDirectory(targetDir)
      .properties(properties)
      .version("1.0.0")
      .artifactId("test-app")
      .build();

    generatorContext = GeneratorContext.builder()
        .logger(new KitLogger.SilentLogger())
        .project(project)
        .build();

    generatorConfig = new GeneratorConfig(properties, "spring-boot", null);
  }

  @Nested
  @DisplayName("getEnv")
  class GetEnv {

    @Test
    @DisplayName("should set JAVA_MAIN_CLASS from layered jar manifest")
    void shouldSetJavaMainClassFromManifest() throws IOException {
      // Given
      File layeredJar = createLayeredJar("org.springframework.boot.loader.launch.JarLauncher");
      LayeredJarGenerator generator = new LayeredJarGenerator(generatorContext, generatorConfig, layeredJar);
      Function<Boolean, Map<String, String>> envSupplier = prePackage -> new HashMap<>();

      // When
      Map<String, String> env = generator.getEnv(envSupplier, false);

      // Then
      assertThat(env)
          .containsEntry("JAVA_MAIN_CLASS", "org.springframework.boot.loader.launch.JarLauncher");
    }

    @Test
    @DisplayName("should preserve existing environment variables")
    void shouldPreserveExistingEnvVars() throws IOException {
      // Given
      File layeredJar = createLayeredJar("org.springframework.boot.loader.JarLauncher");
      LayeredJarGenerator generator = new LayeredJarGenerator(generatorContext, generatorConfig, layeredJar);

      Map<String, String> existingEnv = new HashMap<>();
      existingEnv.put("JAVA_OPTIONS", "-Xmx512m");
      existingEnv.put("APP_PORT", "8080");
      Function<Boolean, Map<String, String>> envSupplier = prePackage -> existingEnv;

      // When
      Map<String, String> env = generator.getEnv(envSupplier, false);

      // Then
      assertThat(env)
          .containsEntry("JAVA_MAIN_CLASS", "org.springframework.boot.loader.JarLauncher")
          .containsEntry("JAVA_OPTIONS", "-Xmx512m")
          .containsEntry("APP_PORT", "8080");
    }

    @Test
    @DisplayName("with null main class, should set null JAVA_MAIN_CLASS")
    void withNullMainClass() throws IOException {
      // Given
      File layeredJar = createLayeredJarWithoutMainClass();
      LayeredJarGenerator generator = new LayeredJarGenerator(generatorContext, generatorConfig, layeredJar);
      Function<Boolean, Map<String, String>> envSupplier = prePackage -> new HashMap<>();

      // When
      Map<String, String> env = generator.getEnv(envSupplier, false);

      // Then
      assertThat(env)
          .containsEntry("JAVA_MAIN_CLASS", null);
    }
  }

  @Nested
  @DisplayName("createAssemblyConfiguration")
  class CreateAssemblyConfiguration {

    @Test
    @DisplayName("should create assembly with all Spring Boot layers")
    void shouldCreateAssemblyWithAllLayers() throws IOException {
      // Given
      File layeredJar = createRealLayeredJar();
      LayeredJarGenerator generator = new LayeredJarGenerator(generatorContext, generatorConfig, recordingJar(layeredJar));
      List<AssemblyFileSet> defaultFileSets = Collections.singletonList(
          AssemblyFileSet.builder().directory(new File("src/main/resources")).build()
      );

      // When
      AssemblyConfiguration config = generator.createAssemblyConfiguration(defaultFileSets);

      // Then
      assertThat(config.getLayers())
          .hasSize(5) // jkube-includes + 4 Spring Boot layers
          .extracting(Assembly::getId)
          .containsExactly("jkube-includes", "dependencies", "spring-boot-loader", "snapshot-dependencies", "application");
    }

    @Test
    @DisplayName("should create flat output directory structure for all layers")
    void shouldCreateFlatOutputStructure() throws IOException {
      // Given
      File layeredJar = createRealLayeredJar();
      LayeredJarGenerator generator = new LayeredJarGenerator(generatorContext, generatorConfig, recordingJar(layeredJar));

      // When
      AssemblyConfiguration config = generator.createAssemblyConfiguration(Collections.emptyList());

      // Then - All Spring Boot layers should output to root directory (flat structure)
      List<Assembly> springBootLayers = config.getLayers().subList(1, config.getLayers().size());
      assertThat(springBootLayers)
          .flatExtracting(Assembly::getFileSets)
          .extracting(AssemblyFileSet::getOutputDirectory)
          .allMatch(dir -> dir.getPath().equals("."));
    }

    @Test
    @DisplayName("should set correct file permissions for layer files")
    void shouldSetCorrectFilePermissions() throws IOException {
      // Given
      File layeredJar = createRealLayeredJar();
      LayeredJarGenerator generator = new LayeredJarGenerator(generatorContext, generatorConfig, recordingJar(layeredJar));

      // When
      AssemblyConfiguration config = generator.createAssemblyConfiguration(Collections.emptyList());

      // Then
      List<Assembly> springBootLayers = config.getLayers().subList(1, config.getLayers().size());
      assertThat(springBootLayers)
          .flatExtracting(Assembly::getFileSets)
          .extracting(AssemblyFileSet::getFileMode)
          .allMatch(mode -> mode.equals("0640"));
    }

    @Test
    @DisplayName("should exclude final output artifact")
    void shouldExcludeFinalOutputArtifact() throws IOException {
      // Given
      File layeredJar = createRealLayeredJar();
      LayeredJarGenerator generator = new LayeredJarGenerator(generatorContext, generatorConfig, recordingJar(layeredJar));

      // When
      AssemblyConfiguration config = generator.createAssemblyConfiguration(Collections.emptyList());

      // Then
      assertThat(config.isExcludeFinalOutputArtifact()).isTrue();
    }

    @Test
    @DisplayName("should include default fileSets in jkube-includes assembly")
    void shouldIncludeDefaultFileSets() throws IOException {
      // Given
      File layeredJar = createRealLayeredJar();
      LayeredJarGenerator generator = new LayeredJarGenerator(generatorContext, generatorConfig, recordingJar(layeredJar));

      List<AssemblyFileSet> defaultFileSets = new ArrayList<>();
      defaultFileSets.add(AssemblyFileSet.builder().directory(new File("src/main/jkube")).build());
      defaultFileSets.add(AssemblyFileSet.builder().directory(new File("src/main/resources")).build());

      // When
      AssemblyConfiguration config = generator.createAssemblyConfiguration(defaultFileSets);

      // Then
      assertThat(config.getLayers())
          .first()
          .satisfies(assembly -> {
            assertThat(assembly.getId()).isEqualTo("jkube-includes");
            assertThat(assembly.getFileSets()).hasSize(2);
          });
    }

    @Test
    @DisplayName("should extract the layered jar into the build package directory")
    void shouldExtractIntoBuildPackageDirectory() throws IOException {
      // Given
      RecordingLayeredJar layeredJar = recordingJar(createRealLayeredJar());
      LayeredJarGenerator generator = new LayeredJarGenerator(generatorContext, generatorConfig, layeredJar);

      // When
      generator.createAssemblyConfiguration(Collections.emptyList());

      // Then
      assertThat(layeredJar.extractionDirs).containsExactly(targetDir);
    }

    @Test
    @DisplayName("should point each layer fileSet at buildPackageDirectory/<layer>")
    void shouldPointLayerFileSetsAtBuildPackageDirectory() throws IOException {
      // Given
      File layeredJar = createRealLayeredJar();
      LayeredJarGenerator generator = new LayeredJarGenerator(generatorContext, generatorConfig, recordingJar(layeredJar));

      // When
      AssemblyConfiguration config = generator.createAssemblyConfiguration(Collections.emptyList());

      // Then - each layer resolves to target/<layer>, relative to the project base directory
      List<Assembly> springBootLayers = config.getLayers().subList(1, config.getLayers().size());
      assertThat(springBootLayers)
          .isNotEmpty()
          .allSatisfy(layer -> assertThat(layer.getFileSets())
              .singleElement()
              .extracting(AssemblyFileSet::getDirectory)
              .isEqualTo(new File("target", layer.getId())));
    }

    @Test
    @DisplayName("should support custom layer names from layers.xml configuration")
    void shouldSupportCustomLayerNames() throws IOException {
      // Given - Jar with custom layer names (libs, loader, snapshots, app) instead of standard names
      // This simulates a custom layers.xml configuration via spring-boot-maven-plugin
      File customLayeredJar = createJarWithCustomLayers();
      LayeredJarGenerator generator = new LayeredJarGenerator(generatorContext, generatorConfig, recordingJar(customLayeredJar));

      // When
      AssemblyConfiguration config = generator.createAssemblyConfiguration(Collections.emptyList());

      // Then - layer names come from the jar's layers.idx, with no assumption about their values
      assertThat(config.getLayers())
          .hasSize(5) // jkube-includes + 4 custom layers
          .extracting(Assembly::getId)
          .containsExactly("jkube-includes", "libs", "loader", "snapshots", "app");

      // And - each custom layer resolves to target/<layer> just like the standard ones
      List<Assembly> springBootLayers = config.getLayers().subList(1, config.getLayers().size());
      assertThat(springBootLayers)
          .allSatisfy(layer -> assertThat(layer.getFileSets())
              .singleElement()
              .extracting(AssemblyFileSet::getDirectory)
              .isEqualTo(new File("target", layer.getId())));
    }
  }


  // Helper methods

  /**
   * A {@link SpringBootLayeredJar} that reads the real jar (manifest, layers.idx) but records
   * extraction requests instead of performing them. Extraction itself forks a JVM and is covered by
   * {@code SpringBootLayeredJarTest}/{@code SpringBootLayeredJarFallbackTest}; here it is recorded so
   * that the generator's use of it stays observable.
   */
  private static final class RecordingLayeredJar extends SpringBootLayeredJar {
    private final List<File> extractionDirs = new ArrayList<>();

    private RecordingLayeredJar(File layeredJar) {
      super(layeredJar, new KitLogger.SilentLogger());
    }

    @Override
    public void extractLayers(File extractionDir) {
      extractionDirs.add(extractionDir);
    }
  }

  private RecordingLayeredJar recordingJar(File layeredJar) {
    return new RecordingLayeredJar(layeredJar);
  }

  private File createLayeredJar(String mainClass) throws IOException {
    File jarFile = new File(tempDir.toFile(), "layered.jar");
    Manifest manifest = new Manifest();
    manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
    manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, mainClass);
    manifest.getMainAttributes().putValue("Spring-Boot-Version", "3.3.0");

    try (JarOutputStream jarOutputStream = new JarOutputStream(Files.newOutputStream(jarFile.toPath()), manifest)) {
      jarOutputStream.putNextEntry(new JarEntry("BOOT-INF/layers.idx"));
      String layersContent = "- \"dependencies\":\n  - \"BOOT-INF/lib/\"\n" +
                            "- \"spring-boot-loader\":\n  - \"org/\"\n" +
                            "- \"snapshot-dependencies\":\n  - \"BOOT-INF/lib/snapshot/\"\n" +
                            "- \"application\":\n  - \"BOOT-INF/classes/\"\n  - \"META-INF/\"\n";
      jarOutputStream.write(layersContent.getBytes());
    }
    return jarFile;
  }

  private File createLayeredJarWithoutMainClass() throws IOException {
    File jarFile = new File(tempDir.toFile(), "no-main.jar");
    Manifest manifest = new Manifest();
    manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
    // Intentionally omit Main-Class
    manifest.getMainAttributes().putValue("Spring-Boot-Version", "3.3.0");

    try (JarOutputStream jarOutputStream = new JarOutputStream(Files.newOutputStream(jarFile.toPath()), manifest)) {
      jarOutputStream.putNextEntry(new JarEntry("BOOT-INF/layers.idx"));
      jarOutputStream.write("- \"dependencies\":\n  - \"BOOT-INF/lib/\"\n".getBytes());
    }
    return jarFile;
  }

  private File createRealLayeredJar() throws IOException {
    File jarFile = new File(tempDir.toFile(), "layered.jar");
    Files.copy(
        Objects.requireNonNull(getClass().getResourceAsStream("/generator-integration-test/layered-jar.jar")),
        jarFile.toPath()
    );
    return jarFile;
  }

  private File createJarWithCustomLayers() throws IOException {
    File jarFile = new File(tempDir.toFile(), "custom-layers.jar");
    Manifest manifest = new Manifest();
    manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
    manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, "org.springframework.boot.loader.JarLauncher");
    manifest.getMainAttributes().putValue("Spring-Boot-Version", "3.3.0");

    try (JarOutputStream jarOutputStream = new JarOutputStream(Files.newOutputStream(jarFile.toPath()), manifest)) {
      jarOutputStream.putNextEntry(new JarEntry("BOOT-INF/layers.idx"));

      // Build layers.idx content with custom layer names
      StringBuilder layersContent = new StringBuilder();
      for (String layerName : new String[]{"libs", "loader", "snapshots", "app"}) {
        layersContent.append("- \"").append(layerName).append("\":\n");
        layersContent.append("  - \"BOOT-INF/lib/\"\n");
      }

      jarOutputStream.write(layersContent.toString().getBytes());
    }
    return jarFile;
  }
}
