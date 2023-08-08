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
package org.eclipse.jkube.springboot.enricher;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Optional;
import java.util.Properties;
import java.util.TreeMap;

import io.fabric8.kubernetes.api.model.Probe;
import org.eclipse.jkube.kit.common.Dependency;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.SpringBootConfigurationHelper;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.eclipse.jkube.kit.common.util.ProjectClassLoaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jkube.springboot.enricher.SpringBootHealthCheckEnricher.REQUIRED_CLASSES;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Check various configurations for spring-boot health checks
 *
 * @author nicola
 */
public abstract class AbstractSpringBootHealthCheckEnricherTestSupport {

    protected JKubeEnricherContext context;

    protected SpringBootConfigurationHelper propertyHelper;

    private ProjectClassLoaders projectClassLoaders;

    @BeforeEach
    void init(@TempDir Path project) throws IOException {
        projectClassLoaders = mock(ProjectClassLoaders.class, RETURNS_DEEP_STUBS);
        context = spy(JKubeEnricherContext.builder()
          .log(new KitLogger.SilentLogger())
          .project(JavaProject.builder()
            .baseDirectory(project.toFile())
            .outputDirectory(Files.createDirectory(project.resolve("target")).toFile())
            .groupId("com.example")
            .artifactId("foo")
            .dependenciesWithTransitive(Collections.singletonList(Dependency.builder()
                .groupId("org.springframework.boot")
                .artifactId("spring-boot")
                .version(getSpringBootVersion())
              .build()))
            .build())
          .processorConfig(new ProcessorConfig())
          .build());
        when(context.getProjectClassLoaders()).thenReturn(projectClassLoaders);
        propertyHelper = new SpringBootConfigurationHelper(getSpringBootVersion());
    }
    protected abstract String getSpringBootVersion();

    @Test
    void zeroConfig() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertHTTPGetPathAndPort(probe, propertyHelper.getActuatorDefaultBasePath() + "/health", 8080);
    }

    @Test
    void withServerPort() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8282");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertHTTPGetPathAndPort(probe, propertyHelper.getActuatorDefaultBasePath() + "/health", 8282);
    }

    @Test
    void withServerPortAndManagementPort() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8282");
        props.put(propertyHelper.getManagementPortPropertyKey(), "8383");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertHTTPGetPathAndPort(probe, propertyHelper.getActuatorDefaultBasePath() + "/health", 8383);
    }

    @Test
    void withServerPortAndManagementPortAndServerContextPath() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8282");
        props.put(propertyHelper.getManagementPortPropertyKey(), "8383");
        props.put(propertyHelper.getServerContextPathPropertyKey(), "/p1");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertHTTPGetPathAndPort(probe, propertyHelper.getActuatorDefaultBasePath() + "/health", 8383);
    }

    @Test
    void withServerPortAndManagementPortAndServerContextPathAndActuatorDefaultBasePath() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8282");
        props.put(propertyHelper.getManagementPortPropertyKey(), "8383");
        props.put(propertyHelper.getServerContextPathPropertyKey(), "/p1");
        props.put(propertyHelper.getActuatorBasePathPropertyKey(), "/p2");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertHTTPGetPathAndPort(probe, "/p2" + "/health", 8383);
    }

    @Test
    void withServerPortAndManagementPortAndServerContextPathAndActuatorDefaultBasePathSlash() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8282");
        props.put(propertyHelper.getManagementPortPropertyKey(), "8383");
        props.put(propertyHelper.getServerContextPathPropertyKey(), "/p1");
        props.put(propertyHelper.getActuatorBasePathPropertyKey(), "/");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertHTTPGetPathAndPort(probe, "/health", 8383);
    }

    @Test
    void withServerPortAndManagementPortAndManagementContextPath() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8282");
        props.put(propertyHelper.getManagementPortPropertyKey(), "8383");
        props.put(propertyHelper.getServerContextPathPropertyKey(), "/p1");
        props.put(propertyHelper.getManagementContextPathPropertyKey(), "/p2");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertHTTPGetPathAndPort(probe, "/p2" + propertyHelper.getActuatorDefaultBasePath() + "/health", 8383);
    }

    @Test
    void withServerPortAndManagementPortAndManagementContextPathAndActuatorDefaultBasePath() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8282");
        props.put(propertyHelper.getManagementPortPropertyKey(), "8383");
        props.put(propertyHelper.getServerContextPathPropertyKey(), "/p1");
        props.put(propertyHelper.getManagementContextPathPropertyKey(), "/p2");
        props.put(propertyHelper.getActuatorBasePathPropertyKey(), "/p3");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertHTTPGetPathAndPort(probe, "/p2/p3" + "/health", 8383);
    }

    @Test
    void withServerPortAndManagementPortAndManagementContextPathAndActuatorDefaultBasePathSlash() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8282");
        props.put(propertyHelper.getManagementPortPropertyKey(), "8383");
        props.put(propertyHelper.getServerContextPathPropertyKey(), "/p1");
        props.put(propertyHelper.getManagementContextPathPropertyKey(), "/");
        props.put(propertyHelper.getActuatorBasePathPropertyKey(), "/");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertHTTPGetPathAndPort(probe, "/health", 8383);
    }

    @Test
    void withServerPortAndManagementPortAndManagementContextPathAndServletPath() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8282");
        props.put(propertyHelper.getManagementPortPropertyKey(), "8383");
        props.put(propertyHelper.getServerContextPathPropertyKey(), "/p1");
        props.put(propertyHelper.getManagementContextPathPropertyKey(), "/p2");
        props.put(propertyHelper.getServletPathPropertyKey(), "/servlet");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertHTTPGetPathAndPort(probe, "/p2" + propertyHelper.getActuatorDefaultBasePath() + "/health", 8383);
    }

    @Test
    void withServerPortAndManagementPortAndManagementContextPathAndServletPathAndActuatorDefaultBasePath() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8282");
        props.put(propertyHelper.getManagementPortPropertyKey(), "8383");
        props.put(propertyHelper.getServerContextPathPropertyKey(), "/p1");
        props.put(propertyHelper.getManagementContextPathPropertyKey(), "/p2");
        props.put(propertyHelper.getServletPathPropertyKey(), "/servlet");
        props.put(propertyHelper.getActuatorBasePathPropertyKey(), "/p3");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertHTTPGetPathAndPort(probe, "/p2/p3" + "/health", 8383);
    }

    @Test
    void withServerPortAndManagementPortAndManagementContextPathAndServletPathAndActuatorDefaultBasePathSlash() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8282");
        props.put(propertyHelper.getManagementPortPropertyKey(), "8383");
        props.put(propertyHelper.getServerContextPathPropertyKey(), "/p1");
        props.put(propertyHelper.getManagementContextPathPropertyKey(), "/");
        props.put(propertyHelper.getServletPathPropertyKey(), "/servlet");
        props.put(propertyHelper.getActuatorBasePathPropertyKey(), "/");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertHTTPGetPathAndPort(probe, "/health", 8383);
    }

    @Test
    void withServerPortAndManagementContextPath() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8282");
        props.put(propertyHelper.getManagementContextPathPropertyKey(), "/p1");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertHTTPGetPathAndPort(probe, "/p1" + propertyHelper.getActuatorDefaultBasePath() +"/health", 8282);
    }

    @Test
    void withServerPortAndManagementContextPathAndActuatorDefaultBasePath() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8282");
        props.put(propertyHelper.getManagementContextPathPropertyKey(), "/p1");
        props.put(propertyHelper.getActuatorBasePathPropertyKey(), "/p2");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertHTTPGetPathAndPort(probe, "/p1/p2" +"/health", 8282);
    }

    @Test
    void withServerPortAndManagementContextPathAndActuatorDefaultBasePathSlash() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8282");
        props.put(propertyHelper.getManagementContextPathPropertyKey(), "/");
        props.put(propertyHelper.getActuatorBasePathPropertyKey(), "/");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertHTTPGetPathAndPort(probe, "/health", 8282);
    }

    @Test
    void withServerPortAndServerContextPathAndManagementContextPath() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8282");
        props.put(propertyHelper.getManagementContextPathPropertyKey(), "/p1");
        props.put(propertyHelper.getServerContextPathPropertyKey(), "/p2");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertHTTPGetPathAndPort(probe, "/p2/p1" + propertyHelper.getActuatorDefaultBasePath() + "/health", 8282);
    }

    @Test
    void withServerPortAndServerContextPathAndManagementContextPathAndActuatorDefaultBasePath() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8282");
        props.put(propertyHelper.getManagementContextPathPropertyKey(), "/p1");
        props.put(propertyHelper.getServerContextPathPropertyKey(), "/p2");
        props.put(propertyHelper.getActuatorBasePathPropertyKey(), "/p3");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertHTTPGetPathAndPort(probe, "/p2/p1/p3" + "/health", 8282);
    }

    @Test
    void withServerPortAndServerContextPathAndManagementContextPathAndActuatorDefaultBasePathSlash() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8282");
        props.put(propertyHelper.getManagementContextPathPropertyKey(), "/");
        props.put(propertyHelper.getServerContextPathPropertyKey(), "/");
        props.put(propertyHelper.getActuatorBasePathPropertyKey(), "/");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertHTTPGetPathAndPort(probe, "/health", 8282);
    }

    @Test
    void withServerPortAndServletPath() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8282");
        props.put(propertyHelper.getServletPathPropertyKey(), "/servlet");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertHTTPGetPathAndPort(probe, "/servlet" + propertyHelper.getActuatorDefaultBasePath() + "/health", 8282);
    }

    @Test
    void withServerPortAndServletAndActuatorDefaultBasePathPath() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8282");
        props.put(propertyHelper.getServletPathPropertyKey(), "/servlet");
        props.put(propertyHelper.getActuatorBasePathPropertyKey(), "/p1");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertHTTPGetPathAndPort(probe, "/servlet/p1" + "/health", 8282);
    }

    @Test
    void withServerPortAndServletPathAndActuatorDefaultBasePathSlash() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8282");
        props.put(propertyHelper.getServletPathPropertyKey(), "/");
        props.put(propertyHelper.getActuatorBasePathPropertyKey(), "/");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertHTTPGetPathAndPort(probe, "/health", 8282);
    }

    @Test
    void withServerPortAndManagementContextPathAndServletPath() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8282");
        props.put(propertyHelper.getServletPathPropertyKey(), "/servlet");
        props.put(propertyHelper.getManagementContextPathPropertyKey(), "/p1");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertHTTPGetPathAndPort(probe, "/servlet/p1" + propertyHelper.getActuatorDefaultBasePath() + "/health", 8282);
    }

    @Test
    void withServerPortAndManagementContextPathAndServletPathAndActuatorDefaultBasePath() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8282");
        props.put(propertyHelper.getServletPathPropertyKey(), "/servlet");
        props.put(propertyHelper.getManagementContextPathPropertyKey(), "/p1");
        props.put(propertyHelper.getActuatorBasePathPropertyKey(), "/p2");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertHTTPGetPathAndPort(probe, "/servlet/p1/p2" + "/health", 8282);
    }

    @Test
    void withServerPortAndManagementContextPathAndServletPathAndActuatorDefaultBasePathSlash() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8282");
        props.put(propertyHelper.getServletPathPropertyKey(), "/");
        props.put(propertyHelper.getManagementContextPathPropertyKey(), "/");
        props.put(propertyHelper.getActuatorBasePathPropertyKey(), "/");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertHTTPGetPathAndPort(probe, "/health", 8282);
    }

    @Test
    void withServerPortAndServerContextPathAndManagementContextPathAndServletPath() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8282");
        props.put(propertyHelper.getServletPathPropertyKey(), "/servlet");
        props.put(propertyHelper.getManagementContextPathPropertyKey(), "/p1");
        props.put(propertyHelper.getServerContextPathPropertyKey(), "/p2");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertHTTPGetPathAndPort(probe, "/p2/servlet/p1" + propertyHelper.getActuatorDefaultBasePath() + "/health", 8282);
    }

    @Test
    void withServerPortAndServerContextPathAndManagementContextPathAndServletPathAndActuatorDefaultBasePath() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8282");
        props.put(propertyHelper.getServletPathPropertyKey(), "/servlet");
        props.put(propertyHelper.getManagementContextPathPropertyKey(), "/p1");
        props.put(propertyHelper.getServerContextPathPropertyKey(), "/p2");
        props.put(propertyHelper.getActuatorBasePathPropertyKey(), "/p3");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertHTTPGetPathAndPort(probe, "/p2/servlet/p1/p3" + "/health", 8282);
    }

    @Test
    void withServerPortAndServerContextPathAndManagementContextPathAndServletPathAndActuatorDefaultBasePathSlash() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8282");
        props.put(propertyHelper.getServletPathPropertyKey(), "/");
        props.put(propertyHelper.getManagementContextPathPropertyKey(), "/");
        props.put(propertyHelper.getServerContextPathPropertyKey(), "/");
        props.put(propertyHelper.getActuatorBasePathPropertyKey(), "/");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertHTTPGetPathAndPort(probe, "/health", 8282);
    }

    @Test
    void schemeWithServerPort() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8443");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertHTTPGetSchemeAndPort(probe, "HTTP", 8443);
    }

    @Test
    void schemeWithServerPortAndManagementKeystore() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8080");
        props.put(propertyHelper.getManagementKeystorePropertyKey(), "classpath:keystore.p12");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertHTTPGetSchemeAndPort(probe, "HTTP", 8080);
    }

    @Test
    void schemeWithServerPortAndServerKeystore() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8443");
        props.put(propertyHelper.getServerKeystorePropertyKey(), "classpath:keystore.p12");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertHTTPGetSchemeAndPort(probe, "HTTPS", 8443);
    }

    @Test
    void schemeWithServerPortAndManagementPortAndServerKeystore() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8443");
        props.put(propertyHelper.getManagementPortPropertyKey(), "8081");
        props.put(propertyHelper.getServerKeystorePropertyKey(), "classpath:keystore.p12");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertHTTPGetSchemeAndPort(probe, "HTTP", 8081);
    }

    @Test
    void schemeWithServerPortAndManagementPortAndManagementKeystore() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8080");
        props.put(propertyHelper.getManagementPortPropertyKey(), "8443");
        props.put(propertyHelper.getManagementKeystorePropertyKey(), "classpath:keystore.p12");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertHTTPGetSchemeAndPort(probe, "HTTPS", 8443);
    }

    @Test
    void testDefaultInitialDelayForLivenessAndReadiness() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        when(context.getProjectClassLoaders().isClassInCompileClasspath(true, REQUIRED_CLASSES))
          .thenReturn(true);
        when(context.getProjectClassLoaders().getCompileClassLoader())
          .thenReturn(new URLClassLoader(new URL[0], AbstractSpringBootHealthCheckEnricherTestSupport.class.getClassLoader()));

        Probe readinessProbe = enricher.getReadinessProbe();
        assertHealthCheckDelays(readinessProbe, 10, null, null);

        Probe livenessProbe = enricher.getLivenessProbe();
        assertHealthCheckDelays(livenessProbe, 180, null, null);
    }

    @Test
    void testCustomInitialDelayForLivenessAndReadinessAndTimeout() {
        TreeMap<String, Object> enricherConfig = new TreeMap<>();
        enricherConfig.put("readinessProbeInitialDelaySeconds", "20");
        enricherConfig.put("livenessProbeInitialDelaySeconds", "360");
        enricherConfig.put("timeoutSeconds", "120");
        context.getConfiguration().getProcessorConfig()
          .setConfig(Collections.singletonMap("jkube-healthcheck-spring-boot", enricherConfig));
        when(projectClassLoaders.isClassInCompileClasspath(true, REQUIRED_CLASSES))
          .thenReturn(true);
        when(projectClassLoaders.getCompileClassLoader())
          .thenReturn(new URLClassLoader(new URL[0], AbstractSpringBootHealthCheckEnricherTestSupport.class.getClassLoader()));

        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);

        Probe readinessProbe = enricher.getReadinessProbe();
        assertHealthCheckDelays(readinessProbe, 20, null, 120);

        Probe livenessProbe = enricher.getLivenessProbe();
        assertHealthCheckDelays(livenessProbe, 360, null, 120);
    }

    @Test
    void testCustomPropertiesForLivenessAndReadiness() {
        TreeMap<String, Object> enricherConfig = new TreeMap<>();
        enricherConfig.put("readinessProbeInitialDelaySeconds", "30");
        enricherConfig.put("readinessProbePeriodSeconds", "40");
        enricherConfig.put("livenessProbeInitialDelaySeconds", "460");
        enricherConfig.put("livenessProbePeriodSeconds", "50");
        context.getConfiguration().getProcessorConfig()
          .setConfig(Collections.singletonMap("jkube-healthcheck-spring-boot", enricherConfig));
        when(projectClassLoaders.isClassInCompileClasspath(true, REQUIRED_CLASSES))
          .thenReturn(true);
        when(projectClassLoaders.getCompileClassLoader())
          .thenReturn(new URLClassLoader(new URL[0], AbstractSpringBootHealthCheckEnricherTestSupport.class.getClassLoader()));

        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);

        Probe readinessProbe = enricher.getReadinessProbe();
        assertHealthCheckDelays(readinessProbe, 30, 40, null);

        Probe livenessProbe = enricher.getLivenessProbe();
        assertHealthCheckDelays(livenessProbe, 460, 50, null);
    }

    private void assertHTTPGetPathAndPort(Probe probe, String path, int port) {
      assertThat(probe).isNotNull()
          .extracting(Probe::getHttpGet).isNotNull()
          .hasFieldOrPropertyWithValue("path", path)
          .hasFieldOrPropertyWithValue("port.intVal", port);
    }

    private void assertHTTPGetSchemeAndPort(Probe probe, String scheme, int port) {
      assertThat(probe).isNotNull()
          .extracting(Probe::getHttpGet).isNotNull()
          .hasFieldOrPropertyWithValue("scheme", scheme)
          .hasFieldOrPropertyWithValue("port.intVal", port);
    }

    private void assertHealthCheckDelays(Probe probe, int initialDelaySeconds, Integer periodSeconds, Integer timeoutSeconds) {
      assertThat(probe).isNotNull()
          .hasFieldOrPropertyWithValue("initialDelaySeconds", initialDelaySeconds)
          .hasFieldOrPropertyWithValue("periodSeconds", periodSeconds)
          .hasFieldOrPropertyWithValue("timeoutSeconds", timeoutSeconds);
    }
}
