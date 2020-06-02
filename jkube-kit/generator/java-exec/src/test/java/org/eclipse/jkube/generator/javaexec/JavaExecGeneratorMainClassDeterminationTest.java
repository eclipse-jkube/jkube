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
package org.eclipse.jkube.generator.javaexec;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.kit.build.service.docker.ImageConfiguration;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.FileUtil;
import org.eclipse.jkube.kit.config.image.build.OpenShiftBuildStrategy;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;

import mockit.Expectations;
import mockit.Injectable;
import mockit.Mocked;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Checking how the JavaExecGenerator checks the need to set a main class as environment variable JAVA_MAIN_CLASS
 * in various situations
 *
 * @author Oliver Weise
 */
public class JavaExecGeneratorMainClassDeterminationTest {

    @Mocked
    private KitLogger log;
    @Mocked
    private JavaProject project;
    @Mocked
    private ProcessorConfig processorConfig;
    @Mocked
    private FatJarDetector fatJarDetector;
    @Mocked
    private FatJarDetector.Result fatJarDetectorResult;
    @Mocked
    private MainClassDetector mainClassDetector;

    @Before
    public void setUp() throws Exception{
        new Expectations() {{
            project.getVersion();
            result = "1.33.7-SNAPSHOT";
            project.getBuildDirectory();
            result = "/the/directory";
            project.getOutputDirectory();
            result = "/the/output/directory";
        }};
    }

    /**
     * The main class is determined via config in a non-fat-jar deployment
     *
     */
    @Test
    public void testMainClassDeterminationFromConfig() {
        // Given
        new Expectations() {{
            processorConfig.getConfig("java-exec", "mainClass");
            result = "the.main.ClassName";
            processorConfig.getConfig("java-exec", "name");
            result = "TheImageName";
        }};
        final GeneratorContext generatorContext = GeneratorContext.builder()
                .project(project)
                .config(processorConfig)
                .strategy(OpenShiftBuildStrategy.docker)
                .logger(log)
                .build();

        JavaExecGenerator generator = new JavaExecGenerator(generatorContext);

        List<ImageConfiguration> customized = generator.customize(new ArrayList<>(), false);

        assertEquals("1 images returned", 1, customized.size());

        ImageConfiguration imageConfig = customized.get(0);

        assertEquals("Image name", "TheImageName", imageConfig.getName());
        assertEquals("Main Class set as environment variable",
                "the.main.ClassName",
                imageConfig.getBuildConfiguration().getEnv().get(JavaExecGenerator.JAVA_MAIN_CLASS_ENV_VAR));
    }

    /**
     * The main class is determined via main class detection in a non-fat-jar deployment
     *
     */
    @Test
    public void testMainClassDeterminationFromDetectionOnNonFatJar(@Injectable File baseDir) {
        new Expectations() {{
            project.getBaseDirectory();
            result = baseDir;
            fatJarDetector.scan();
            result = null;
            mainClassDetector.getMainClass();
            result = "the.detected.MainClass";
            processorConfig.getConfig("java-exec", "name");
            result = "TheNonFatJarImageName";
        }};

        final GeneratorContext generatorContext = GeneratorContext.builder()
                .project(project)
                .config(processorConfig)
                .strategy(OpenShiftBuildStrategy.docker)
                .logger(log)
                .build();

        JavaExecGenerator generator = new JavaExecGenerator(generatorContext);

        List<ImageConfiguration> customized = generator.customize(new ArrayList<>(), false);

        assertEquals("1 images returned", 1, customized.size());

        ImageConfiguration imageConfig = customized.get(0);

        assertEquals("Image name", "TheNonFatJarImageName", imageConfig.getName());
        assertEquals("Main Class set as environment variable",
                "the.detected.MainClass",
                imageConfig.getBuildConfiguration().getEnv().get(JavaExecGenerator.JAVA_MAIN_CLASS_ENV_VAR));
    }

    /**
     * The main class is determined as the Main-Class of a fat jar
     *
     */
    @Test
    public void testMainClassDeterminationFromFatJar(
            @Mocked FileUtil fileUtil, @Injectable File baseDir, @Injectable File fatJarArchive) {
        new Expectations() {{
            project.getBaseDirectory();
            result = baseDir;
            fileUtil.getRelativePath(withInstanceOf(File.class), withInstanceOf(File.class));
            result = baseDir;
            fatJarDetector.scan();
            result = fatJarDetectorResult;
            fatJarDetectorResult.getArchiveFile();
            result = fatJarArchive;
            processorConfig.getConfig("java-exec", "name");
            result = "TheFatJarImageName";
        }};
        final GeneratorContext generatorContext = GeneratorContext.builder()
                .project(project)
                .config(processorConfig)
                .strategy(OpenShiftBuildStrategy.docker)
                .logger(log)
                .build();

        JavaExecGenerator generator = new JavaExecGenerator(generatorContext);

        final List<ImageConfiguration> images = new ArrayList<>();

        List<ImageConfiguration> customized = generator.customize(images, false);

        assertEquals("1 images returned", 1, customized.size());

        ImageConfiguration imageConfig = customized.get(0);

        assertEquals("Image name", "TheFatJarImageName", imageConfig.getName());
        assertNull("Main Class is NOT set as environment variable#",
                imageConfig.getBuildConfiguration().getEnv().get(JavaExecGenerator.JAVA_MAIN_CLASS_ENV_VAR));
    }


}
