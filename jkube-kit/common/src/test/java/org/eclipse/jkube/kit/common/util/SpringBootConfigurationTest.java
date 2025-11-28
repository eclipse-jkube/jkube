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

import org.eclipse.jkube.kit.common.Dependency;
import org.eclipse.jkube.kit.common.JavaProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class SpringBootConfigurationTest {

  private JavaProject project;
  private SpringBootConfiguration springBootConfiguration;

  @BeforeEach
  void setUp(@TempDir Path target) throws IOException {
    final Properties properties = new Properties();
    properties.put("management.port", "1");
    properties.put("management.server.port", "2");
    properties.put("server.port", "1");
    properties.put("server.ssl.key-store", "server.ssl.key-store");
    properties.put("management.ssl.key-store", "management.ssl.key-store");
    properties.put("server.servlet-path", "server.servlet-path");
    properties.put("server.servlet.path", "server.servlet.path");
    properties.put("spring.mvc.servlet.path", "spring.mvc.servlet.path");
    properties.put("server.context-path", "server.context-path");
    properties.put("server.servlet.context-path", "server.servlet.context-path");
    properties.put("management.server.base-path", "management.server.base-path");
    properties.put("management.server.ssl.key-store", "management.server.ssl.key-store");
    properties.put("management.context-path", "management.context-path");
    properties.put("management.server.servlet.context-path", "management.server.servlet.context-path");
    properties.put("management.endpoints.web.base-path", "management.endpoints.web.base-path");
    properties.put("server.ssl.enabled", "true");
    properties.put("management.ssl.enabled", "true");
    properties.put("management.server.ssl.enabled", "true");
    try (OutputStream fos = Files.newOutputStream(target.resolve("application.properties"))) {
      properties.store(fos, null);
    }
    project = JavaProject.builder()
      .properties(properties)
      .outputDirectory(target.toFile())
      .build();
  }

  @Nested
  @DisplayName("With '3.0' Spring Boot Version")
  class SpringBoot3 {

    @BeforeEach
    void setUp() {
      springBootConfiguration = SpringBootConfiguration.from(project.toBuilder()
        .dependency(Dependency.builder()
          .groupId("org.springframework.boot")
          .artifactId("spring-boot")
          .version("3.0")
          .build())
        .build());
    }

    @Test
    @DisplayName("getManagementPort defaults to 'management.server.port'")
    void getManagementPort() {
      assertThat(springBootConfiguration.getManagementPort()).isEqualTo(2);
    }

    @Test
    @DisplayName("getServerPort defaults to 'server.port'")
    void getServerPort() {
      assertThat(springBootConfiguration.getServerPort()).isEqualTo(1);
    }

    @Test
    @DisplayName("getServletPath defaults to 'spring.mvc.servlet.path'")
    void getServletPath() {
      assertThat(springBootConfiguration.getServletPath()).isEqualTo("spring.mvc.servlet.path");
    }

    @Test
    @DisplayName("getManagementContextPath defaults to 'management.server.base-path'")
    void getManagementContextPath() {
      assertThat(springBootConfiguration.getManagementContextPath()).isEqualTo("management.server.base-path");
    }

    @Test
    @DisplayName("getServerKeystore defaults to 'server.ssl.key-store'")
    void getServerKeystore() {
      assertThat(springBootConfiguration.getServerKeystore()).isEqualTo("server.ssl.key-store");
    }

    @Test
    @DisplayName("getManagementKeystore defaults to 'management.server.ssl.key-store'")
    void getManagementKeystore() {
      assertThat(springBootConfiguration.getManagementKeystore())
          .isEqualTo("management.server.ssl.key-store");
    }

    @Test
    @DisplayName("getServerContextPath defaults to 'server.servlet.context-path'")
    void getServerContextPath() {
      assertThat(springBootConfiguration.getServerContextPath())
          .isEqualTo("server.servlet.context-path");
    }

    @Test
    @DisplayName("getActuatorBasePath defaults to 'management.endpoints.web.base-path'")
    void getActuatorBasePath() {
      assertThat(springBootConfiguration.getActuatorBasePath())
          .isEqualTo("management.endpoints.web.base-path");
    }

    @Test
    @DisplayName("getActuatorDefaultBasePath defaults to '/actuator'")
    void getActuatorDefaultBasePath() {
      assertThat(springBootConfiguration.getActuatorDefaultBasePath()).isEqualTo("/actuator");
    }

    @Test
    @DisplayName("isServerSslEnabled returns true when server.ssl.enabled is true")
    void isServerSslEnabled_whenExplicitlyTrue() {
      assertThat(springBootConfiguration.isServerSslEnabled()).isTrue();
    }

    @Test
    @DisplayName("isManagementSslEnabled returns true when management.server.ssl.enabled is true")
    void isManagementSslEnabled_whenExplicitlyTrue() {
      assertThat(springBootConfiguration.isManagementSslEnabled()).isTrue();
    }

    @Test
    @DisplayName("isServerSslEnabled defaults to true when keystore configured and ssl.enabled not set")
    void isServerSslEnabled_defaultsToTrueWithKeystore(@TempDir Path target) throws IOException {
      final Properties properties = new Properties();
      properties.put("server.ssl.key-store", "keystore.jks");
      try (OutputStream fos = Files.newOutputStream(target.resolve("application.properties"))) {
        properties.store(fos, null);
      }
      SpringBootConfiguration config = SpringBootConfiguration.from(JavaProject.builder()
        .properties(properties)
        .outputDirectory(target.toFile())
        .dependency(Dependency.builder()
          .groupId("org.springframework.boot")
          .artifactId("spring-boot")
          .version("3.0")
          .build())
        .build());
      assertThat(config.isServerSslEnabled()).isTrue();
    }

    @Test
    @DisplayName("isServerSslEnabled returns false when server.ssl.enabled is false")
    void isServerSslEnabled_whenExplicitlyFalse(@TempDir Path target) throws IOException {
      final Properties properties = new Properties();
      properties.put("server.ssl.key-store", "keystore.jks");
      properties.put("server.ssl.enabled", "false");
      try (OutputStream fos = Files.newOutputStream(target.resolve("application.properties"))) {
        properties.store(fos, null);
      }
      SpringBootConfiguration config = SpringBootConfiguration.from(JavaProject.builder()
        .properties(properties)
        .outputDirectory(target.toFile())
        .dependency(Dependency.builder()
          .groupId("org.springframework.boot")
          .artifactId("spring-boot")
          .version("3.0")
          .build())
        .build());
      assertThat(config.isServerSslEnabled()).isFalse();
    }

    @Test
    @DisplayName("isManagementSslEnabled returns false when management.server.ssl.enabled is false")
    void isManagementSslEnabled_whenExplicitlyFalse(@TempDir Path target) throws IOException {
      final Properties properties = new Properties();
      properties.put("management.server.ssl.key-store", "keystore.jks");
      properties.put("management.server.ssl.enabled", "false");
      try (OutputStream fos = Files.newOutputStream(target.resolve("application.properties"))) {
        properties.store(fos, null);
      }
      SpringBootConfiguration config = SpringBootConfiguration.from(JavaProject.builder()
        .properties(properties)
        .outputDirectory(target.toFile())
        .dependency(Dependency.builder()
          .groupId("org.springframework.boot")
          .artifactId("spring-boot")
          .version("3.0")
          .build())
        .build());
      assertThat(config.isManagementSslEnabled()).isFalse();
    }

    @Test
    @DisplayName("isServerSslEnabled defaults to false when no keystore and ssl.enabled not set")
    void isServerSslEnabled_defaultsToFalseWithoutKeystore(@TempDir Path target) throws IOException {
      final Properties properties = new Properties();
      try (OutputStream fos = Files.newOutputStream(target.resolve("application.properties"))) {
        properties.store(fos, null);
      }
      SpringBootConfiguration config = SpringBootConfiguration.from(JavaProject.builder()
        .properties(properties)
        .outputDirectory(target.toFile())
        .dependency(Dependency.builder()
          .groupId("org.springframework.boot")
          .artifactId("spring-boot")
          .version("3.0")
          .build())
        .build());
      assertThat(config.isServerSslEnabled()).isFalse();
    }
  }

  @Nested
  @DisplayName("With '2.0' Spring Boot Version")
  class SpringBoot2 {
    @BeforeEach
    void setUp() {
      springBootConfiguration = SpringBootConfiguration.from(project.toBuilder()
        .dependency(Dependency.builder()
          .groupId("org.springframework.boot")
          .artifactId("spring-boot")
          .version("2.0")
          .build())
        .build());
    }

    @Test
    @DisplayName("getManagementPort defaults to 'management.server.port'")
    void getManagementPort() {
      assertThat(springBootConfiguration.getManagementPort()).isEqualTo(2);
    }

    @Test
    @DisplayName("getServerPort defaults to 'server.port'")
    void getServerPort() {
      assertThat(springBootConfiguration.getServerPort()).isEqualTo(1);
    }

    @Test
    @DisplayName("getServerKeystorePropertyKey defaults to 'server.ssl.key-store'")
    void getServerKeystore() {
      assertThat(springBootConfiguration.getServerKeystore()).isEqualTo("server.ssl.key-store");
    }

    @Test
    @DisplayName("getManagementKeystorePropertyKey defaults to 'management.server.ssl.key-store'")
    void getManagementKeystore() {
      assertThat(springBootConfiguration.getManagementKeystore())
        .isEqualTo("management.server.ssl.key-store");
    }

    @Test
    @DisplayName("getServletPath defaults to 'server.servlet.path'")
    void getServletPath() {
      assertThat(springBootConfiguration.getServletPath()).isEqualTo("server.servlet.path");
    }

    @Test
    @DisplayName("getServerContextPath defaults to 'server.servlet.context-path'")
    void getServerContextPath() {
      assertThat(springBootConfiguration.getServerContextPath())
        .isEqualTo("server.servlet.context-path");
    }

    @Test
    @DisplayName("getManagementContextPath defaults to 'management.server.servlet.context-path'")
    void getManagementContextPath() {
      assertThat(springBootConfiguration.getManagementContextPath())
        .isEqualTo("management.server.servlet.context-path");
    }

    @Test
    @DisplayName("getActuatorBasePath defaults to 'management.endpoints.web.base-path'")
    void getActuatorBasePath() {
      assertThat(springBootConfiguration.getActuatorBasePath())
        .isEqualTo("management.endpoints.web.base-path");
    }

    @Test
    @DisplayName("getActuatorDefaultBasePath defaults to '/actuator'")
    void getActuatorDefaultBasePath() {
      assertThat(springBootConfiguration.getActuatorDefaultBasePath()).isEqualTo("/actuator");
    }

    @Test
    @DisplayName("isServerSslEnabled returns true when server.ssl.enabled is true")
    void isServerSslEnabled_whenExplicitlyTrue() {
      assertThat(springBootConfiguration.isServerSslEnabled()).isTrue();
    }

    @Test
    @DisplayName("isManagementSslEnabled returns true when management.server.ssl.enabled is true")
    void isManagementSslEnabled_whenExplicitlyTrue() {
      assertThat(springBootConfiguration.isManagementSslEnabled()).isTrue();
    }

    @Test
    @DisplayName("isServerSslEnabled returns false when server.ssl.enabled is false")
    void isServerSslEnabled_whenExplicitlyFalse(@TempDir Path target) throws IOException {
      final Properties properties = new Properties();
      properties.put("server.ssl.key-store", "keystore.jks");
      properties.put("server.ssl.enabled", "false");
      try (OutputStream fos = Files.newOutputStream(target.resolve("application.properties"))) {
        properties.store(fos, null);
      }
      SpringBootConfiguration config = SpringBootConfiguration.from(JavaProject.builder()
        .properties(properties)
        .outputDirectory(target.toFile())
        .dependency(Dependency.builder()
          .groupId("org.springframework.boot")
          .artifactId("spring-boot")
          .version("2.0")
          .build())
        .build());
      assertThat(config.isServerSslEnabled()).isFalse();
    }

    @Test
    @DisplayName("isManagementSslEnabled returns false when management.server.ssl.enabled is false")
    void isManagementSslEnabled_whenExplicitlyFalse(@TempDir Path target) throws IOException {
      final Properties properties = new Properties();
      properties.put("management.server.ssl.key-store", "keystore.jks");
      properties.put("management.server.ssl.enabled", "false");
      try (OutputStream fos = Files.newOutputStream(target.resolve("application.properties"))) {
        properties.store(fos, null);
      }
      SpringBootConfiguration config = SpringBootConfiguration.from(JavaProject.builder()
        .properties(properties)
        .outputDirectory(target.toFile())
        .dependency(Dependency.builder()
          .groupId("org.springframework.boot")
          .artifactId("spring-boot")
          .version("2.0")
          .build())
        .build());
      assertThat(config.isManagementSslEnabled()).isFalse();
    }
  }

  @Nested
  @DisplayName("With '1.0' (or undefined) Spring Boot Version")
  class SpringBoot1 {

    @BeforeEach
    void setUp() {
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = { "1.0", "undefined" })
    @DisplayName("getManagementPort defaults to 'management.port' (Spring Boot 1)")
    void getManagementPort(String version) {
      springBootConfiguration = SpringBootConfiguration.from(project.toBuilder()
        .dependency(Dependency.builder()
          .groupId("org.springframework.boot")
          .artifactId("spring-boot")
          .version(version)
          .build())
        .build());
      assertThat(springBootConfiguration.getManagementPort())
        .isEqualTo(1);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = { "1.0", "undefined" })
    @DisplayName("getServerPort defaults to 'server.port' (Spring Boot 1)")
    void getServerPort(String version) {
      springBootConfiguration = SpringBootConfiguration.from(project.toBuilder()
        .dependency(Dependency.builder()
          .groupId("org.springframework.boot")
          .artifactId("spring-boot")
          .version(version)
          .build())
        .build());
      assertThat(springBootConfiguration.getServerPort())
        .isEqualTo(1);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = { "1.0", "undefined" })
    @DisplayName("getServerKeystore defaults to 'server.ssl.key-store' (Spring Boot 1)")
    void getServerKeystore(String version) {
      springBootConfiguration = SpringBootConfiguration.from(project.toBuilder()
        .dependency(Dependency.builder()
          .groupId("org.springframework.boot")
          .artifactId("spring-boot")
          .version(version)
          .build())
        .build());
      assertThat(springBootConfiguration.getServerKeystore())
        .isEqualTo("server.ssl.key-store");
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = { "1.0", "undefined" })
    @DisplayName("getManagementKeystore defaults to 'management.ssl.key-store' (Spring Boot 1)")
    void getManagementKeystore(String version) {
      springBootConfiguration = SpringBootConfiguration.from(project.toBuilder()
        .dependency(Dependency.builder()
          .groupId("org.springframework.boot")
          .artifactId("spring-boot")
          .version(version)
          .build())
        .build());
      assertThat(springBootConfiguration.getManagementKeystore())
        .isEqualTo("management.ssl.key-store");
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = { "1.0", "undefined" })
    @DisplayName("getServletPath defaults to 'server.servlet-path' (Spring Boot 1)")
    void getServletPath(String version) {
      springBootConfiguration = SpringBootConfiguration.from(project.toBuilder()
        .dependency(Dependency.builder()
          .groupId("org.springframework.boot")
          .artifactId("spring-boot")
          .version(version)
          .build())
        .build());
      assertThat(springBootConfiguration.getServletPath())
        .isEqualTo("server.servlet-path");
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = { "1.0", "undefined" })
    @DisplayName("getServerContextPath defaults to 'server.context-path' (Spring Boot 1)")
    void getServerContextPath(String version) {
      springBootConfiguration = SpringBootConfiguration.from(project.toBuilder()
        .dependency(Dependency.builder()
          .groupId("org.springframework.boot")
          .artifactId("spring-boot")
          .version(version)
          .build())
        .build());
      assertThat(springBootConfiguration.getServerContextPath())
        .isEqualTo("server.context-path");
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = { "1.0", "undefined" })
    @DisplayName("getManagementContextPath defaults to 'management.context-path' (Spring Boot 1)")
    void getManagementContextPath(String version) {
      springBootConfiguration = SpringBootConfiguration.from(project.toBuilder()
        .dependency(Dependency.builder()
          .groupId("org.springframework.boot")
          .artifactId("spring-boot")
          .version(version)
          .build())
        .build());
      assertThat(springBootConfiguration.getManagementContextPath())
        .isEqualTo("management.context-path");
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = { "1.0", "undefined" })
    @DisplayName("getActuatorBasePath defaults to '' (Spring Boot 1)")
    void getActuatorBasePath(String version) {
      springBootConfiguration = SpringBootConfiguration.from(project.toBuilder()
        .dependency(Dependency.builder()
          .groupId("org.springframework.boot")
          .artifactId("spring-boot")
          .version(version)
          .build())
        .build());
      assertThat(springBootConfiguration.getActuatorBasePath())
        .isEmpty();
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = { "1.0", "undefined" })
    @DisplayName("getActuatorDefaultBasePath defaults to '' (Spring Boot 1)")
    void getActuatorDefaultBasePath(String version) {
      springBootConfiguration = SpringBootConfiguration.from(project.toBuilder()
        .dependency(Dependency.builder()
          .groupId("org.springframework.boot")
          .artifactId("spring-boot")
          .version(version)
          .build())
        .build());
      assertThat(springBootConfiguration.getActuatorDefaultBasePath()).
        isEmpty();
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"1.0", "undefined"})
    @DisplayName("isServerSslEnabled returns true when ssl.enabled is true (Spring Boot 1)")
    void isServerSslEnabled_whenExplicitlyTrue(String version) {
      springBootConfiguration = SpringBootConfiguration.from(project.toBuilder()
        .dependency(Dependency.builder()
          .groupId("org.springframework.boot")
          .artifactId("spring-boot")
          .version(version)
          .build())
        .build());
      assertThat(springBootConfiguration.isServerSslEnabled()).isTrue();
    }

    @Test
    @DisplayName("isManagementSslEnabled uses management.ssl.enabled property (Spring Boot 1)")
    void isManagementSslEnabled_withManagementSslEnabled(@TempDir Path target) throws IOException {
      final Properties properties = new Properties();
      properties.put("management.ssl.key-store", "keystore.jks");
      properties.put("management.ssl.enabled", "false");
      try (OutputStream fos = Files.newOutputStream(target.resolve("application.properties"))) {
        properties.store(fos, null);
      }
      SpringBootConfiguration config = SpringBootConfiguration.from(JavaProject.builder()
        .properties(properties)
        .outputDirectory(target.toFile())
        .dependency(Dependency.builder()
          .groupId("org.springframework.boot")
          .artifactId("spring-boot")
          .version("1.0")
          .build())
        .build());
      assertThat(config.isManagementSslEnabled()).isFalse();
    }

    @Test
    @DisplayName("isServerSslEnabled returns false when server.ssl.enabled is false (Spring Boot 1)")
    void isServerSslEnabled_whenExplicitlyFalse(@TempDir Path target) throws IOException {
      final Properties properties = new Properties();
      properties.put("server.ssl.key-store", "keystore.jks");
      properties.put("server.ssl.enabled", "false");
      try (OutputStream fos = Files.newOutputStream(target.resolve("application.properties"))) {
        properties.store(fos, null);
      }
      SpringBootConfiguration config = SpringBootConfiguration.from(JavaProject.builder()
        .properties(properties)
        .outputDirectory(target.toFile())
        .dependency(Dependency.builder()
          .groupId("org.springframework.boot")
          .artifactId("spring-boot")
          .version("1.0")
          .build())
        .build());
      assertThat(config.isServerSslEnabled()).isFalse();
    }
  }
}
