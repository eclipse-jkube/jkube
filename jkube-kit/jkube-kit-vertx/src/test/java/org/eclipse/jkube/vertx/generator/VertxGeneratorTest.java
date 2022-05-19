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
package org.eclipse.jkube.vertx.generator;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.Dependency;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.kit.common.util.JKubeProjectUtil;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class VertxGeneratorTest {

  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  private JavaProject project;

  private GeneratorContext context;
  private Dependency dropwizard;
  private Dependency core;

  @Before
  public void init() throws IOException {
    dropwizard = Dependency.builder().groupId("io.vertx").artifactId("vertx-dropwizard-metrics").version("3.4.2")
        .type("jar").scope("compile").file(folder.newFile("vertx-dropwizard-metrics.jar")).build();
    core = Dependency.builder().groupId("io.vertx").artifactId("vertx-core").version("3.4.2").type("jar")
        .scope("compile").file(folder.newFile("vertx-core.jar")).build();
    project = mock(JavaProject.class, RETURNS_DEEP_STUBS);
    KitLogger logger = mock(KitLogger.class, RETURNS_DEEP_STUBS);
    context = GeneratorContext.builder()
        .logger(logger)
        .project(project)
        .build();
  }

  @Test
  public void testDefaultOptions() {
    // Given
    when(project.getOutputDirectory()).thenReturn(new File("target/tmp/target"));
    // When
    List<String> list = new VertxGenerator(context).getExtraJavaOptions();
    // Then
    assertThat(list).containsOnly("-Dvertx.cacheDirBase=/tmp/vertx-cache", "-Dvertx.disableDnsResolver=true");
  }

  @Test
  public void testWithMetrics() {
    // Given
    when(project.getDependencies()).thenReturn(Arrays.asList(dropwizard, core));
    // When
    List<String> list = new VertxGenerator(context).getExtraJavaOptions();
    // Then
    assertThat(list).containsOnly(
            // Default entries
            "-Dvertx.cacheDirBase=/tmp/vertx-cache", "-Dvertx.disableDnsResolver=true",
            // Metrics entries
            "-Dvertx.metrics.options.enabled=true", "-Dvertx.metrics.options.jmxEnabled=true", "-Dvertx.metrics.options.jmxDomain=vertx");
  }

    @Test
    public void testWithInfinispanClusterManager() {
      try (MockedStatic<JKubeProjectUtil> mockedJKubeProjectUtil = mockStatic(JKubeProjectUtil.class)) {
        // Given
        mockedJKubeProjectUtil
            .when(() -> JKubeProjectUtil.hasResource(project, "META-INF/services/io.vertx.core.spi.cluster.ClusterManager"))
            .thenReturn(true);
        mockedJKubeProjectUtil
            .when(() -> JKubeProjectUtil.hasDependency(project, "io.vertx", "vertx-infinispan"))
            .thenReturn(true);
        when(project.getOutputDirectory()).thenReturn(new File("target/tmp/target"));
        // When
        Map<String, String> env = new VertxGenerator(context).getEnv(true);
        // Then
        assertThat(env)
            .containsEntry("JAVA_OPTIONS", "-Dvertx.cacheDirBase=/tmp/vertx-cache -Dvertx.disableDnsResolver=true " +
                // Force IPv4
                "-Djava.net.preferIPv4Stack=true")
            .containsEntry("JAVA_ARGS", "-cluster");
      }
    }
}
