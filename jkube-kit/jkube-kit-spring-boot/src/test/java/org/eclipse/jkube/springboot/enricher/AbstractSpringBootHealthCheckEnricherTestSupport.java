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
import java.io.OutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Properties;
import java.util.TreeMap;

import io.fabric8.kubernetes.api.model.Probe;
import org.eclipse.jkube.kit.common.Dependency;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.eclipse.jkube.kit.common.util.ProjectClassLoaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jkube.springboot.enricher.SpringBootHealthCheckEnricher.REQUIRED_CLASSES;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
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
    private Properties props;

    protected JKubeEnricherContext context;

    private ProjectClassLoaders projectClassLoaders;

    @BeforeEach
    void init(@TempDir Path project) throws IOException {
        props = new Properties();
        projectClassLoaders = mock(ProjectClassLoaders.class, RETURNS_DEEP_STUBS);
        context = spy(JKubeEnricherContext.builder()
          .log(new KitLogger.SilentLogger())
          .project(JavaProject.builder()
            .properties(props)
            .baseDirectory(project.toFile())
            .outputDirectory(Files.createDirectory(project.resolve("target")).toFile())
            .groupId("com.example")
            .artifactId("foo")
            .dependency(Dependency.builder()
                .groupId("org.springframework.boot")
                .artifactId("spring-boot")
                .version(getSpringBootVersion())
              .build())
            .build())
          .processorConfig(new ProcessorConfig())
          .build());
        when(context.getProjectClassLoaders()).thenReturn(projectClassLoaders);
    }
    protected abstract String getSpringBootVersion();
    protected abstract String getActuatorDefaultBasePath();
    private boolean isSpringBootOne() {
        return getSpringBootVersion().substring(0, getSpringBootVersion().indexOf('.')).equals("1");
    }

    @Test
    void zeroConfig() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);

        Probe probe = enricher.buildProbe(10, null, null, 3, 1);
        assertHTTPGetPathAndPort(probe, getActuatorDefaultBasePath() + "/health", 8080);
    }

    @Test
    void withServerPort() {
        props.put("server.port", "8282");
        writeProps();

        Probe probe = new SpringBootHealthCheckEnricher(context)
          .buildProbe(10, null, null, 3, 1);
        assertHTTPGetPathAndPort(probe, getActuatorDefaultBasePath() + "/health", 8282);
    }

    @Test
    void withServerPortAndManagementPort() {
        props.put("server.port", "8282");
        props.put("management.port", "8383");
        props.put("management.server.port", "8383");
        writeProps();

        Probe probe = new SpringBootHealthCheckEnricher(context)
          .buildProbe(10, null, null, 3, 1);
        assertHTTPGetPathAndPort(probe, getActuatorDefaultBasePath() + "/health", 8383);
    }

    @Test
    void withServerPortAndManagementPortAndServerContextPath() {
        props.put("server.port", "8282");
        props.put("management.port", "8383");
        props.put("management.server.port", "8383");
        props.put("server.context-path", "/p1");
        props.put("server.servlet.context-path", "/p1");
        writeProps();

        Probe probe = new SpringBootHealthCheckEnricher(context)
          .buildProbe(10, null, null, 3, 1);
        assertHTTPGetPathAndPort(probe, getActuatorDefaultBasePath() + "/health", 8383);
    }

    @Test
    void withServerPortAndManagementPortAndServerContextPathAndActuatorBasePath() {
        assumeFalse(isSpringBootOne());
        props.put("server.port", "8282");
        props.put("management.port", "8383");
        props.put("management.server.port", "8383");
        props.put("server.context-path", "/p1");
        props.put("server.servlet.context-path", "/p1");
        props.put("management.endpoints.web.base-path", "/actuator-base-path");
        writeProps();

        Probe probe = new SpringBootHealthCheckEnricher(context)
          .buildProbe(10, null, null, 3, 1);
        assertHTTPGetPathAndPort(probe, "/actuator-base-path/health", 8383);
    }

    @Test
    void withServerPortAndManagementPortAndServerContextPathAndActuatorDefaultBasePathSlash() {
        props.put("server.port", "8282");
        props.put("management.port", "8383");
        props.put("management.server.port", "8383");
        props.put("server.context-path", "/p1");
        props.put("server.servlet.context-path", "/p1");
        props.put("management.endpoints.web.base-path", "/");
        writeProps();

        Probe probe = new SpringBootHealthCheckEnricher(context)
          .buildProbe(10, null, null, 3, 1);
        assertHTTPGetPathAndPort(probe, "/health", 8383);
    }

    @Test
    void withServerPortAndManagementPortAndManagementContextPath() {
        props.put("server.port", "8282");
        props.put("management.port", "8383");
        props.put("management.server.port", "8383");
        props.put("server.context-path", "/p1");
        props.put("server.servlet.context-path", "/p1");
        props.put("management.context-path", "/p2");
        props.put("management.server.servlet.context-path", "/p2");
        props.put("management.server.base-path", "/p2");
        writeProps();

        Probe probe = new SpringBootHealthCheckEnricher(context)
          .buildProbe(10, null, null, 3, 1);
        assertHTTPGetPathAndPort(probe, "/p2" + getActuatorDefaultBasePath() + "/health", 8383);
    }

    @Test
    void withServerPortAndManagementPortAndManagementContextPathAndActuatorDefaultBasePath() {
        assumeFalse(isSpringBootOne());
        props.put("server.port", "8282");
        props.put("management.port", "8383");
        props.put("management.server.port", "8383");
        props.put("server.context-path", "/p1");
        props.put("server.servlet.context-path", "/p1");
        props.put("management.context-path", "/p2");
        props.put("management.server.servlet.context-path", "/p2");
        props.put("management.server.base-path", "/p2");
        props.put("management.endpoints.web.base-path", "/actuator-base-path");
        writeProps();

        Probe probe = new SpringBootHealthCheckEnricher(context)
          .buildProbe(10, null, null, 3, 1);
        assertHTTPGetPathAndPort(probe, "/p2/actuator-base-path/health", 8383);
    }

    @Test
    void withServerPortAndManagementPortAndManagementContextPathAndActuatorDefaultBasePathSlash() {
        props.put("server.port", "8282");
        props.put("management.port", "8383");
        props.put("management.server.port", "8383");
        props.put("server.context-path", "/p1");
        props.put("server.servlet.context-path", "/p1");
        props.put("management.context-path", "/");
        props.put("management.server.servlet.context-path", "/");
        props.put("management.server.base-path", "/");
        props.put("management.endpoints.web.base-path", "/");
        writeProps();

        Probe probe = new SpringBootHealthCheckEnricher(context)
          .buildProbe(10, null, null, 3, 1);
        assertHTTPGetPathAndPort(probe, "/health", 8383);
    }

    @Test
    void withServerPortAndManagementPortAndManagementContextPathAndServletPath() {
        props.put("server.port", "8282");
        props.put("management.port", "8383");
        props.put("management.server.port", "8383");
        props.put("server.context-path", "/p1");
        props.put("server.servlet.context-path", "/p1");
        props.put("management.context-path", "/p2");
        props.put("management.server.servlet.context-path", "/p2");
        props.put("management.server.base-path", "/p2");
        props.put("server.servlet-path", "/servlet");
        props.put("server.servlet.path", "/servlet");
        props.put("spring.mvc.servlet.path", "/servlet");
        writeProps();

        Probe probe = new SpringBootHealthCheckEnricher(context)
          .buildProbe(10, null, null, 3, 1);
        assertHTTPGetPathAndPort(probe, "/p2" + getActuatorDefaultBasePath() + "/health", 8383);
    }

    @Test
    void withServerPortAndManagementPortAndManagementContextPathAndServletPathAndActuatorBasePath() {
        assumeFalse(isSpringBootOne());
        props.put("server.port", "8282");
        props.put("management.port", "8383");
        props.put("management.server.port", "8383");
        props.put("server.context-path", "/p1");
        props.put("server.servlet.context-path", "/p1");
        props.put("management.context-path", "/p2");
        props.put("management.server.servlet.context-path", "/p2");
        props.put("management.server.base-path", "/p2");
        props.put("server.servlet-path", "/servlet");
        props.put("server.servlet.path", "/servlet");
        props.put("spring.mvc.servlet.path", "/servlet");
        props.put("management.endpoints.web.base-path", "/actuator-base-path");
        writeProps();

        Probe probe = new SpringBootHealthCheckEnricher(context)
          .buildProbe(10, null, null, 3, 1);
        assertHTTPGetPathAndPort(probe, "/p2/actuator-base-path/health", 8383);
    }

    @Test
    void withServerPortAndManagementPortAndManagementContextPathAndServletPathAndActuatorDefaultBasePathSlash() {
        props.put("server.port", "8282");
        props.put("management.port", "8383");
        props.put("management.server.port", "8383");
        props.put("server.context-path", "/p1");
        props.put("server.servlet.context-path", "/p1");
        props.put("management.context-path", "/");
        props.put("management.server.servlet.context-path", "/");
        props.put("management.server.base-path", "/");
        props.put("server.servlet-path", "/servlet");
        props.put("server.servlet.path", "/servlet");
        props.put("spring.mvc.servlet.path", "/servlet");
        props.put("management.endpoints.web.base-path", "/");
        writeProps();

        Probe probe = new SpringBootHealthCheckEnricher(context)
          .buildProbe(10, null, null, 3, 1);
        assertHTTPGetPathAndPort(probe, "/health", 8383);
    }

    @Test
    void withServerPortAndManagementContextPath() {
        props.put("server.port", "8282");
        props.put("management.context-path", "/p1");
        props.put("management.server.servlet.context-path", "/p1");
        props.put("management.server.base-path", "/p1");
        writeProps();

        Probe probe = new SpringBootHealthCheckEnricher(context)
          .buildProbe(10, null, null, 3, 1);
        assertHTTPGetPathAndPort(probe, "/p1" + getActuatorDefaultBasePath() +"/health", 8282);
    }

    @Test
    void withServerPortAndManagementContextPathAndActuatorBasePath() {
        assumeFalse(isSpringBootOne());
        props.put("server.port", "8282");
        props.put("management.context-path", "/p1");
        props.put("management.server.servlet.context-path", "/p1");
        props.put("management.server.base-path", "/p1");
        props.put("management.endpoints.web.base-path", "/actuator-base-path");
        writeProps();

        Probe probe = new SpringBootHealthCheckEnricher(context)
          .buildProbe(10, null, null, 3, 1);
        assertHTTPGetPathAndPort(probe, "/p1/actuator-base-path/health", 8282);
    }

    @Test
    void withServerPortAndManagementContextPathAndActuatorDefaultBasePathSlash() {
        props.put("server.port", "8282");
        props.put("management.context-path", "/");
        props.put("management.server.servlet.context-path", "/");
        props.put("management.server.base-path", "/");
        props.put("management.endpoints.web.base-path", "/");
        writeProps();

        Probe probe = new SpringBootHealthCheckEnricher(context)
          .buildProbe(10, null, null, 3, 1);
        assertHTTPGetPathAndPort(probe, "/health", 8282);
    }

    @Test
    void withServerPortAndServerContextPathAndManagementContextPath() {
        props.put("server.port", "8282");
        props.put("management.context-path", "/p1");
        props.put("management.server.servlet.context-path", "/p1");
        props.put("management.server.base-path", "/p1");
        props.put("server.context-path", "/p2");
        props.put("server.servlet.context-path", "/p2");
        writeProps();

        Probe probe = new SpringBootHealthCheckEnricher(context)
          .buildProbe(10, null, null, 3, 1);
        assertHTTPGetPathAndPort(probe, "/p2/p1" + getActuatorDefaultBasePath() + "/health", 8282);
    }

    @Test
    void withServerPortAndServerContextPathAndManagementContextPathAndActuatorBasePath() {
        assumeFalse(isSpringBootOne());
        props.put("server.port", "8282");
        props.put("management.context-path", "/p1");
        props.put("management.server.servlet.context-path", "/p1");
        props.put("management.server.base-path", "/p1");
        props.put("server.context-path", "/p2");
        props.put("server.servlet.context-path", "/p2");
        props.put("management.endpoints.web.base-path", "/actuator-base-path");
        writeProps();

        Probe probe = new SpringBootHealthCheckEnricher(context)
          .buildProbe(10, null, null, 3, 1);
        assertHTTPGetPathAndPort(probe, "/p2/p1/actuator-base-path/health", 8282);
    }

    @Test
    void withServerPortAndServerContextPathAndManagementContextPathAndActuatorDefaultBasePathSlash() {
        props.put("server.port", "8282");
        props.put("management.context-path", "/");
        props.put("management.server.servlet.context-path", "/");
        props.put("management.server.base-path", "/");
        props.put("server.context-path", "/");
        props.put("server.servlet.context-path", "/");
        props.put("management.endpoints.web.base-path", "/");
        writeProps();

        Probe probe = new SpringBootHealthCheckEnricher(context)
          .buildProbe(10, null, null, 3, 1);
        assertHTTPGetPathAndPort(probe, "/health", 8282);
    }

    @Test
    void withServerPortAndServletPath() {
        props.put("server.port", "8282");
        props.put("server.servlet-path", "/servlet");
        props.put("server.servlet.path", "/servlet");
        props.put("spring.mvc.servlet.path", "/servlet");
        writeProps();

        Probe probe = new SpringBootHealthCheckEnricher(context)
          .buildProbe(10, null, null, 3, 1);
        assertHTTPGetPathAndPort(probe, "/servlet" + getActuatorDefaultBasePath() + "/health", 8282);
    }

    @Test
    void withServerPortAndServletAndActuatorBasePathPath() {
        assumeFalse(isSpringBootOne());
        props.put("server.port", "8282");
        props.put("server.servlet-path", "/servlet");
        props.put("server.servlet.path", "/servlet");
        props.put("spring.mvc.servlet.path", "/servlet");
        props.put("management.endpoints.web.base-path", "/actuator-base-path");
        writeProps();

        Probe probe = new SpringBootHealthCheckEnricher(context)
          .buildProbe(10, null, null, 3, 1);
        assertHTTPGetPathAndPort(probe, "/servlet/actuator-base-path/health", 8282);
    }

    @Test
    void withServerPortAndServletPathAndActuatorDefaultBasePathSlash() {
        props.put("server.port", "8282");
        props.put("server.servlet-path", "/");
        props.put("server.servlet.path", "/");
        props.put("spring.mvc.servlet.path", "/");
        props.put("management.endpoints.web.base-path", "/");
        writeProps();

        Probe probe = new SpringBootHealthCheckEnricher(context)
          .buildProbe(10, null, null, 3, 1);
        assertHTTPGetPathAndPort(probe, "/health", 8282);
    }

    @Test
    void withServerPortAndManagementContextPathAndServletPath() {
        props.put("server.port", "8282");
        props.put("server.servlet-path", "/servlet");
        props.put("server.servlet.path", "/servlet");
        props.put("spring.mvc.servlet.path", "/servlet");
        props.put("management.context-path", "/p1");
        props.put("management.server.servlet.context-path", "/p1");
        props.put("management.server.base-path", "/p1");
        writeProps();

        Probe probe = new SpringBootHealthCheckEnricher(context)
          .buildProbe(10, null, null, 3, 1);
        assertHTTPGetPathAndPort(probe, "/servlet/p1" + getActuatorDefaultBasePath() + "/health", 8282);
    }

    @Test
    void withServerPortAndManagementContextPathAndServletPathAndActuatorBasePath() {
        assumeFalse(isSpringBootOne());
        props.put("server.port", "8282");
        props.put("server.servlet-path", "/servlet");
        props.put("server.servlet.path", "/servlet");
        props.put("spring.mvc.servlet.path", "/servlet");
        props.put("management.context-path", "/p1");
        props.put("management.server.servlet.context-path", "/p1");
        props.put("management.server.base-path", "/p1");
        props.put("management.endpoints.web.base-path", "/actuator-base-path");
        writeProps();

        Probe probe = new SpringBootHealthCheckEnricher(context)
          .buildProbe(10, null, null, 3, 1);
        assertHTTPGetPathAndPort(probe, "/servlet/p1/actuator-base-path/health", 8282);
    }

    @Test
    void withServerPortAndManagementContextPathAndServletPathAndActuatorDefaultBasePathSlash() {
        props.put("server.port", "8282");
        props.put("server.servlet-path", "/");
        props.put("server.servlet.path", "/");
        props.put("spring.mvc.servlet.path", "/");
        props.put("management.context-path", "/");
        props.put("management.server.servlet.context-path", "/");
        props.put("management.server.base-path", "/");
        props.put("management.endpoints.web.base-path", "/");
        writeProps();

        Probe probe = new SpringBootHealthCheckEnricher(context)
          .buildProbe(10, null, null, 3, 1);
        assertHTTPGetPathAndPort(probe, "/health", 8282);
    }

    @Test
    void withServerPortAndServerContextPathAndManagementContextPathAndServletPath() {
        props.put("server.port", "8282");
        props.put("server.servlet-path", "/servlet");
        props.put("server.servlet.path", "/servlet");
        props.put("spring.mvc.servlet.path", "/servlet");
        props.put("management.context-path", "/p1");
        props.put("management.server.servlet.context-path", "/p1");
        props.put("management.server.base-path", "/p1");
        props.put("server.context-path", "/p2");
        props.put("server.servlet.context-path", "/p2");
        writeProps();

        Probe probe = new SpringBootHealthCheckEnricher(context)
          .buildProbe(10, null, null, 3, 1);
        assertHTTPGetPathAndPort(probe, "/p2/servlet/p1" + getActuatorDefaultBasePath() + "/health", 8282);
    }

    @Test
    void withServerPortAndServerContextPathAndManagementContextPathAndServletPathAndActuatorBasePath() {
        assumeFalse(isSpringBootOne());
        props.put("server.port", "8282");
        props.put("server.servlet-path", "/servlet");
        props.put("server.servlet.path", "/servlet");
        props.put("spring.mvc.servlet.path", "/servlet");
        props.put("management.context-path", "/p1");
        props.put("management.server.servlet.context-path", "/p1");
        props.put("management.server.base-path", "/p1");
        props.put("server.context-path", "/p2");
        props.put("server.servlet.context-path", "/p2");
        props.put("management.endpoints.web.base-path", "/actuator-base-path");
        writeProps();

        Probe probe = new SpringBootHealthCheckEnricher(context)
          .buildProbe(10, null, null, 3, 1);
        assertHTTPGetPathAndPort(probe, "/p2/servlet/p1/actuator-base-path/health", 8282);
    }

    @Test
    void withServerPortAndServerContextPathAndManagementContextPathAndServletPathAndActuatorDefaultBasePathSlash() {
        props.put("server.port", "8282");
        props.put("server.servlet-path", "/");
        props.put("server.servlet.path", "/");
        props.put("spring.mvc.servlet.path", "/");
        props.put("management.context-path", "/");
        props.put("management.server.servlet.context-path", "/");
        props.put("management.server.base-path", "/");
        props.put("server.context-path", "/");
        props.put("server.servlet.context-path", "/");
        props.put("management.endpoints.web.base-path", "/");
        writeProps();

        Probe probe = new SpringBootHealthCheckEnricher(context)
          .buildProbe(10, null, null, 3, 1);
        assertHTTPGetPathAndPort(probe, "/health", 8282);
    }

    @Test
    void schemeWithServerPort() {
        props.put("server.port", "8443");
        writeProps();

        Probe probe = new SpringBootHealthCheckEnricher(context)
          .buildProbe(10, null, null, 3, 1);
        assertHTTPGetSchemeAndPort(probe, "HTTP", 8443);
    }

    @Test
    void schemeWithServerPortAndManagementKeystore() {
        props.put("server.port", "8080");
        props.put("management.ssl.key-store", "classpath:keystore.p12");
        props.put("management.server.ssl.key-store", "classpath:keystore.p12");
        writeProps();

        Probe probe = new SpringBootHealthCheckEnricher(context)
          .buildProbe(10, null, null, 3, 1);
        assertHTTPGetSchemeAndPort(probe, "HTTP", 8080);
    }

    @Test
    void schemeWithServerPortAndServerKeystore() {
        props.put("server.port", "8443");
        props.put("server.ssl.key-store", "classpath:keystore.p12");
        writeProps();

        Probe probe = new SpringBootHealthCheckEnricher(context)
          .buildProbe(10, null, null, 3, 1);
        assertHTTPGetSchemeAndPort(probe, "HTTPS", 8443);
    }

    @Test
    void schemeWithServerPortAndManagementPortAndServerKeystore() {
        props.put("server.port", "8443");
        props.put("management.port", "8081");
        props.put("management.server.port", "8081");
        props.put("server.ssl.key-store", "classpath:keystore.p12");
        writeProps();

        Probe probe = new SpringBootHealthCheckEnricher(context)
          .buildProbe(10, null, null, 3, 1);
        assertHTTPGetSchemeAndPort(probe, "HTTP", 8081);
    }

    @Test
    void schemeWithServerPortAndManagementPortAndManagementKeystore() {
        props.put("server.port", "8080");
        props.put("management.port", "8443");
        props.put("management.server.port", "8443");
        props.put("management.ssl.key-store", "classpath:keystore.p12");
        props.put("management.server.ssl.key-store", "classpath:keystore.p12");
        writeProps();

        Probe probe = new SpringBootHealthCheckEnricher(context)
          .buildProbe(10, null, null, 3, 1);
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

    private void writeProps() {
        try (OutputStream fos = Files.newOutputStream(context.getProject().getOutputDirectory().toPath().resolve("application.properties"))) {
            props.store(fos, null);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
