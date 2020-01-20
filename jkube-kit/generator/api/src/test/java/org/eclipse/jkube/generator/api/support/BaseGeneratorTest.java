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
package org.eclipse.jkube.generator.api.support;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.eclipse.jkube.kit.build.maven.config.MavenBuildConfiguration;
import org.eclipse.jkube.kit.build.service.docker.ImageConfiguration;
import org.eclipse.jkube.kit.common.JkubeProject;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.image.build.OpenShiftBuildStrategy;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.eclipse.jkube.generator.api.FromSelector;
import org.eclipse.jkube.generator.api.GeneratorContext;
import mockit.Expectations;
import mockit.Mocked;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import static org.eclipse.jkube.kit.config.image.build.OpenShiftBuildStrategy.SourceStrategy.name;
import static org.eclipse.jkube.kit.config.image.build.OpenShiftBuildStrategy.SourceStrategy.kind;
import static org.eclipse.jkube.kit.config.image.build.OpenShiftBuildStrategy.SourceStrategy.namespace;

/**
 * @author roland
 * @since 10/01/17
 */
public class BaseGeneratorTest {

    @Mocked
    private GeneratorContext ctx;

    @Mocked
    private JkubeProject project;

    @Mocked
    private ProcessorConfig config;


    @Test
    public void fromAsConfigured() {
        final Properties projectProps = new Properties();
        projectProps.put("jkube.generator.from","propFrom");

        setupContextKubernetes(projectProps,"configFrom", null);
        BaseGenerator generator = createGenerator(null);
        assertEquals("configFrom",generator.getFromAsConfigured());

        setupContextKubernetes(projectProps,null, null);
        generator = createGenerator(null);
        assertEquals("propFrom",generator.getFromAsConfigured());
    }

    public TestBaseGenerator createGenerator(FromSelector fromSelector) {
        return fromSelector != null ?
                new TestBaseGenerator(ctx, "test-generator", fromSelector) :
                new TestBaseGenerator(ctx, "test-generator");
    }

    @Test
    public void defaultAddFrom() {
        Properties props = new Properties();
        for (boolean isOpenShift : new Boolean[]{false, true}) {
            for (boolean isRedHat : new Boolean[]{false, true}) {
                for (TestFromSelector selector : new TestFromSelector[]{null, new TestFromSelector(ctx, isRedHat)}) {
                    for (String from : new String[]{null, "openshift/testfrom"}) {
                        setupContext(props, isOpenShift, from, null);

                        BuildConfiguration.Builder builder = new BuildConfiguration.Builder();
                        BaseGenerator generator = createGenerator(selector);
                        generator.addFrom(builder);
                        BuildConfiguration config = builder.build();
                        if (isRedHat && isOpenShift && selector != null) {
                            if (from != null) {
                                assertEquals("testfrom:latest", config.getFrom());
                                assertFromExt(config.getFromExt(), "testfrom:latest", "openshift");
                            } else {
                                assertEquals("selectorIstagFromRedhat", config.getFrom());
                                assertFromExt(config.getFromExt(), selector.getIstagFrom(), "openshift");
                            }
                        } else {
                            if (from != null) {
                                assertEquals(config.getFrom(), from);
                                assertNull(config.getFromExt());
                            } else {
                                System.out.println(isRedHat + " " + isOpenShift);
                                assertNull(config.getFromExt());
                                assertEquals(config.getFrom(),
                                        selector != null ?
                                                (isOpenShift ?
                                                        selector.getS2iBuildFrom() :
                                                        selector.getDockerBuildFrom())
                                                : null);
                            }
                        }
                    }
                }
            }
        }
    }

    public void assertFromExt(Map<String, String> fromExt, String fromName, String namespaceName) {
        assertEquals(3, fromExt.keySet().size());
        assertEquals(fromName, fromExt.get(name.key()));
        assertEquals("ImageStreamTag", fromExt.get(kind.key()));
        assertEquals(namespaceName, fromExt.get(namespace.key()));
    }

    @Test
    public void addFromIstagModeWithSelector() {
        Properties props = new Properties();
        props.put("jkube.generator.fromMode","istag");

        for (String from : new String[] { null, "test_namespace/test_image:2.0"}) {
            setupContext(props, false, from, null);

            BuildConfiguration.Builder builder = new BuildConfiguration.Builder();
            BaseGenerator generator = createGenerator(new TestFromSelector(ctx, false));
            generator.addFrom(builder);
            BuildConfiguration config = builder.build();
            assertEquals(from == null ? "selectorIstagFromUpstream" : "test_image:2.0", config.getFrom());
            Map<String, String> fromExt = config.getFromExt();
            if (from != null) {
                assertFromExt(fromExt,"test_image:2.0", "test_namespace");
            } else {
                assertFromExt(fromExt, "selectorIstagFromUpstream", "openshift");
            }
        }
    }

    @Test
    public void addFromIstagModeWithoutSelector() {
        Properties props = new Properties();
        props.put("jkube.generator.fromMode","istag");
        for (String from : new String[] { null, "test_namespace/test_image:2.0"}) {
            setupContext(props, false, from, null);

            BuildConfiguration.Builder builder = new BuildConfiguration.Builder();
            BaseGenerator generator = createGenerator(null);
            generator.addFrom(builder);
            BuildConfiguration config = builder.build();
            assertEquals(from == null ? null : "test_image:2.0", config.getFrom());
            Map<String, String> fromExt = config.getFromExt();
            if (from == null) {
                assertNull(fromExt);
            } else {
                assertFromExt(fromExt, "test_image:2.0", "test_namespace");
            }
        }
    }

    @Test
    public void addFromIstagWithNameWithoutTag() {
        Properties props = new Properties();
        setupContext(props, false, "test_namespace/test_image", "istag");
        BuildConfiguration.Builder builder = new BuildConfiguration.Builder();
        BaseGenerator generator = createGenerator(null);
        generator.addFrom(builder);
        BuildConfiguration config = builder.build();
        assertEquals("test_image:latest",config.getFrom());
    }


    @Test
    public void addFromInvalidMode() {
        try {
            Properties props = new Properties();
            setupContextKubernetes(props, null, "blub");

            BuildConfiguration.Builder builder = new BuildConfiguration.Builder();
            BaseGenerator generator = createGenerator(null);
            generator.addFrom(builder);
            fail();
        } catch (IllegalArgumentException exp) {
            assertTrue(exp.getMessage().contains("fromMode"));
            assertTrue(exp.getMessage().contains("test-generator"));
        }
    }

    @Test
    public void shouldAddDefaultImage(@Mocked final ImageConfiguration ic1, @Mocked final ImageConfiguration ic2,
                                      @Mocked final MavenBuildConfiguration bc) {
        new Expectations() {{
            ic1.getBuildConfiguration(); result = bc; minTimes = 0;
            ic2.getBuildConfiguration(); result = null; minTimes = 0;
        }};
        BaseGenerator generator = createGenerator(null);
        assertTrue(generator.shouldAddImageConfiguration(Collections.<ImageConfiguration>emptyList()));
        assertFalse(generator.shouldAddImageConfiguration(Arrays.asList(ic1, ic2)));
        assertTrue(generator.shouldAddImageConfiguration(Arrays.asList(ic2)));
        assertFalse(generator.shouldAddImageConfiguration(Arrays.asList(ic1)));
    }

    @Test
    public void addLatestTagIfSnapshot() {
        new Expectations() {{
            ctx.getProject(); result = project;
            project.getVersion(); result = "1.2-SNAPSHOT";
        }};
        BuildConfiguration.Builder builder = new BuildConfiguration.Builder();
        BaseGenerator generator = createGenerator(null);
        generator.addLatestTagIfSnapshot(builder);;
        BuildConfiguration config = builder.build();
        List<String> tags = config.getTags();
        assertEquals(1, tags.size());
        assertTrue(tags.get(0).endsWith("latest"));
    }

    @Test
    public void getImageName() {
        setupNameContext(null, "config_test_name");
        BaseGenerator generator = createGenerator(null);
        assertEquals("config_test_name", generator.getImageName());

        setupNameContext("prop_test_name", null);
        generator = createGenerator(null);
        assertEquals("prop_test_name", generator.getImageName());

        setupNameContext("prop_test_name", "config_test_name");
        generator = createGenerator(null);
        assertEquals("config_test_name", generator.getImageName());

        setupNameContext(null, null);
        generator = createGenerator(null);
        assertEquals("%g/%a:%l", generator.getImageName());

    }

    @Test
    public void getRegistry() {
        Properties props = new Properties();
        props.put("jkube.generator.registry", "jkube.io");
        setupContextKubernetes(props, null, null);

        BaseGenerator generator = createGenerator(null);
        assertEquals("jkube.io", generator.getRegistry());
    }

    @Test
    public void getRegistryInOpenshift() {
        Properties props = new Properties();
        props.put("jkube.generator.registry", "jkube.io");
        props.put(RuntimeMode.FABRIC8_EFFECTIVE_PLATFORM_MODE, "openshift");
        setupContextOpenShift(props, null, null);

        BaseGenerator generator = createGenerator(null);
        assertNull(generator.getRegistry());
    }

    private void setupNameContext(String propertyName, final String configName) {
        final Properties props = new Properties();
        if (propertyName != null) {
            props.put("jkube.generator.name", propertyName);
        }
        new Expectations() {{
            ctx.getProject(); result = project;
            project.getProperties(); result = props;
            ctx.getConfig(); result = config;
            config.getConfig("test-generator", "name"); result = configName; minTimes = 0;
        }};
    }

    public void setupContext(Properties props, boolean isOpenShift, String from, String fromMode) {
        if (isOpenShift) {
            setupContextOpenShift(props, from, fromMode);
        } else {
            setupContextKubernetes(props, from, fromMode);
        }
    }

    public void setupContextKubernetes(final Properties projectProps, final String configFrom, final String configFromMode) {
        new Expectations() {{
            ctx.getProject(); result = project;
            project.getProperties(); result = projectProps;
            ctx.getConfig(); result = config;
            config.getConfig("test-generator", "from"); result = configFrom; minTimes = 0;
            config.getConfig("test-generator", "fromMode"); result = configFromMode; minTimes = 0;
            ctx.getRuntimeMode();result = RuntimeMode.kubernetes;minTimes = 0;
            ctx.getStrategy(); result = null; minTimes = 0;
        }};
    }

    public void setupContextOpenShift(final Properties projectProps, final String configFrom, final String configFromMode) {
        new Expectations() {{
            ctx.getProject(); result = project;
            project.getProperties(); result = projectProps;
            ctx.getConfig(); result = config;
            config.getConfig("test-generator", "from"); result = configFrom; minTimes = 0;
            config.getConfig("test-generator", "fromMode"); result = configFromMode; minTimes = 0;
            ctx.getRuntimeMode();result = RuntimeMode.openshift;minTimes = 0;
            ctx.getStrategy(); result = OpenShiftBuildStrategy.s2i; minTimes = 0;
        }};
    }

    private class TestBaseGenerator extends BaseGenerator {
        public TestBaseGenerator(GeneratorContext context, String name) {
            super(context, name);
        }

        public TestBaseGenerator(GeneratorContext context, String name, FromSelector fromSelector) {
            super(context, name, fromSelector);
        }

        @Override
        public boolean isApplicable(List<ImageConfiguration> configs) {
            return true;
        }

        @Override
        public List<ImageConfiguration> customize(List<ImageConfiguration> existingConfigs, boolean prePackagePhase) throws MojoExecutionException {
            return existingConfigs;
        }
    }

    private class TestFromSelector extends FromSelector {

        private boolean isRedHat;

        public TestFromSelector(GeneratorContext context, boolean isRedHat) {
            super(context);
            this.isRedHat = isRedHat;
        }

        @Override
        public boolean isRedHat() {
            return isRedHat;
        }

        @Override
        protected String getDockerBuildFrom() {
            return !isRedHat ? "selectorDockerFromUpstream" : "selectorDockerFromRedhat";
        }

        @Override
        protected String getS2iBuildFrom() {
            return !isRedHat ? "selectorS2iFromUpstream" : "selectorS2iFromRedhat";
        }

        @Override
        protected String getIstagFrom() {
            return !isRedHat ? "selectorIstagFromUpstream" : "selectorIstagFromRedhat";
        }
    }
}
