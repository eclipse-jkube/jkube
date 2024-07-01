/*
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

import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
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
import org.eclipse.jkube.kit.common.util.EnvUtil;
import org.eclipse.jkube.kit.common.access.ClusterConfiguration;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;
import org.eclipse.jkube.watcher.api.WatcherContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@EnableKubernetesMockClient(crud = true)
class SpringBootWatcherIntegrationTest {

  @TempDir
  Path project;
  KubernetesClient kubernetesClient;
  private WatcherContext watcherContext;
  private Path target;
  private Deployment deployment;
  private KitLogger logger;

  @BeforeEach
  void setUp() throws IOException {
    target = Files.createDirectory(project.resolve("target"));
    deployment = new DeploymentBuilder()
      .withNewSpec()
      .withNewSelector().addToMatchLabels("app", "spring-boot-test").endSelector()
      .endSpec()
      .build();
    // Watcher Configuration
    logger = spy(new KitLogger.SilentLogger());
    final JKubeServiceHub jKubeServiceHub = JKubeServiceHub.builder()
      .log(logger)
      .platformMode(RuntimeMode.KUBERNETES)
      .configuration(JKubeConfiguration.builder()
        .clusterConfiguration(ClusterConfiguration.from(kubernetesClient.getConfiguration()).build())
        .build())
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
  @DisabledOnOs(OS.WINDOWS)
  void withAllRequirementsShouldStartWatcherProcess() throws Exception {
    try {
      // Given
      Map<String, String> propMap = new HashMap<>();
      File dummyJavaArtifact =  new File(Objects.requireNonNull(SpringBootWatcherIntegrationTest.class.getResource("/dummy-java")).getFile());
      Path testJavaHome = project.resolve("java-home");
      Files.createDirectory(testJavaHome);
      Files.createDirectory(testJavaHome.resolve("bin"));
      Files.copy(dummyJavaArtifact.toPath(), testJavaHome.resolve("bin").resolve("java"), StandardCopyOption.COPY_ATTRIBUTES);
      propMap.put("java.home", testJavaHome.toString());
      propMap.put("os.name", "linux");
      EnvUtil.overridePropertyGetter(propMap::get);
      Files.createFile(target.resolve("spring-boot-lib.jar"));
      Files.createFile(target.resolve("spring-boot-devtools.jar"));
      FileUtils.write(target.resolve("application.properties").toFile(), "spring.devtools.remote.secret=this-is-a-test", StandardCharsets.UTF_8);
      // When
      new SpringBootWatcher(watcherContext)
        .watch(Collections.emptyList(), null, Collections.singletonList(deployment), PlatformMode.kubernetes);
      // Then
      ArgumentCaptor<String> javaProcessArgumentCaptor = ArgumentCaptor.forClass(String.class);
      verify(logger).info("spring-boot: Watching pods with selector %s waiting for a running pod...", new LabelSelectorBuilder().addToMatchLabels("app", "spring-boot-test").build());
      verify(logger).info("spring-boot: Terminating the Spring remote client...");
      verify(logger).debug(javaProcessArgumentCaptor.capture());
      assertThat(javaProcessArgumentCaptor.getValue())
        .contains("spring-boot: Running: ")
        .contains(String.format("%s/bin/java -cp ", testJavaHome))
        .contains(String.format("%s/target/spring-boot-lib.jar:%s/target/spring-boot-devtools.jar ", project, project))
        .contains("-Dspring.devtools.remote.secret=this-is-a-test ")
        .contains("org.springframework.boot.devtools.RemoteSpringApplication ")
        .contains("http://localhost:");
    } finally {
      EnvUtil.overridePropertyGetter(System::getProperty);
    }
  }
}
