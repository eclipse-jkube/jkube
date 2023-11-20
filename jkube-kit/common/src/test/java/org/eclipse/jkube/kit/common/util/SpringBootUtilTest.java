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
package org.eclipse.jkube.kit.common.util;

import org.apache.commons.io.FileUtils;
import org.eclipse.jkube.kit.common.Dependency;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.Plugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.BeforeEach;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

class SpringBootUtilTest {

    private JavaProject mavenProject;

    @BeforeEach
    public void setUp() {
        mavenProject = JavaProject.builder()
          .properties(new Properties())
          .build();
    }

    @Test
    void getSpringBootApplicationPropertiesLoadsStandardProperties(@TempDir File tempDir) throws IOException {
        //Given
        URLClassLoader cl = createClassLoader(tempDir, "/util/springboot/spring-boot-application.properties");
        //When
        Properties result =  SpringBootUtil.getSpringBootApplicationProperties(cl);
        //Then
        assertThat(result).containsOnly(
                entry("spring.application.name", "demoservice"),
                entry("server.port", "9090")
        );
    }

    @Test
    void getSpringBootVersion() {
        //Given
        mavenProject = mavenProject.toBuilder()
          .dependency(Dependency.builder().groupId("org.springframework.boot").version("1.6.3").build())
          .build();

        //when
        Optional<String> result = SpringBootUtil.getSpringBootVersion(mavenProject);

        //Then
        assertThat(result).isPresent().contains("1.6.3");
    }

    @Test
    void getSpringBootActiveProfileWhenNotNull() {
        //Given
        mavenProject.getProperties().put("spring.profiles.active","spring-boot");

        // When
        String result = SpringBootUtil.getSpringBootActiveProfile(mavenProject);

        //Then
        assertThat(result).isEqualTo("spring-boot");
    }

    @Test
    void testGetSpringBootActiveProfileWhenNull() {
        assertThat(SpringBootUtil.getSpringBootActiveProfile(null)).isNull();
    }

    @Test
    void getSpringBootApplicationProperties_withCompileClassloader_shouldLoadProperties() {
        // Given
        JavaProject javaProject = JavaProject.builder()
            .compileClassPathElement(Objects.requireNonNull(getClass().getResource("/util/springboot/resources")).getPath())
            .outputDirectory(new File("target"))
            .build();
        URLClassLoader compileClassLoader = JKubeProjectUtil.getClassLoader(javaProject);

        // When
        Properties properties = SpringBootUtil.getSpringBootApplicationProperties(compileClassLoader);

        // Then
        assertThat(properties)
            .containsEntry("server.port", "8081");
    }


    @Test
    void testYamlToPropertiesParsing() {

        Properties props = YamlUtil.getPropertiesFromYamlResource(getClass().getResource("/util/springboot/test-application.yml"));
        assertThat(props).isNotEmpty()
            .contains(
                entry("management.port", "8081"),
                entry("spring.datasource.url", "jdbc:mysql://127.0.0.1:3306"),
                entry("example.nested.items[0].value", "value0"),
                entry("example.nested.items[1].value", "value1"),
                entry("example.nested.items[2].elements[0].element[0].subelement", "sub0"),
                entry("example.nested.items[2].elements[0].element[1].subelement", "sub1"),
                entry("example.1", "integerKeyElement"));
    }

    @Test
    void testInvalidFileThrowsException() {
        URL resource = SpringBootUtil.class.getResource("/util/springboot/invalid-application.yml");
        assertThrows(IllegalStateException.class, () -> YamlUtil.getPropertiesFromYamlResource(resource));
    }

    @Test
    void testNonExistentYamlToPropertiesParsing() {
        Properties props = YamlUtil.getPropertiesFromYamlResource(getClass().getResource("/this-file-does-not-exist"));
        assertThat(props).isNotNull().isEmpty();
    }

    @Test
    void getSpringBootApplicationProperties_withMultipleProfilesAndDefault(@TempDir File tempDir) throws IOException {
        URLClassLoader cl = createClassLoader(tempDir, "/util/springboot/test-application-with-multiple-profiles.yml");
        Properties props = SpringBootUtil.getSpringBootApplicationProperties(cl);
        assertThat(props).isNotEmpty()
            .contains(
                entry("spring.application.name", "spring-boot-k8-recipes"),
                entry("management.endpoints.enabled-by-default", "false"),
                entry("management.endpoint.health.enabled", "true"))
            .doesNotContainEntry("cloud.kubernetes.reload.enabled", null);
    }

    @Test
    void getSpringBootApplicationProperties_withMultipleProfilesAndSpecificProfile(@TempDir File tempDir) throws IOException {
        URLClassLoader cl = createClassLoader(tempDir, "/util/springboot/test-application-with-multiple-profiles.yml");
        Properties props = SpringBootUtil.getSpringBootApplicationProperties("kubernetes", cl);
        assertThat(props)
            .containsEntry("cloud.kubernetes.reload.enabled", "true")
            .doesNotContain(
                entry("cloud.kubernetes.reload.enabled", null),
                entry("spring.application.name", null));
    }

    @Test
    void getSpringBootPluginConfiguration_whenNothingPresent_thenReturnsEmptyMap() {
        // Given
        JavaProject javaProject = JavaProject.builder().build();

        // When
        Map<String, Object> configuration = SpringBootUtil.getSpringBootPluginConfiguration(javaProject);

        // Then
        assertThat(configuration).isEmpty();
    }

    @Test
    void getSpringBootPluginConfiguration_whenSpringBootMavenPluginPresent_thenReturnsPluginConfiguration() {
        // Given
        JavaProject javaProject = JavaProject.builder()
            .plugin(Plugin.builder()
                .groupId("org.springframework.boot")
                .artifactId("spring-boot-maven-plugin")
                .configuration(Collections.singletonMap("layout", "ZIP"))
                .build())
            .build();

        // When
        Map<String, Object> configuration = SpringBootUtil.getSpringBootPluginConfiguration(javaProject);

        // Then
        assertThat(configuration).isNotNull().containsEntry("layout", "ZIP");
    }

    @Test
    void getSpringBootPluginConfiguration_whenSpringBootGradlePluginPresent_thenReturnsPluginConfiguration() {
        // Given
        JavaProject javaProject = JavaProject.builder()
            .plugin(Plugin.builder()
                .groupId("org.springframework.boot")
                .artifactId("org.springframework.boot.gradle.plugin")
                .configuration(Collections.singletonMap("mainClass", "com.example.ExampleApplication"))
                .build())
            .build();

        // When
        Map<String, Object> configuration = SpringBootUtil.getSpringBootPluginConfiguration(javaProject);

        // Then
        assertThat(configuration).isNotNull().containsEntry("mainClass", "com.example.ExampleApplication");
    }

    @Test
    void isSpringBootRepackage_whenPluginHasRepackageExecution_thenReturnTrue() {
        // Given
        JavaProject javaProject = JavaProject.builder()
            .plugin(Plugin.builder()
                .groupId("org.springframework.boot")
                .artifactId("spring-boot-maven-plugin")
                .executions(Collections.singletonList("repackage"))
                .build())
            .build();

        // When
        boolean result = SpringBootUtil.isSpringBootRepackage(javaProject);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void isSpringBootRepackage_whenNoExecution_thenReturnFalse() {
        // Given
        JavaProject javaProject = JavaProject.builder()
            .plugin(Plugin.builder()
                .groupId("org.springframework.boot")
                .artifactId("spring-boot-maven-plugin")
                .executions(Collections.emptyList())
                .build())
            .build();

        // When
        boolean result = SpringBootUtil.isSpringBootRepackage(javaProject);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void isLayeredJar_whenInvalidFile_thenThrowException() {
        // When + Then
        assertThatIllegalStateException()
            .isThrownBy(() -> SpringBootUtil.isLayeredJar(new File("i-dont-exist.jar")))
            .withMessage("Failure in inspecting fat jar for layers.idx file");
    }

    @Test
    void isLayeredJar_whenJarContainsLayers_thenReturnTrue(@TempDir File temporaryFolder) throws IOException {
        // Given
        File jarFile = new File(temporaryFolder, "fat.jar");
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, "org.example.Foo");
        try (JarOutputStream jarOutputStream = new JarOutputStream(Files.newOutputStream(jarFile.toPath()), manifest)) {
            jarOutputStream.putNextEntry(new JarEntry("BOOT-INF/layers.idx"));
        }

        // When
        boolean result = SpringBootUtil.isLayeredJar(jarFile);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void getNativePlugin_whenNoNativePluginPresent_thenReturnNull() {
        assertThat(SpringBootUtil.getNativePlugin(JavaProject.builder().build())).isNull();
    }

    @Test
    void getNativePlugin_whenMavenNativePluginPresent_thenReturnPlugin() {
        // Given
        JavaProject javaProject = JavaProject.builder()
            .plugin(Plugin.builder()
                .groupId("org.graalvm.buildtools")
                .artifactId("native-maven-plugin")
                .build())
            .build();

        // When
        Plugin plugin = SpringBootUtil.getNativePlugin(javaProject);

        // Then
        assertThat(plugin).isNotNull();
    }

    @Test
    void getNativePlugin_whenGradleNativePluginPresent_thenReturnPlugin() {
        // Given
        JavaProject javaProject = JavaProject.builder()
            .plugin(Plugin.builder()
                .groupId("org.graalvm.buildtools.native")
                .artifactId("org.graalvm.buildtools.native.gradle.plugin")
                .build())
            .build();

        // When
        Plugin plugin = SpringBootUtil.getNativePlugin(javaProject);

        // Then
        assertThat(plugin).isNotNull();
    }

    @Test
    void findNativeArtifactFile_whenNativeExecutableNotFound_thenReturnNull(@TempDir File temporaryFolder) throws IOException {
        // Given
        JavaProject javaProject = JavaProject.builder()
            .artifactId("sample")
            .buildDirectory(temporaryFolder)
            .plugin(Plugin.builder()
                .groupId("org.graalvm.buildtools")
                .artifactId("native-maven-plugin")
                .build())
            .build();

        // When
        File nativeArtifactFound = SpringBootUtil.findNativeArtifactFile(javaProject);

        // Then
        assertThat(nativeArtifactFound).isNull();
    }

    @Test
    void findNativeArtifactFile_whenNativeExecutableInStandardMavenBuildDirectory_thenReturnNativeArtifact(@TempDir File temporaryFolder) throws IOException {
        // Given
        File nativeArtifactFile = Files.createFile(temporaryFolder.toPath().resolve("sample")).toFile();
        assertThat(nativeArtifactFile.setExecutable(true)).isTrue();
        JavaProject javaProject = JavaProject.builder()
            .artifactId("sample")
            .buildDirectory(temporaryFolder)
            .plugin(Plugin.builder()
                .groupId("org.graalvm.buildtools")
                .artifactId("native-maven-plugin")
                .build())
            .build();

        // When
        File nativeArtifactFound = SpringBootUtil.findNativeArtifactFile(javaProject);

        // Then
        assertThat(nativeArtifactFound).hasName("sample");
    }

    @Test
    void findNativeArtifactFile_whenMoreThanOneNativeExecutableInStandardMavenBuildDirectory_thenThrowException(@TempDir File temporaryFolder) throws IOException {
        // Given
        File nativeArtifactFile = Files.createFile(temporaryFolder.toPath().resolve("sample")).toFile();
        assertThat(nativeArtifactFile.setExecutable(true)).isTrue();
        File nativeArtifactFile2 = Files.createFile(temporaryFolder.toPath().resolve("sample2")).toFile();
        assertThat(nativeArtifactFile2.setExecutable(true)).isTrue();
        JavaProject javaProject = JavaProject.builder()
            .artifactId("sample")
            .buildDirectory(temporaryFolder)
            .plugin(Plugin.builder()
                .groupId("org.graalvm.buildtools")
                .artifactId("native-maven-plugin")
                .build())
            .build();

        // When + Then
        assertThatIllegalStateException()
            .isThrownBy(() -> SpringBootUtil.findNativeArtifactFile(javaProject))
            .withMessage("More than one native executable file found in " + temporaryFolder.getAbsolutePath());
    }

    @Test
    void findNativeArtifactFile_whenNativeExecutableInStandardMavenBuildDirectoryAndImageNameOverridden_thenReturnNativeArtifact(@TempDir File temporaryFolder) throws IOException {
        // Given
        File nativeArtifactFile = Files.createFile(temporaryFolder.toPath().resolve("custom-native-name")).toFile();
        assertThat(nativeArtifactFile.setExecutable(true)).isTrue();
        JavaProject javaProject = JavaProject.builder()
            .artifactId("sample")
            .buildDirectory(temporaryFolder)
            .plugin(Plugin.builder()
                .groupId("org.graalvm.buildtools")
                .artifactId("native-maven-plugin")
                .build())
            .build();

        // When
        File nativeArtifactFound = SpringBootUtil.findNativeArtifactFile(javaProject);

        // Then
        assertThat(nativeArtifactFound).hasName("custom-native-name");
    }

    @Test
    void findNativeArtifactFile_whenNativeExecutableInStandardGradleNativeDirectory_thenReturnNativeArtifact(@TempDir File temporaryFolder) throws IOException {
        // Given
        Files.createDirectories(temporaryFolder.toPath().resolve("native").resolve("nativeCompile"));
        File nativeArtifactFile = Files.createFile(temporaryFolder.toPath().resolve("native").resolve("nativeCompile").resolve("sample")).toFile();
        assertThat(nativeArtifactFile.setExecutable(true)).isTrue();
        JavaProject javaProject = JavaProject.builder()
            .artifactId("sample")
            .buildDirectory(temporaryFolder)
            .plugin(Plugin.builder()
                .groupId("org.graalvm.buildtools.native")
                .artifactId("org.graalvm.buildtools.native.gradle.plugin")
                .build())
            .build();

        // When
        File nativeArtifactFound = SpringBootUtil.findNativeArtifactFile(javaProject);

        // Then
        assertThat(nativeArtifactFound).hasName("sample");
    }

    static URLClassLoader createClassLoader(File temporaryFolder, String resource) throws IOException {
        File applicationProp =  new File(Objects.requireNonNull(SpringBootUtilTest.class.getResource(resource)).getPath());
        File classesInTarget = new File(new File(temporaryFolder, "target"), "classes");
        FileUtil.createDirectory(classesInTarget);
        File applicationPropertiesInsideTarget = new File(classesInTarget, "application." +
          applicationProp.getName().substring(applicationProp.getName().lastIndexOf(".") + 1));
        FileUtils.copyFile(applicationProp, applicationPropertiesInsideTarget);
        return ClassUtil.createClassLoader(Arrays.asList(classesInTarget.getAbsolutePath(), applicationProp.getAbsolutePath()));
    }
}
