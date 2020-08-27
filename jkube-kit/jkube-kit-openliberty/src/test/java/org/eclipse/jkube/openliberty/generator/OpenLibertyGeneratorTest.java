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
import java.util.Map;
import java.util.Properties;

import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.generator.javaexec.FatJarDetector;
import org.eclipse.jkube.kit.common.AssemblyFileSet;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;

import mockit.Expectations;
import mockit.Mocked;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.notNullValue;

@SuppressWarnings({"ResultOfMethodCallIgnored", "unused", "unchecked"})
public class OpenLibertyGeneratorTest {

    @Mocked
    KitLogger log;
    @Mocked
    private GeneratorContext context;
    @Mocked
    private JavaProject project;

  @Before
  public void setUp() throws Exception {
    // @formatter:off
    new Expectations() {{
      context.getProject(); result = project;
      project.getBaseDirectory(); result = "basedirectory"; minTimes = 0;

      String tempDir = Files.createTempDirectory("openliberty-test-project").toFile().getAbsolutePath();
      project.getBuildDirectory(); result = tempDir;
      project.getOutputDirectory(); result = tempDir; minTimes = 0;
      project.getVersion(); result = "1.0.0"; minTimes = 0;
    }};
    // @formatter:on
  }

  @Test
  public void getEnvWithFatJar(@Mocked FatJarDetector fatJarDetector, @Mocked FatJarDetector.Result mockResult) {
    // Given
    // @formatter:off
    new Expectations() {{
      fatJarDetector.scan(); result = mockResult;
      mockResult.getArchiveFile(); result = new File("/the/archive/file");
      mockResult.getMainClass(); result = OpenLibertyGenerator.LIBERTY_SELF_EXTRACTOR;
    }};
    // @formatter:on
    // When
    final Map<String, String> result = new OpenLibertyGenerator(context).getEnv(false);
    // Then
    assertThat(result, hasKey(OpenLibertyGenerator.LIBERTY_RUNNABLE_JAR));
    assertThat(result, hasKey(OpenLibertyGenerator.JAVA_APP_JAR));
  }

  @Test
  public void getEnvWithoutFatJar() {
    // Given
    final Properties properties = new Properties();
    properties.put("jkube.generator.openliberty.mainClass", "com.example.MainClass");
    properties.put("jkube.generator.java-exec.mainClass", "com.example.main");
    // @formatter:off
    new Expectations() {{
      context.getProject().getProperties(); result = properties;
    }};
    // @formatter:on
    // When
    final Map<String, String> result = new OpenLibertyGenerator(context).getEnv(false);
    // Then
    assertThat(result, not(hasKey(OpenLibertyGenerator.LIBERTY_RUNNABLE_JAR)));
    assertThat(result, not(hasKey(OpenLibertyGenerator.JAVA_APP_JAR)));
    assertThat(result, hasEntry("JAVA_MAIN_CLASS", "com.example.MainClass"));
  }

  @Test
  public void testExtractPorts() {
    // When
    final List<String> ports = new OpenLibertyGenerator(context).extractPorts();
    // Then
    assertThat(ports, notNullValue());
    assertThat(ports, hasItem("9080"));
  }

  @Test
  public void addAdditionalFiles() {
    // When
    final List<AssemblyFileSet> result = new OpenLibertyGenerator(context).addAdditionalFiles();
    // Then
    assertThat(result, containsInAnyOrder(
        hasProperty("directory", equalTo(new File("src/main/jkube-includes"))),
        hasProperty("directory", equalTo(new File("src/main/jkube-includes/bin"))),
        hasProperty("directory", equalTo(new File("src/main/liberty/config")))
    ));
  }
}
