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
package org.eclipse.jkube.springboot.watcher;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import org.apache.commons.io.FileUtils;
import org.eclipse.jkube.kit.common.Dependency;
import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.Plugin;
import org.eclipse.jkube.kit.config.access.ClusterAccess;
import org.eclipse.jkube.kit.config.access.ClusterConfiguration;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;
import org.eclipse.jkube.watcher.api.WatcherContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentMatchers;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@EnableKubernetesMockClient(crud = true)
class SpringBootWatcherIntegrationTest {

  @TempDir
  Path project;
  KubernetesClient kubernetesClient;
  private WatcherContext watcherContext;
  private File applicationProperties;
  private Deployment deployment;

  @BeforeEach
  void setUp() throws IOException {
    final Path target = Files.createDirectory(project.resolve("target"));
    applicationProperties = target.resolve("application.properties").toFile();
    deployment = new DeploymentBuilder()
      .withNewSpec()
      .withNewSelector().addToMatchLabels("app", "spring-boot-test").endSelector()
      .endSpec()
      .build();
    // Watcher Configuration
    final KitLogger logger = new KitLogger.SilentLogger();
    final JKubeServiceHub jKubeServiceHub = JKubeServiceHub.builder()
      .log(logger)
      .platformMode(RuntimeMode.KUBERNETES)
      .configuration(JKubeConfiguration.builder().build())
      .clusterAccess(new ClusterAccess(ClusterConfiguration.from(kubernetesClient.getConfiguration()).namespace("test").build()))
      .build();
    watcherContext = WatcherContext.builder()
      .logger(logger)
      .buildContext(JKubeConfiguration.builder()
        .project(JavaProject.builder()
          .outputDirectory(target.toFile())
          .compileClassPathElements(Collections.singletonList(target.resolve("spring-boot-lib.jar").toFile().getAbsolutePath()))
          .plugin(Plugin.builder()
            .groupId("org.springframework.boot")
            .artifactId("spring-boot-maven-plugin")
            .configuration(Collections.singletonMap("excludeDevtools", "false"))
            .build())
          .dependency(Dependency.builder()
            .groupId("org.springframework.boot")
            .artifactId("spring-boot")
            .version("2.5.0")
            .build())
          .dependency(Dependency.builder()
            .groupId("org.springframework.boot")
            .artifactId("spring-boot-devtools")
            .version("2.5.0")
            .type("jar")
            .file(target.resolve("spring-boot-devtools.jar").toFile())
            .build())
          .build())
        .build())
      .jKubeServiceHub(jKubeServiceHub)
      .build();
  }

  @Test
  void withMissingSelectorsThrowsException() {
    final SpringBootWatcher springBootWatcher = new SpringBootWatcher(watcherContext);
    assertThatThrownBy(() -> springBootWatcher.watch(Collections.emptyList(), null, Collections.emptyList(), PlatformMode.kubernetes))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Unable to open a channel to the remote pod.");
  }

  @Test
  void withNoRemoteSecretThrowsException() {
    final SpringBootWatcher springBootWatcher = new SpringBootWatcher(watcherContext);
    assertThatThrownBy(() -> springBootWatcher.watch(Collections.emptyList(), null, Collections.singletonList(deployment), PlatformMode.kubernetes))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("No spring.devtools.remote.secret property defined in application.properties or system properties");
  }

  @Test
  void withAllRequirementsShouldStartWatcherProcess() throws Exception {
    final Runtime runtime = mock(Runtime.class, RETURNS_DEEP_STUBS);
    FileUtils.write(applicationProperties, "spring.devtools.remote.secret=this-is-a-test", StandardCharsets.UTF_8);
    new SpringBootWatcher(runtime, watcherContext)
      .watch(Collections.emptyList(), null, Collections.singletonList(deployment), PlatformMode.kubernetes);
    verify(runtime).exec(ArgumentMatchers.<String>argThat(command ->
      command.startsWith("java -cp")
        && command.contains(
        absolutePath("spring-boot-lib.jar") + File.pathSeparator + absolutePath("spring-boot-devtools.jar"))
        && command.matches(".+ org\\.springframework\\.boot.devtools\\.RemoteSpringApplication http://localhost:\\d+$")
    ));
  }

  private String absolutePath(String jar) {
    return project.resolve("target").resolve(jar).toFile().getAbsolutePath();
  }

}
