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
package org.eclipse.jkube.kit.common.util;

import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.Plugin;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Checking the behaviour of utility methods.
 */
class SpringBootUtilTest {

    @Test
    void testYamlToPropertiesParsing() {

        Properties props = YamlUtil.getPropertiesFromYamlResource(SpringBootUtilTest.class.getResource("/util/test-application.yml"));
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
        URL resource = SpringBootUtil.class.getResource("/util/invalid-application.yml");
        assertThrows(IllegalStateException.class, () -> YamlUtil.getPropertiesFromYamlResource(resource));
    }

    @Test
    void testNonExistentYamlToPropertiesParsing() {
        Properties props = YamlUtil.getPropertiesFromYamlResource(SpringBootUtilTest.class.getResource("/this-file-does-not-exist"));
        assertThat(props).isNotNull().isEmpty();
    }

    @Test
    void testMultipleProfilesParsing() {
        Properties props = SpringBootUtil.getPropertiesFromApplicationYamlResource(null, getClass().getResource("/util/test-application-with-multiple-profiles.yml"));

        assertThat(props).isNotEmpty()
                .contains(
                        entry("spring.application.name", "spring-boot-k8-recipes"),
                        entry("management.endpoints.enabled-by-default", "false"),
                        entry("management.endpoint.health.enabled", "true"))
                .doesNotContainEntry("cloud.kubernetes.reload.enabled", null);

        props = SpringBootUtil.getPropertiesFromApplicationYamlResource("kubernetes", getClass().getResource("/util/test-application-with-multiple-profiles.yml"));

        assertThat(props)
                .containsEntry("cloud.kubernetes.reload.enabled", "true")
                .doesNotContain(
                        entry("cloud.kubernetes.reload.enabled", null),
                        entry("spring.application.name", null));
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

}
