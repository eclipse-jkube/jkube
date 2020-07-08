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
package org.eclipse.jkube.quarkus.generator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.JKubeBuildStrategy;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;

import mockit.Expectations;
import mockit.Mocked;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertNotNull;

/**
 * @author jzuriaga
 */
public class QuarkusGeneratorTest {

    private static final String BASE_JAVA_IMAGE = "java:latest";
    private static final String BASE_NATIVE_IMAGE = "fedora:latest";

    @Mocked
    private GeneratorContext ctx;

    @Mocked
    private JavaProject project;

    private ProcessorConfig config;
    private Properties projectProps = new Properties();

    @Before
    public void setUp() throws IOException {
        createFakeRunnerJar();
        config = new ProcessorConfig();
        projectProps.put("jkube.generator.name", "quarkus");
        // @formatter:off
        new Expectations() {{
            project.getVersion(); result = "0.0.1-SNAPSHOT";
            project.getBuildDirectory(); result = new File("target/tmp").getAbsolutePath();
            project.getProperties(); result = projectProps;
            ctx.getProject(); result = project;
            ctx.getConfig(); result = config;
            ctx.getRuntimeMode(); result = RuntimeMode.OPENSHIFT; minTimes = 0;
            ctx.getStrategy(); result = JKubeBuildStrategy.s2i; minTimes = 0;
        }};
        // @formatter:on
    }

    @Test
    public void testCustomizeReturnsDefaultFrom () {
        QuarkusGenerator generator = new QuarkusGenerator(ctx);
        List<ImageConfiguration> existingImages = new ArrayList<>();

        final List<ImageConfiguration> result = generator.customize(existingImages, true);

        assertBuildFrom(result, "openjdk:11");
    }

    @Test
    public void testCustomizeReturnsDefaultFromWhenNative () throws IOException {
        setNativeConfig();

        QuarkusGenerator generator = new QuarkusGenerator(ctx);
        List<ImageConfiguration> existingImages = new ArrayList<>();

        List<ImageConfiguration> resultImages = generator.customize(existingImages, true);

        assertBuildFrom(resultImages, "registry.access.redhat.com/ubi8/ubi-minimal:8.1");
    }

    @Test
    public void testCustomizeReturnsConfiguredFrom () {
        config.getConfig().put("quarkus", Collections.singletonMap("from", BASE_JAVA_IMAGE));
        QuarkusGenerator generator = new QuarkusGenerator(ctx);
        List<ImageConfiguration> existingImages = new ArrayList<>();

        List<ImageConfiguration> resultImages = generator.customize(existingImages, true);

        assertBuildFrom(resultImages, BASE_JAVA_IMAGE);
    }

    @Test
    public void testCustomizeReturnsConfiguredFromWhenNative () throws IOException {
        setNativeConfig();
        config.getConfig().put("quarkus", Collections.singletonMap("from", BASE_NATIVE_IMAGE));

        QuarkusGenerator generator = new QuarkusGenerator(ctx);
        List<ImageConfiguration> existingImages = new ArrayList<>();

        List<ImageConfiguration> resultImages = generator.customize(existingImages, true);

        assertBuildFrom(resultImages, BASE_NATIVE_IMAGE);
    }

    @Test
    public void testCustomizeReturnsPropertiesFrom () {
        projectProps.put("jkube.generator.quarkus.from", BASE_JAVA_IMAGE);

        QuarkusGenerator generator = new QuarkusGenerator(ctx);
        List<ImageConfiguration> resultImages = null;
        List<ImageConfiguration> existingImages = new ArrayList<>();

        resultImages = generator.customize(existingImages, true);

        assertBuildFrom(resultImages, BASE_JAVA_IMAGE);
    }


    @Test
    public void testCustomizeReturnsPropertiesFromWhenNative () throws IOException {
        setNativeConfig();
        projectProps.put("jkube.generator.quarkus.from", BASE_NATIVE_IMAGE);

        QuarkusGenerator generator = new QuarkusGenerator(ctx);
        List<ImageConfiguration> resultImages = null;
        List<ImageConfiguration> existingImages = new ArrayList<>();

        resultImages = generator.customize(existingImages, true);

        assertBuildFrom(resultImages, BASE_NATIVE_IMAGE);
    }

    private void assertBuildFrom (List<ImageConfiguration> resultImages, String baseImage) {
        assertNotNull(resultImages);
        assertThat(resultImages, hasSize(1));
        assertThat(resultImages,
                hasItem(hasProperty("buildConfiguration", hasProperty("from", equalTo(baseImage)))));
    }

    private void createFakeRunnerJar () throws IOException {
        File baseDir = createBaseDir();
        File runnerJar = new File(baseDir, "sample-runner.jar");
        runnerJar.createNewFile();
    }

    private File createBaseDir () {
        File baseDir = new File("target", "tmp");
        baseDir.mkdir();
        return baseDir;
    }

    private void setNativeConfig () throws IOException {
        createFakeNativeImage();
        projectProps.put("jkube.generator.quarkus.nativeImage", "true");
    }

    private void createFakeNativeImage () throws IOException {
        File baseDir = createBaseDir();
        File runnerExec = new File(baseDir, "sample-runner");
        runnerExec.createNewFile();
    }

}