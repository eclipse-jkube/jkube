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

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.generator.javaexec.FatJarDetector;
import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;

import mockit.Expectations;
import mockit.Mocked;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

public class OpenLibertyGeneratorTest {

    @Mocked
    KitLogger log;
    @Mocked
    private GeneratorContext context;
    @Mocked
    private JavaProject project;
    private OpenLibertyGenerator generator;

  @Before
  public void setUp() throws Exception {
    new Expectations() {{
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
    }};
    generator = new OpenLibertyGenerator(context);
  }

  @Test
  public void testLibertyRunnable(@Mocked FatJarDetector fatJarDetector, @Mocked FatJarDetector.Result mockResult) {
    // Given
    new Expectations() {{
      fatJarDetector.scan();
      result = mockResult;
      mockResult.getArchiveFile();
      result = new File("/the/archive/file");
      mockResult.getMainClass();
      result = OpenLibertyGenerator.LIBERTY_SELF_EXTRACTOR;
    }};
    // When
    generator.addAssembly(AssemblyConfiguration.builder());
    // Then
    assertThat(generator.getEnv(false), hasKey(OpenLibertyGenerator.LIBERTY_RUNNABLE_JAR));
    assertThat(generator.getEnv(false), hasKey(OpenLibertyGenerator.JAVA_APP_JAR));
  }

  @Test
  public void testExtractPorts() {
    // When
    final List<String> ports = generator.extractPorts();
    // Then
    assertThat(ports, notNullValue());
    assertThat(ports, hasItem("9080"));
  }
}
