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

import lombok.Builder;
import lombok.Getter;
import org.eclipse.jkube.kit.common.JavaProject;

import java.util.Optional;
import java.util.Properties;

@Builder
@Getter
public class SpringBootConfiguration {

  private static final String DEFAULT_SERVER_PORT = "8080";

  private Integer managementPort;
  private Integer serverPort;
  private String serverKeystore;
  private String managementKeystore;
  private String servletPath;
  private boolean serverSslEnabled;
  private boolean managementSslEnabled;
  private String serverContextPath;
  private String managementContextPath;
  private String actuatorBasePath;
  private String actuatorDefaultBasePath;
  private String webFluxBasePath;
  private boolean managementHealthProbesEnabled;

  public static SpringBootConfiguration from(JavaProject project) {
    final Properties properties = SpringBootUtil.getSpringBootApplicationProperties(
      SpringBootUtil.getSpringBootActiveProfile(project),
      JKubeProjectUtil.getClassLoader(project));
    final int majorVersion = SpringBootUtil.getSpringBootVersion(project)
      .map(semVer -> {
        try {
          return Integer.parseInt(semVer.substring(0, semVer.indexOf('.')));
        } catch (Exception e) {
          return null;
        }
      })
      // Defaults to Spring 1
      .orElse(1);
    final SpringBootConfiguration.SpringBootConfigurationBuilder configBuilder = SpringBootConfiguration.builder();
    // Spring Boot 1 and common properties
    final String serverKeystore = properties.getProperty("server.ssl.key-store");
    final String managementKeystore = properties.getProperty("management.ssl.key-store");

    configBuilder
      .managementPort(Optional.ofNullable(properties.getProperty("management.port")).map(Integer::parseInt).orElse(null))
      .serverPort(Integer.parseInt(properties.getProperty("server.port", DEFAULT_SERVER_PORT)))
      .serverKeystore(serverKeystore)
      .managementHealthProbesEnabled(Boolean.parseBoolean(properties.getProperty("management.health.probes.enabled")))
      .managementKeystore(managementKeystore)
      .servletPath(properties.getProperty("server.servlet-path"))
      .serverContextPath(properties.getProperty("server.context-path"))
      .managementContextPath(properties.getProperty("management.context-path"))
      .actuatorBasePath("")
      .actuatorDefaultBasePath("")
      .webFluxBasePath(properties.getProperty("spring.webflux.base-path"))
      .serverSslEnabled(Optional.ofNullable(properties.getProperty("server.ssl.enabled"))
        .map(Boolean::parseBoolean)
        .orElse(serverKeystore != null))
      .managementSslEnabled(Optional.ofNullable(properties.getProperty("management.ssl.enabled"))
        .map(Boolean::parseBoolean)
        .orElse(managementKeystore != null));
    if (majorVersion > 1) {
      final String managementServerKeystore = properties.getProperty("management.server.ssl.key-store");
      configBuilder
        .managementPort(Optional.ofNullable(properties.getProperty("management.server.port")).map(Integer::parseInt).orElse(null))
        .managementKeystore(managementServerKeystore)
        .servletPath(properties.getProperty("server.servlet.path"))
        .serverContextPath(properties.getProperty("server.servlet.context-path"))
        .managementContextPath(properties.getProperty("management.server.servlet.context-path"))
        .actuatorBasePath(properties.getProperty("management.endpoints.web.base-path"))
        .actuatorDefaultBasePath("/actuator")
        .managementSslEnabled(Optional.ofNullable(properties.getProperty("management.server.ssl.enabled"))
          .map(Boolean::parseBoolean)
          .orElse(managementServerKeystore != null));
    }
    if (majorVersion == 3) {
      configBuilder
        .servletPath(properties.getProperty("spring.mvc.servlet.path"))
        .managementContextPath(properties.getProperty("management.server.base-path"));
    }
    return configBuilder.build();
  }
}
