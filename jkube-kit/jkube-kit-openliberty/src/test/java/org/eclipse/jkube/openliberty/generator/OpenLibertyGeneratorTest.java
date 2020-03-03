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
package org.eclipse.jkube.openliberty.generator;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.jar.Attributes;

import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.generator.javaexec.FatJarDetector;
import org.eclipse.jkube.kit.build.core.config.JKubeAssemblyConfiguration;
import org.eclipse.jkube.kit.common.JKubeProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.junit.Test;

import mockit.Expectations;
import mockit.Invocation;
import mockit.Mock;
import mockit.MockUp;
import mockit.Mocked;

public class OpenLibertyGeneratorTest {

    @Mocked
    KitLogger log;

    @Mocked
    private GeneratorContext context;

    @Mocked
    private JKubeProject project;

    @Test
    public void testLibertyRunnable() throws IOException {

        new MockFatJarDetector(true);

        OpenLibertyGenerator generator = new OpenLibertyGenerator(createGeneratorContext());

        generator.addAssembly(new JKubeAssemblyConfiguration.Builder());
        assertTrue("The LIBERTY_RUNNABLE_JAR env var should be set",
                generator.getEnv(false).containsKey(OpenLibertyGenerator.LIBERTY_RUNNABLE_JAR));
        assertTrue("The JAVA_APP_DIR env var should be set",
                generator.getEnv(false).containsKey(OpenLibertyGenerator.JAVA_APP_JAR));

    }

    @Test
    public void testExtractPorts() throws IOException {

        OpenLibertyGenerator generator = new OpenLibertyGenerator(createGeneratorContext());
        List<String> ports = generator.extractPorts();
        assertNotNull(ports);
        assertTrue("The list of ports should contain 9080", ports.contains("9080"));

    }

    private GeneratorContext createGeneratorContext() throws IOException {
        new Expectations() {
            {
                context.getProject();
                result = project;
                project.getBaseDirectory();
                minTimes = 0;
                result = "basedirectory";

                String tempDir = Files.createTempDirectory("openliberty-test-project").toFile().getAbsolutePath();

                project.getBuildDirectory();
                result = tempDir;
                project.getOutputDirectory();
                result = tempDir;
                minTimes = 0;
                project.getVersion();
                result = "1.0.0";
                minTimes = 0;
            }
        };
        return context;

    }

    public static class MockFatJarDetector extends MockUp<FatJarDetector> {

        private final boolean findClass;

        public MockFatJarDetector(boolean findClass) {
            this.findClass = findClass;
        }

        @Mock
        FatJarDetector.Result scan(Invocation invocation) {

            if (!findClass) {
                return null;
            }

            FatJarDetector detector = invocation.getInvokedInstance();
            return detector.new Result(new File("/the/archive/file"), OpenLibertyGenerator.LIBERTY_SELF_EXTRACTOR,
                    new Attributes());
        }

    }

}
