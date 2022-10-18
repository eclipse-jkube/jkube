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
package org.eclipse.jkube.springboot.enricher;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.TreeMap;

import io.fabric8.kubernetes.api.model.Probe;
import org.eclipse.jkube.kit.common.util.SpringBootConfigurationHelper;
import org.eclipse.jkube.kit.common.util.SpringBootUtil;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.eclipse.jkube.kit.enricher.api.model.Configuration;
import org.eclipse.jkube.kit.common.util.ProjectClassLoaders;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.eclipse.jkube.springboot.enricher.SpringBootHealthCheckEnricher.REQUIRED_CLASSES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Check various configurations for spring-boot health checks
 *
 * @author nicola
 */
public abstract class AbstractSpringBootHealthCheckEnricherTestSupport {

    protected JKubeEnricherContext context;

    protected SpringBootConfigurationHelper propertyHelper;

    protected ProjectClassLoaders projectClassLoaders;

    @Before
    public void init() {
        projectClassLoaders = mock(ProjectClassLoaders.class);
        context = mock(JKubeEnricherContext.class,RETURNS_DEEP_STUBS);
        String version = getSpringBootVersion();
        this.propertyHelper = new SpringBootConfigurationHelper(Optional.of(version));
        when(context.getDependencyVersion(SpringBootConfigurationHelper.SPRING_BOOT_GROUP_ID, SpringBootConfigurationHelper.SPRING_BOOT_ARTIFACT_ID)).thenReturn(Optional.of(version));
    }
    protected abstract String getSpringBootVersion();

    @Test
    public void testZeroConfig() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertNotNull(probe);
        assertNotNull(probe.getHttpGet());
        assertEquals(propertyHelper.getActuatorDefaultBasePath() + "/health", probe.getHttpGet().getPath());
        assertEquals(8080, probe.getHttpGet().getPort().getIntVal().intValue());
    }

    @Test
    public void testWithServerPort() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8282");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertNotNull(probe);
        assertNotNull(probe.getHttpGet());
        assertEquals(propertyHelper.getActuatorDefaultBasePath() + "/health", probe.getHttpGet().getPath());
        assertEquals(8282, probe.getHttpGet().getPort().getIntVal().intValue());
    }

    @Test
    public void testWithServerPortAndManagementPort() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8282");
        props.put(propertyHelper.getManagementPortPropertyKey(), "8383");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertNotNull(probe);
        assertNotNull(probe.getHttpGet());
        assertEquals(propertyHelper.getActuatorDefaultBasePath() + "/health", probe.getHttpGet().getPath());
        assertEquals(8383, probe.getHttpGet().getPort().getIntVal().intValue());
    }

    @Test
    public void testWithServerPortAndManagementPortAndServerContextPath() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8282");
        props.put(propertyHelper.getManagementPortPropertyKey(), "8383");
        props.put(propertyHelper.getServerContextPathPropertyKey(), "/p1");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertNotNull(probe);
        assertNotNull(probe.getHttpGet());
        assertEquals(propertyHelper.getActuatorDefaultBasePath() + "/health", probe.getHttpGet().getPath());
        assertEquals(8383, probe.getHttpGet().getPort().getIntVal().intValue());
    }

    @Test
    public void testWithServerPortAndManagementPortAndServerContextPathAndActuatorDefaultBasePath() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8282");
        props.put(propertyHelper.getManagementPortPropertyKey(), "8383");
        props.put(propertyHelper.getServerContextPathPropertyKey(), "/p1");
        props.put(propertyHelper.getActuatorBasePathPropertyKey(), "/p2");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertNotNull(probe);
        assertNotNull(probe.getHttpGet());
        assertEquals("/p2" + "/health", probe.getHttpGet().getPath());
        assertEquals(8383, probe.getHttpGet().getPort().getIntVal().intValue());
    }

    @Test
    public void testWithServerPortAndManagementPortAndServerContextPathAndActuatorDefaultBasePathSlash() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8282");
        props.put(propertyHelper.getManagementPortPropertyKey(), "8383");
        props.put(propertyHelper.getServerContextPathPropertyKey(), "/p1");
        props.put(propertyHelper.getActuatorBasePathPropertyKey(), "/");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertNotNull(probe);
        assertNotNull(probe.getHttpGet());
        assertEquals("/health", probe.getHttpGet().getPath());
        assertEquals(8383, probe.getHttpGet().getPort().getIntVal().intValue());
    }

    @Test
    public void testWithServerPortAndManagementPortAndManagementContextPath() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8282");
        props.put(propertyHelper.getManagementPortPropertyKey(), "8383");
        props.put(propertyHelper.getServerContextPathPropertyKey(), "/p1");
        props.put(propertyHelper.getManagementContextPathPropertyKey(), "/p2");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertNotNull(probe);
        assertNotNull(probe.getHttpGet());
        assertEquals("/p2" + propertyHelper.getActuatorDefaultBasePath() + "/health", probe.getHttpGet().getPath());
        assertEquals(8383, probe.getHttpGet().getPort().getIntVal().intValue());
    }

    @Test
    public void testWithServerPortAndManagementPortAndManagementContextPathAndActuatorDefaultBasePath() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8282");
        props.put(propertyHelper.getManagementPortPropertyKey(), "8383");
        props.put(propertyHelper.getServerContextPathPropertyKey(), "/p1");
        props.put(propertyHelper.getManagementContextPathPropertyKey(), "/p2");
        props.put(propertyHelper.getActuatorBasePathPropertyKey(), "/p3");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertNotNull(probe);
        assertNotNull(probe.getHttpGet());
        assertEquals("/p2/p3" + "/health", probe.getHttpGet().getPath());
        assertEquals(8383, probe.getHttpGet().getPort().getIntVal().intValue());
    }

    @Test
    public void testWithServerPortAndManagementPortAndManagementContextPathAndActuatorDefaultBasePathSlash() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8282");
        props.put(propertyHelper.getManagementPortPropertyKey(), "8383");
        props.put(propertyHelper.getServerContextPathPropertyKey(), "/p1");
        props.put(propertyHelper.getManagementContextPathPropertyKey(), "/");
        props.put(propertyHelper.getActuatorBasePathPropertyKey(), "/");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertNotNull(probe);
        assertNotNull(probe.getHttpGet());
        assertEquals("/health", probe.getHttpGet().getPath());
        assertEquals(8383, probe.getHttpGet().getPort().getIntVal().intValue());
    }

    @Test
    public void testWithServerPortAndManagementPortAndManagementContextPathAndServletPath() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8282");
        props.put(propertyHelper.getManagementPortPropertyKey(), "8383");
        props.put(propertyHelper.getServerContextPathPropertyKey(), "/p1");
        props.put(propertyHelper.getManagementContextPathPropertyKey(), "/p2");
        props.put(propertyHelper.getServletPathPropertyKey(), "/servlet");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertNotNull(probe);
        assertNotNull(probe.getHttpGet());
        assertEquals("/p2" + propertyHelper.getActuatorDefaultBasePath() + "/health", probe.getHttpGet().getPath());
        assertEquals(8383, probe.getHttpGet().getPort().getIntVal().intValue());
    }

    @Test
    public void testWithServerPortAndManagementPortAndManagementContextPathAndServletPathAndActuatorDefaultBasePath() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8282");
        props.put(propertyHelper.getManagementPortPropertyKey(), "8383");
        props.put(propertyHelper.getServerContextPathPropertyKey(), "/p1");
        props.put(propertyHelper.getManagementContextPathPropertyKey(), "/p2");
        props.put(propertyHelper.getServletPathPropertyKey(), "/servlet");
        props.put(propertyHelper.getActuatorBasePathPropertyKey(), "/p3");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertNotNull(probe);
        assertNotNull(probe.getHttpGet());
        assertEquals("/p2/p3" + "/health", probe.getHttpGet().getPath());
        assertEquals(8383, probe.getHttpGet().getPort().getIntVal().intValue());
    }

    @Test
    public void testWithServerPortAndManagementPortAndManagementContextPathAndServletPathAndActuatorDefaultBasePathSlash() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8282");
        props.put(propertyHelper.getManagementPortPropertyKey(), "8383");
        props.put(propertyHelper.getServerContextPathPropertyKey(), "/p1");
        props.put(propertyHelper.getManagementContextPathPropertyKey(), "/");
        props.put(propertyHelper.getServletPathPropertyKey(), "/servlet");
        props.put(propertyHelper.getActuatorBasePathPropertyKey(), "/");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertNotNull(probe);
        assertNotNull(probe.getHttpGet());
        assertEquals("/health", probe.getHttpGet().getPath());
        assertEquals(8383, probe.getHttpGet().getPort().getIntVal().intValue());
    }

    @Test
    public void testWithServerPortAndManagementContextPath() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8282");
        props.put(propertyHelper.getManagementContextPathPropertyKey(), "/p1");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertNotNull(probe);
        assertNotNull(probe.getHttpGet());
        assertEquals("/p1" + propertyHelper.getActuatorDefaultBasePath() +"/health", probe.getHttpGet().getPath());
        assertEquals(8282, probe.getHttpGet().getPort().getIntVal().intValue());
    }

    @Test
    public void testWithServerPortAndManagementContextPathAndActuatorDefaultBasePath() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8282");
        props.put(propertyHelper.getManagementContextPathPropertyKey(), "/p1");
        props.put(propertyHelper.getActuatorBasePathPropertyKey(), "/p2");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertNotNull(probe);
        assertNotNull(probe.getHttpGet());
        assertEquals("/p1/p2" +"/health", probe.getHttpGet().getPath());
        assertEquals(8282, probe.getHttpGet().getPort().getIntVal().intValue());
    }

    @Test
    public void testWithServerPortAndManagementContextPathAndActuatorDefaultBasePathSlash() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8282");
        props.put(propertyHelper.getManagementContextPathPropertyKey(), "/");
        props.put(propertyHelper.getActuatorBasePathPropertyKey(), "/");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertNotNull(probe);
        assertNotNull(probe.getHttpGet());
        assertEquals("/health", probe.getHttpGet().getPath());
        assertEquals(8282, probe.getHttpGet().getPort().getIntVal().intValue());
    }

    @Test
    public void testWithServerPortAndServerContextPathAndManagementContextPath() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8282");
        props.put(propertyHelper.getManagementContextPathPropertyKey(), "/p1");
        props.put(propertyHelper.getServerContextPathPropertyKey(), "/p2");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertNotNull(probe);
        assertNotNull(probe.getHttpGet());
        assertEquals("/p2/p1" + propertyHelper.getActuatorDefaultBasePath() + "/health", probe.getHttpGet().getPath());
        assertEquals(8282, probe.getHttpGet().getPort().getIntVal().intValue());
    }

    @Test
    public void testWithServerPortAndServerContextPathAndManagementContextPathAndActuatorDefaultBasePath() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8282");
        props.put(propertyHelper.getManagementContextPathPropertyKey(), "/p1");
        props.put(propertyHelper.getServerContextPathPropertyKey(), "/p2");
        props.put(propertyHelper.getActuatorBasePathPropertyKey(), "/p3");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertNotNull(probe);
        assertNotNull(probe.getHttpGet());
        assertEquals("/p2/p1/p3" + "/health", probe.getHttpGet().getPath());
        assertEquals(8282, probe.getHttpGet().getPort().getIntVal().intValue());
    }

    @Test
    public void testWithServerPortAndServerContextPathAndManagementContextPathAndActuatorDefaultBasePathSlash() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8282");
        props.put(propertyHelper.getManagementContextPathPropertyKey(), "/");
        props.put(propertyHelper.getServerContextPathPropertyKey(), "/");
        props.put(propertyHelper.getActuatorBasePathPropertyKey(), "/");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertNotNull(probe);
        assertNotNull(probe.getHttpGet());
        assertEquals("/health", probe.getHttpGet().getPath());
        assertEquals(8282, probe.getHttpGet().getPort().getIntVal().intValue());
    }

    @Test
    public void testWithServerPortAndServletPath() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8282");
        props.put(propertyHelper.getServletPathPropertyKey(), "/servlet");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertNotNull(probe);
        assertNotNull(probe.getHttpGet());
        assertEquals("/servlet" + propertyHelper.getActuatorDefaultBasePath() + "/health", probe.getHttpGet().getPath());
        assertEquals(8282, probe.getHttpGet().getPort().getIntVal().intValue());
    }

    @Test
    public void testWithServerPortAndServletAndActuatorDefaultBasePathPath() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8282");
        props.put(propertyHelper.getServletPathPropertyKey(), "/servlet");
        props.put(propertyHelper.getActuatorBasePathPropertyKey(), "/p1");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertNotNull(probe);
        assertNotNull(probe.getHttpGet());
        assertEquals("/servlet/p1" + "/health", probe.getHttpGet().getPath());
        assertEquals(8282, probe.getHttpGet().getPort().getIntVal().intValue());
    }

    @Test
    public void testWithServerPortAndServletPathAndActuatorDefaultBasePathSlash() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8282");
        props.put(propertyHelper.getServletPathPropertyKey(), "/");
        props.put(propertyHelper.getActuatorBasePathPropertyKey(), "/");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertNotNull(probe);
        assertNotNull(probe.getHttpGet());
        assertEquals("/health", probe.getHttpGet().getPath());
        assertEquals(8282, probe.getHttpGet().getPort().getIntVal().intValue());
    }

    @Test
    public void testWithServerPortAndManagementContextPathAndServletPath() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8282");
        props.put(propertyHelper.getServletPathPropertyKey(), "/servlet");
        props.put(propertyHelper.getManagementContextPathPropertyKey(), "/p1");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertNotNull(probe);
        assertNotNull(probe.getHttpGet());
        assertEquals("/servlet/p1" + propertyHelper.getActuatorDefaultBasePath() + "/health", probe.getHttpGet().getPath());
        assertEquals(8282, probe.getHttpGet().getPort().getIntVal().intValue());
    }

    @Test
    public void testWithServerPortAndManagementContextPathAndServletPathAndActuatorDefaultBasePath() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8282");
        props.put(propertyHelper.getServletPathPropertyKey(), "/servlet");
        props.put(propertyHelper.getManagementContextPathPropertyKey(), "/p1");
        props.put(propertyHelper.getActuatorBasePathPropertyKey(), "/p2");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertNotNull(probe);
        assertNotNull(probe.getHttpGet());
        assertEquals("/servlet/p1/p2" + "/health", probe.getHttpGet().getPath());
        assertEquals(8282, probe.getHttpGet().getPort().getIntVal().intValue());
    }

    @Test
    public void testWithServerPortAndManagementContextPathAndServletPathAndActuatorDefaultBasePathSlash() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8282");
        props.put(propertyHelper.getServletPathPropertyKey(), "/");
        props.put(propertyHelper.getManagementContextPathPropertyKey(), "/");
        props.put(propertyHelper.getActuatorBasePathPropertyKey(), "/");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertNotNull(probe);
        assertNotNull(probe.getHttpGet());
        assertEquals("/health", probe.getHttpGet().getPath());
        assertEquals(8282, probe.getHttpGet().getPort().getIntVal().intValue());
    }

    @Test
    public void testWithServerPortAndServerContextPathAndManagementContextPathAndServletPath() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8282");
        props.put(propertyHelper.getServletPathPropertyKey(), "/servlet");
        props.put(propertyHelper.getManagementContextPathPropertyKey(), "/p1");
        props.put(propertyHelper.getServerContextPathPropertyKey(), "/p2");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertNotNull(probe);
        assertNotNull(probe.getHttpGet());
        assertEquals("/p2/servlet/p1" + propertyHelper.getActuatorDefaultBasePath() + "/health", probe.getHttpGet().getPath());
        assertEquals(8282, probe.getHttpGet().getPort().getIntVal().intValue());
    }

    @Test
    public void testWithServerPortAndServerContextPathAndManagementContextPathAndServletPathAndActuatorDefaultBasePath() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8282");
        props.put(propertyHelper.getServletPathPropertyKey(), "/servlet");
        props.put(propertyHelper.getManagementContextPathPropertyKey(), "/p1");
        props.put(propertyHelper.getServerContextPathPropertyKey(), "/p2");
        props.put(propertyHelper.getActuatorBasePathPropertyKey(), "/p3");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertNotNull(probe);
        assertNotNull(probe.getHttpGet());
        assertEquals("/p2/servlet/p1/p3" + "/health", probe.getHttpGet().getPath());
        assertEquals(8282, probe.getHttpGet().getPort().getIntVal().intValue());
    }

    @Test
    public void testWithServerPortAndServerContextPathAndManagementContextPathAndServletPathAndActuatorDefaultBasePathSlash() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8282");
        props.put(propertyHelper.getServletPathPropertyKey(), "/");
        props.put(propertyHelper.getManagementContextPathPropertyKey(), "/");
        props.put(propertyHelper.getServerContextPathPropertyKey(), "/");
        props.put(propertyHelper.getActuatorBasePathPropertyKey(), "/");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertNotNull(probe);
        assertNotNull(probe.getHttpGet());
        assertEquals("/health", probe.getHttpGet().getPath());
        assertEquals(8282, probe.getHttpGet().getPort().getIntVal().intValue());
    }

    @Test
    public void testSchemeWithServerPort() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8443");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertNotNull(probe);
        assertNotNull(probe.getHttpGet());
        assertEquals("HTTP", probe.getHttpGet().getScheme());
        assertEquals(8443, probe.getHttpGet().getPort().getIntVal().intValue());
    }

    @Test
    public void testSchemeWithServerPortAndManagementKeystore() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8080");
        props.put(propertyHelper.getManagementKeystorePropertyKey(), "classpath:keystore.p12");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertNotNull(probe);
        assertNotNull(probe.getHttpGet());
        assertEquals("HTTP", probe.getHttpGet().getScheme());
        assertEquals(8080, probe.getHttpGet().getPort().getIntVal().intValue());
    }

    @Test
    public void testSchemeWithServerPortAndServerKeystore() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8443");
        props.put(propertyHelper.getServerKeystorePropertyKey(), "classpath:keystore.p12");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertNotNull(probe);
        assertNotNull(probe.getHttpGet());
        assertEquals("HTTPS", probe.getHttpGet().getScheme());
        assertEquals(8443, probe.getHttpGet().getPort().getIntVal().intValue());
    }

    @Test
    public void testSchemeWithServerPortAndManagementPortAndServerKeystore() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8443");
        props.put(propertyHelper.getManagementPortPropertyKey(), "8081");
        props.put(propertyHelper.getServerKeystorePropertyKey(), "classpath:keystore.p12");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertNotNull(probe);
        assertNotNull(probe.getHttpGet());
        assertEquals("HTTP", probe.getHttpGet().getScheme());
        assertEquals(8081, probe.getHttpGet().getPort().getIntVal().intValue());
    }

    @Test
    public void testSchemeWithServerPortAndManagementPortAndManagementKeystore() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8080");
        props.put(propertyHelper.getManagementPortPropertyKey(), "8443");
        props.put(propertyHelper.getManagementKeystorePropertyKey(), "classpath:keystore.p12");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        assertNotNull(probe);
        assertNotNull(probe.getHttpGet());
        assertEquals("HTTPS", probe.getHttpGet().getScheme());
        assertEquals(8443, probe.getHttpGet().getPort().getIntVal().intValue());
    }

    @Test
    public void testDefaultInitialDelayForLivenessAndReadiness() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        withProjectProperties(new Properties());
        when(context.getProjectClassLoaders()).thenReturn(projectClassLoaders);
        when(projectClassLoaders.isClassInCompileClasspath(true, REQUIRED_CLASSES)).thenReturn(true);
        when(projectClassLoaders.getCompileClassLoader()).thenReturn(new URLClassLoader(new URL[0], AbstractSpringBootHealthCheckEnricherTestSupport.class.getClassLoader()));
        Probe probe = enricher.getReadinessProbe();
        assertNotNull(probe);
        assertEquals(10, probe.getInitialDelaySeconds().intValue());

        probe = enricher.getLivenessProbe();
        assertNotNull(probe);
        assertEquals(180, probe.getInitialDelaySeconds().intValue());
    }

    @Test
    public void testCustomInitialDelayForLivenessAndReadinessAndTimeout() {
        Map<String, Map<String, Object>> globalConfig = new HashMap<>();
        TreeMap<String, Object> enricherConfig = new TreeMap<>();
        globalConfig.put(SpringBootHealthCheckEnricher.ENRICHER_NAME, enricherConfig);
        enricherConfig.put("readinessProbeInitialDelaySeconds", "20");
        enricherConfig.put("livenessProbeInitialDelaySeconds", "360");
        enricherConfig.put("timeoutSeconds", "120");

        final ProcessorConfig config = new ProcessorConfig(null,null, globalConfig);
        when(context.getProjectClassLoaders()).thenReturn(projectClassLoaders);
        when(projectClassLoaders.getCompileClassLoader())
            .thenReturn(new URLClassLoader(new URL[0], AbstractSpringBootHealthCheckEnricherTestSupport.class.getClassLoader()));
        when(projectClassLoaders.isClassInCompileClasspath(true, REQUIRED_CLASSES)).thenReturn(true);
        when(context.getConfiguration()).thenReturn(Configuration.builder().processorConfig(config).build());
        withProjectProperties(new Properties());

        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);

        Probe probe = enricher.getReadinessProbe();
        assertNotNull(probe);
        assertEquals(20, probe.getInitialDelaySeconds().intValue());
        assertNull(probe.getPeriodSeconds());
        assertEquals(120, probe.getTimeoutSeconds().intValue());

        probe = enricher.getLivenessProbe();
        assertNotNull(probe);
        assertEquals(360, probe.getInitialDelaySeconds().intValue());
        assertNull(probe.getPeriodSeconds());
        assertEquals(120, probe.getTimeoutSeconds().intValue());
    }

    @Test
    public void testCustomPropertiesForLivenessAndReadiness() {
        Map<String, Map<String, Object>> globalConfig = new HashMap<>();
        TreeMap<String, Object> enricherConfig = new TreeMap<>();
        globalConfig.put(SpringBootHealthCheckEnricher.ENRICHER_NAME, enricherConfig);
        enricherConfig.put("readinessProbeInitialDelaySeconds", "30");
        enricherConfig.put("readinessProbePeriodSeconds", "40");
        enricherConfig.put("livenessProbeInitialDelaySeconds", "460");
        enricherConfig.put("livenessProbePeriodSeconds", "50");

        final ProcessorConfig config = new ProcessorConfig(null,null, globalConfig);
        when(projectClassLoaders.isClassInCompileClasspath(true, REQUIRED_CLASSES)).thenReturn(true);
        when(projectClassLoaders.getCompileClassLoader()).thenReturn(new URLClassLoader(new URL[0], AbstractSpringBootHealthCheckEnricherTestSupport.class.getClassLoader()));
        when(context.getProjectClassLoaders()).thenReturn(projectClassLoaders);
        when(context.getConfiguration()).thenReturn(Configuration.builder().processorConfig(config).build());
        withProjectProperties(new Properties());
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);

        Probe probe = enricher.getReadinessProbe();
        assertNotNull(probe);
        assertEquals(30, probe.getInitialDelaySeconds().intValue());
        assertEquals(40, probe.getPeriodSeconds().intValue());

        probe = enricher.getLivenessProbe();
        assertNotNull(probe);
        assertEquals(460, probe.getInitialDelaySeconds().intValue());
        assertEquals(50, probe.getPeriodSeconds().intValue());
    }

    private void withProjectProperties(final Properties properties) {
        try (MockedStatic<SpringBootUtil> mockStatic = Mockito.mockStatic(SpringBootUtil.class)) {
            mockStatic.when(()-> SpringBootUtil.getSpringBootApplicationProperties(any())).thenReturn(properties);
        }

    }
}
