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
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

class SpringBootUtilTest {

    private JavaProject mavenProject;

    @BeforeEach
    public void setUp() {
        mavenProject = mock(JavaProject.class);
    }

    @Test
    void testGetSpringBootApplicationProperties(@TempDir File temporaryFolder) throws IOException {
        //Given
        File applicationProp =  new File(Objects.requireNonNull(getClass().getResource("/util/spring-boot-application.properties")).getPath());
        String springActiveProfile = null;
        File targetFolder = new File(temporaryFolder, "target");
        File classesInTarget = new File(targetFolder, "classes");
        boolean isTargetClassesCreated = classesInTarget.mkdirs();
        File applicationPropertiesInsideTarget = new File(classesInTarget, "application.properties");
        FileUtils.copyFile(applicationProp, applicationPropertiesInsideTarget);
        URLClassLoader urlClassLoader = ClassUtil.createClassLoader(Arrays.asList(classesInTarget.getAbsolutePath(), applicationProp.getAbsolutePath()), classesInTarget.getAbsolutePath());

        //When
        Properties result =  SpringBootUtil.getSpringBootApplicationProperties(springActiveProfile ,urlClassLoader);

        //Then
        assertThat(isTargetClassesCreated).isTrue();
        assertThat(result).containsOnly(
                entry("spring.application.name", "demoservice"),
                entry("server.port", "9090")
        );
    }

    @Test
    void testGetSpringBootDevToolsVersion() {
        //Given
        Dependency p = Dependency.builder().groupId("org.springframework.boot").version("1.6.3").build();
        when(mavenProject.getDependencies()).thenReturn(Collections.singletonList(p));

        //when
        Optional<String> result = SpringBootUtil.getSpringBootDevToolsVersion(mavenProject);

        //Then
        assertThat(result).isPresent().contains("1.6.3");
    }


    @Test
    void testGetSpringBootVersion() {
        //Given
        Dependency p = Dependency.builder().groupId("org.springframework.boot").version("1.6.3").build();
        when(mavenProject.getDependencies()).thenReturn(Collections.singletonList(p));

        //when
        Optional<String> result = SpringBootUtil.getSpringBootVersion(mavenProject);

        //Then
        assertThat(result).isPresent().contains("1.6.3");
    }

    @Test
    void testGetSpringBootActiveProfileWhenNotNull() {
        //Given
        Properties p = new Properties();
        p.put("spring.profiles.active","spring-boot");
        when(mavenProject.getProperties()).thenReturn(p);

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
    void testMultipleProfilesParsing() {
        Properties props = SpringBootUtil.getPropertiesFromApplicationYamlResource(null, getClass().getResource("/util/springboot/test-application-with-multiple-profiles.yml"));

        assertThat(props).isNotEmpty()
            .contains(
                entry("spring.application.name", "spring-boot-k8-recipes"),
                entry("management.endpoints.enabled-by-default", "false"),
                entry("management.endpoint.health.enabled", "true"))
            .doesNotContainEntry("cloud.kubernetes.reload.enabled", null);

        props = SpringBootUtil.getPropertiesFromApplicationYamlResource("kubernetes", getClass().getResource("/util/springboot/test-application-with-multiple-profiles.yml"));

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
}
