/**
 * Copyright 2016 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package io.jkube.springboot.enricher;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.TreeMap;

import io.fabric8.kubernetes.api.model.Probe;
import io.jkube.kit.common.util.SpringBootConfigurationHelper;
import io.jkube.kit.common.util.SpringBootUtil;
import io.jkube.kit.config.resource.ProcessorConfig;
import io.jkube.maven.enricher.api.MavenEnricherContext;
import io.jkube.maven.enricher.api.model.Configuration;
import io.jkube.maven.enricher.api.util.ProjectClassLoaders;
import mockit.Expectations;
import mockit.Mock;
import mockit.MockUp;
import mockit.Mocked;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Check various configurations for spring-boot health checks
 *
 * @author nicola
 */
public abstract class AbstractSpringBootHealthCheckEnricherTestSupport {

    @Mocked
    protected MavenEnricherContext context;

    protected SpringBootConfigurationHelper propertyHelper;

    @Before
    public void init() {
        String version = getSpringBootVersion();
        this.propertyHelper = new SpringBootConfigurationHelper(Optional.of(version));

        new Expectations() {{
            context.getDependencyVersion(SpringBootConfigurationHelper.SPRING_BOOT_GROUP_ID, SpringBootConfigurationHelper.SPRING_BOOT_ARTIFACT_ID);
            result = Optional.of(version);
        }};
    }

    protected abstract String getSpringBootVersion();

    @Test
    public void testZeroConfig() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        Assert.assertNotNull(probe);
        Assert.assertNotNull(probe.getHttpGet());
        Assert.assertEquals(propertyHelper.getActuatorDefaultBasePath() + "/health", probe.getHttpGet().getPath());
        Assert.assertEquals(8080, probe.getHttpGet().getPort().getIntVal().intValue());
    }

    @Test
    public void testWithServerPort() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8282");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        Assert.assertNotNull(probe);
        Assert.assertNotNull(probe.getHttpGet());
        Assert.assertEquals(propertyHelper.getActuatorDefaultBasePath() + "/health", probe.getHttpGet().getPath());
        Assert.assertEquals(8282, probe.getHttpGet().getPort().getIntVal().intValue());
    }

    @Test
    public void testWithServerPortAndManagementPort() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8282");
        props.put(propertyHelper.getManagementPortPropertyKey(), "8383");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        Assert.assertNotNull(probe);
        Assert.assertNotNull(probe.getHttpGet());
        Assert.assertEquals(propertyHelper.getActuatorDefaultBasePath() + "/health", probe.getHttpGet().getPath());
        Assert.assertEquals(8383, probe.getHttpGet().getPort().getIntVal().intValue());
    }

    @Test
    public void testWithServerPortAndManagementPortAndServerContextPath() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8282");
        props.put(propertyHelper.getManagementPortPropertyKey(), "8383");
        props.put(propertyHelper.getServerContextPathPropertyKey(), "/p1");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        Assert.assertNotNull(probe);
        Assert.assertNotNull(probe.getHttpGet());
        Assert.assertEquals(propertyHelper.getActuatorDefaultBasePath() + "/health", probe.getHttpGet().getPath());
        Assert.assertEquals(8383, probe.getHttpGet().getPort().getIntVal().intValue());
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
        Assert.assertNotNull(probe);
        Assert.assertNotNull(probe.getHttpGet());
        Assert.assertEquals("/p2" + propertyHelper.getActuatorDefaultBasePath() + "/health", probe.getHttpGet().getPath());
        Assert.assertEquals(8383, probe.getHttpGet().getPort().getIntVal().intValue());
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
        Assert.assertNotNull(probe);
        Assert.assertNotNull(probe.getHttpGet());
        Assert.assertEquals("/p2" + propertyHelper.getActuatorDefaultBasePath() + "/health", probe.getHttpGet().getPath());
        Assert.assertEquals(8383, probe.getHttpGet().getPort().getIntVal().intValue());
    }

    @Test
    public void testWithServerPortAndManagementContextPath() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8282");
        props.put(propertyHelper.getManagementContextPathPropertyKey(), "/p1");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        Assert.assertNotNull(probe);
        Assert.assertNotNull(probe.getHttpGet());
        Assert.assertEquals("/p1" + propertyHelper.getActuatorDefaultBasePath() +"/health", probe.getHttpGet().getPath());
        Assert.assertEquals(8282, probe.getHttpGet().getPort().getIntVal().intValue());
    }

    @Test
    public void testWithServerPortAndServerContextPathAndManagementContextPath() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8282");
        props.put(propertyHelper.getManagementContextPathPropertyKey(), "/p1");
        props.put(propertyHelper.getServerContextPathPropertyKey(), "/p2");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        Assert.assertNotNull(probe);
        Assert.assertNotNull(probe.getHttpGet());
        Assert.assertEquals("/p2/p1" + propertyHelper.getActuatorDefaultBasePath() + "/health", probe.getHttpGet().getPath());
        Assert.assertEquals(8282, probe.getHttpGet().getPort().getIntVal().intValue());
    }

    @Test
    public void testWithServerPortAndServletPath() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8282");
        props.put(propertyHelper.getServletPathPropertyKey(), "/servlet");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        Assert.assertNotNull(probe);
        Assert.assertNotNull(probe.getHttpGet());
        Assert.assertEquals("/servlet" + propertyHelper.getActuatorDefaultBasePath() + "/health", probe.getHttpGet().getPath());
        Assert.assertEquals(8282, probe.getHttpGet().getPort().getIntVal().intValue());
    }

    @Test
    public void testWithServerPortAndManagementContextPathAndServletPath() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8282");
        props.put(propertyHelper.getServletPathPropertyKey(), "/servlet");
        props.put(propertyHelper.getManagementContextPathPropertyKey(), "/p1");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        Assert.assertNotNull(probe);
        Assert.assertNotNull(probe.getHttpGet());
        Assert.assertEquals("/servlet/p1" + propertyHelper.getActuatorDefaultBasePath() + "/health", probe.getHttpGet().getPath());
        Assert.assertEquals(8282, probe.getHttpGet().getPort().getIntVal().intValue());
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
        Assert.assertNotNull(probe);
        Assert.assertNotNull(probe.getHttpGet());
        Assert.assertEquals("/p2/servlet/p1" + propertyHelper.getActuatorDefaultBasePath() + "/health", probe.getHttpGet().getPath());
        Assert.assertEquals(8282, probe.getHttpGet().getPort().getIntVal().intValue());
    }

    @Test
    public void testSchemeWithServerPort() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8443");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        Assert.assertNotNull(probe);
        Assert.assertNotNull(probe.getHttpGet());
        Assert.assertEquals("HTTP", probe.getHttpGet().getScheme());
        Assert.assertEquals(8443, probe.getHttpGet().getPort().getIntVal().intValue());
    }

    @Test
    public void testSchemeWithServerPortAndManagementKeystore() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8080");
        props.put(propertyHelper.getManagementKeystorePropertyKey(), "classpath:keystore.p12");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        Assert.assertNotNull(probe);
        Assert.assertNotNull(probe.getHttpGet());
        Assert.assertEquals("HTTP", probe.getHttpGet().getScheme());
        Assert.assertEquals(8080, probe.getHttpGet().getPort().getIntVal().intValue());
    }

    @Test
    public void testSchemeWithServerPortAndServerKeystore() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8443");
        props.put(propertyHelper.getServerKeystorePropertyKey(), "classpath:keystore.p12");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        Assert.assertNotNull(probe);
        Assert.assertNotNull(probe.getHttpGet());
        Assert.assertEquals("HTTPS", probe.getHttpGet().getScheme());
        Assert.assertEquals(8443, probe.getHttpGet().getPort().getIntVal().intValue());
    }

    @Test
    public void testSchemeWithServerPortAndManagementPortAndServerKeystore() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8443");
        props.put(propertyHelper.getManagementPortPropertyKey(), "8081");
        props.put(propertyHelper.getServerKeystorePropertyKey(), "classpath:keystore.p12");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        Assert.assertNotNull(probe);
        Assert.assertNotNull(probe.getHttpGet());
        Assert.assertEquals("HTTP", probe.getHttpGet().getScheme());
        Assert.assertEquals(8081, probe.getHttpGet().getPort().getIntVal().intValue());
    }

    @Test
    public void testSchemeWithServerPortAndManagementPortAndManagementKeystore() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        Properties props = new Properties();
        props.put(propertyHelper.getServerPortPropertyKey(), "8080");
        props.put(propertyHelper.getManagementPortPropertyKey(), "8443");
        props.put(propertyHelper.getManagementKeystorePropertyKey(), "classpath:keystore.p12");

        Probe probe = enricher.buildProbe(props, 10, null, null, 3, 1);
        Assert.assertNotNull(probe);
        Assert.assertNotNull(probe.getHttpGet());
        Assert.assertEquals("HTTPS", probe.getHttpGet().getScheme());
        Assert.assertEquals(8443, probe.getHttpGet().getPort().getIntVal().intValue());
    }

    @Test
    public void testDefaultInitialDelayForLivenessAndReadiness() {
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);
        withProjectProperties(new Properties());

        new Expectations(){{
            context.getProjectClassLoaders();
            result = new ProjectClassLoaders(
                new URLClassLoader(new URL[0], AbstractSpringBootHealthCheckEnricherTestSupport.class.getClassLoader())) {
                @Override
                public boolean isClassInCompileClasspath(boolean all, String... clazz) {
                    return true;
                }
            };
        }};


        Probe probe = enricher.getReadinessProbe();
        Assert.assertNotNull(probe);
        Assert.assertEquals(10, probe.getInitialDelaySeconds().intValue());

        probe = enricher.getLivenessProbe();
        Assert.assertNotNull(probe);
        Assert.assertEquals(180, probe.getInitialDelaySeconds().intValue());
    }

    @Test
    public void testCustomInitialDelayForLivenessAndReadinessAndTimeout() {
        Map<String, TreeMap> globalConfig = new HashMap<>();
        TreeMap<String, String> enricherConfig = new TreeMap<>();
        globalConfig.put(SpringBootHealthCheckEnricher.ENRICHER_NAME, enricherConfig);
        enricherConfig.put("readinessProbeInitialDelaySeconds", "20");
        enricherConfig.put("livenessProbeInitialDelaySeconds", "360");
        enricherConfig.put("timeoutSeconds", "120");

        final ProcessorConfig config = new ProcessorConfig(null,null, globalConfig);
        new Expectations() {
            {
                context.getConfiguration();
                result = new Configuration.Builder().processorConfig(config).build();
                context.getProjectClassLoaders();
                result = new ProjectClassLoaders(
                    new URLClassLoader(new URL[0], AbstractSpringBootHealthCheckEnricherTestSupport.class.getClassLoader())) {
                    @Override
                    public boolean isClassInCompileClasspath(boolean all, String... clazz) {
                        return true;
                    }
                };
            }};
        withProjectProperties(new Properties());

        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);

        Probe probe = enricher.getReadinessProbe();
        Assert.assertNotNull(probe);
        Assert.assertEquals(20, probe.getInitialDelaySeconds().intValue());
        Assert.assertNull(probe.getPeriodSeconds());
        Assert.assertEquals(120, probe.getTimeoutSeconds().intValue());

        probe = enricher.getLivenessProbe();
        Assert.assertNotNull(probe);
        Assert.assertEquals(360, probe.getInitialDelaySeconds().intValue());
        Assert.assertNull(probe.getPeriodSeconds());
        Assert.assertEquals(120, probe.getTimeoutSeconds().intValue());


    }

    @Test
    public void testCustomPropertiesForLivenessAndReadiness() {
        Map<String, TreeMap> globalConfig = new HashMap<>();
        TreeMap<String, String> enricherConfig = new TreeMap<>();
        globalConfig.put(SpringBootHealthCheckEnricher.ENRICHER_NAME, enricherConfig);
        enricherConfig.put("readinessProbeInitialDelaySeconds", "30");
        enricherConfig.put("readinessProbePeriodSeconds", "40");
        enricherConfig.put("livenessProbeInitialDelaySeconds", "460");
        enricherConfig.put("livenessProbePeriodSeconds", "50");

        final ProcessorConfig config = new ProcessorConfig(null,null, globalConfig);
        new Expectations() {{
            context.getConfiguration(); result = new Configuration.Builder().processorConfig(config).build();
            context.getProjectClassLoaders();
            result = new ProjectClassLoaders(
                new URLClassLoader(new URL[0], AbstractSpringBootHealthCheckEnricherTestSupport.class.getClassLoader())) {
                @Override
                public boolean isClassInCompileClasspath(boolean all, String... clazz) {
                    return true;
                }
            };
        }};
        withProjectProperties(new Properties());
        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(context);

        Probe probe = enricher.getReadinessProbe();
        Assert.assertNotNull(probe);
        Assert.assertEquals(30, probe.getInitialDelaySeconds().intValue());
        Assert.assertEquals(40, probe.getPeriodSeconds().intValue());

        probe = enricher.getLivenessProbe();
        Assert.assertNotNull(probe);
        Assert.assertEquals(460, probe.getInitialDelaySeconds().intValue());
        Assert.assertEquals(50, probe.getPeriodSeconds().intValue());
    }

    private void withProjectProperties(final Properties properties) {
        new MockUp<SpringBootUtil>() {
            @Mock
            public Properties getSpringBootApplicationProperties(URLClassLoader urlClassLoader) {
                return properties;
            }
        };
    }
}
