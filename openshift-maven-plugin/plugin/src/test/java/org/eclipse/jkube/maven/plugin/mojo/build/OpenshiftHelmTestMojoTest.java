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
package org.eclipse.jkube.maven.plugin.mojo.build;

import com.marcnuri.helm.Helm;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretListBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.eclipse.jkube.kit.common.access.ClusterConfiguration;
import org.eclipse.jkube.kit.common.util.AsyncUtil;
import org.eclipse.jkube.kit.resource.helm.HelmConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.eclipse.jkube.kit.common.util.KubernetesMockServerUtil.prepareMockWebServerExpectationsForAggregatedDiscoveryEndpoints;

@EnableKubernetesMockClient(crud = true)
class OpenshiftHelmTestMojoTest {
  @TempDir
  private Path projectDir;
  private PrintStream originalPrintStream;
  private ByteArrayOutputStream outputStream;
  private OpenshiftHelmTestMojo openShiftHelmTestMojo;
  private KubernetesClient kubernetesClient;
  private KubernetesMockServer server;

  @BeforeEach
  void setUp() throws Exception {
    originalPrintStream = System.out;
    outputStream = new ByteArrayOutputStream();
    System.setOut(new PrintStream(outputStream));
    Helm.create().withDir(projectDir).withName("empty-project").call();
    Path helmChartOutputDir = projectDir.resolve("target").resolve("jkube").resolve("helm");
    Files.createDirectories(helmChartOutputDir.resolve("openshift"));
    FileUtils.copyDirectory(projectDir.resolve("empty-project").toFile(), helmChartOutputDir.resolve("openshift").toFile());
    System.setProperty("jkube.kubernetesTemplate", projectDir.toFile().getAbsolutePath());
    openShiftHelmTestMojo = new OpenshiftHelmTestMojo();
    openShiftHelmTestMojo.helm = HelmConfig.builder()
      .outputDir(helmChartOutputDir.toString())
      .disableOpenAPIValidation(true)
      .build();
    openShiftHelmTestMojo.interpolateTemplateParameters = true;
    openShiftHelmTestMojo.access = ClusterConfiguration.from(kubernetesClient.getConfiguration()).build();
    openShiftHelmTestMojo.settings = new Settings();
    openShiftHelmTestMojo.project = new MavenProject();
    openShiftHelmTestMojo.project.setVersion("0.1.0");
    openShiftHelmTestMojo.project.getBuild()
      .setOutputDirectory(projectDir.resolve("target").resolve("classes").toFile().getAbsolutePath());
    openShiftHelmTestMojo.project.getBuild().setDirectory(projectDir.resolve("target").toFile().getAbsolutePath());
    openShiftHelmTestMojo.project.setFile(projectDir.resolve("target").toFile());
    // Remove after https://github.com/fabric8io/kubernetes-client/issues/6062 is fixed
    prepareMockWebServerExpectationsForAggregatedDiscoveryEndpoints(server);
  }

  @AfterEach
  void tearDown() {
    System.setOut(originalPrintStream);
    System.clearProperty("jkube.kubernetesTemplate");
    openShiftHelmTestMojo = null;
  }

  @Test
  @DisplayName("Helm release installed on OpenShift cluster, then test helm release")
  void execute_whenReleasePresent_shouldTestChartFromOpenShiftCluster() throws MojoFailureException, IOException {
    // Given
    // OpenAPI validation endpoints required by helm test
    server.expect().get().withPath("/openapi/v3?timeout=32s")
      .andReturn(200, IOUtils.toString(Objects.requireNonNull(OpenshiftHelmTestMojoTest.class.getResourceAsStream("/helm-test-mojo/kubernetes-openapi-v3-schema.json")), StandardCharsets.UTF_8))
      .always();
    server.expect().get().withPath("/openapi/v3/api/v1?timeout=32s")
      .andReturn(200, IOUtils.toString(Objects.requireNonNull(OpenshiftHelmTestMojoTest.class.getResourceAsStream("/helm-test-mojo/kubernetes-openapi-v3-api-v1-schema-pod.json")), StandardCharsets.UTF_8))
      .always();
    openShiftHelmTestMojo.init();
    openShiftHelmTestMojo.jkubeServiceHub.getHelmService().install(openShiftHelmTestMojo.helm);
    // Should be removed once https://github.com/fabric8io/kubernetes-client/issues/6220 gets fixed
    Secret secret = kubernetesClient.secrets().withName("sh.helm.release.v1.empty-project.v1").get();
    server.expect().get().withPath("/api/v1/namespaces/test/secrets?labelSelector=name%3Dempty-project%2Cowner%3Dhelm")
      .andReturn(200, new SecretListBuilder()
        .addToItems(secret)
        .build())
      .once();
    // When
    CompletableFuture<Boolean> openShiftHelmTest = AsyncUtil.async(() -> {
      openShiftHelmTestMojo.execute();
      return true;
    });

    kubernetesClient.pods().withName("empty-project-test-connection")
      .waitUntilCondition(Objects::nonNull, 5, TimeUnit.SECONDS);
    kubernetesClient.pods().withName("empty-project-test-connection").editStatus(p -> new PodBuilder(p)
      .editOrNewStatus()
      .withPhase("Succeeded")
      .endStatus()
      .build());
    // Then
    assertThat(openShiftHelmTest).succeedsWithin(5, TimeUnit.SECONDS);
    assertThat(outputStream.toString())
      .contains("[INFO] Testing Helm Chart empty-project 0.1.0")
      .contains("[INFO] [[W]]NAME: empty-project")
      .contains("[INFO] [[W]]STATUS: deployed")
      .contains("[INFO] [[W]]REVISION: 1")
      .contains("[INFO] [[W]]Phase: Succeeded");
  }

  @Test
  @DisplayName("Helm Release not installed on OpenShift cluster, then throw exception")
  void execute_whenReleaseNotPresent_thenThrowException() {
    assertThatIllegalStateException()
      .isThrownBy(() -> openShiftHelmTestMojo.execute())
      .withMessageContaining(" not found");
  }
}
