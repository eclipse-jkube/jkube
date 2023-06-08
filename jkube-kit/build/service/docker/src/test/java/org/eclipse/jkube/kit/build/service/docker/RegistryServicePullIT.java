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
package org.eclipse.jkube.kit.build.service.docker;

import io.fabric8.mockwebserver.DefaultMockServer;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jkube.kit.build.service.docker.access.DockerAccess;
import org.eclipse.jkube.kit.build.service.docker.access.DockerAccessException;
import org.eclipse.jkube.kit.build.service.docker.access.hc.DockerAccessWithHcClient;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.RegistryConfig;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.Collections;
import java.util.Properties;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIOException;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class RegistryServicePullIT {
  private DefaultMockServer server;
  private KitLogger kitLogger;

  @BeforeEach
  void setUp() {
    kitLogger = spy(new KitLogger.SilentLogger());
    server = new DefaultMockServer();
    server.start();
  }

  @AfterEach
  void tearDown() {
    server.shutdown();
  }

  @ParameterizedTest(name = "pull {0} and no registry from any source, then pull url = {1}")
  @CsvSource({
      "word:word,/v1.18/images/create?fromImage=word&tag=word",
      "word/word:tag,/v1.18/images/create?fromImage=word%2Fword&tag=tag",
      "word.word/word/word:tag,/v1.18/images/create?fromImage=word.word%2Fword%2Fword&tag=tag",
      "word:5000/word:tag,/v1.18/images/create?fromImage=word%3A5000%2Fword&tag=tag",
      "word.word:5000/word:tag,/v1.18/images/create?fromImage=word.word%3A5000%2Fword&tag=tag",
      "word.word/word:tag,/v1.18/images/create?fromImage=word.word%2Fword&tag=tag",
      "word.word/word.word/word:tag,/v1.18/images/create?fromImage=word.word%2Fword.word%2Fword&tag=tag",
      "word.word.word/word:tag,/v1.18/images/create?fromImage=word.word.word%2Fword&tag=tag",
      "word.word.word/word/word:tag,/v1.18/images/create?fromImage=word.word.word%2Fword%2Fword&tag=tag"
  })
  void pullImageWithPolicy_whenNoRegistryFromAnySource_thenPullImage(String imageName, String pullImageEndpoint) throws IOException {
    // Given
    server.expect().post()
        .withPath(pullImageEndpoint)
        .andReturn(200, Collections.singletonMap("message", "OK"))
        .once();
    RegistryService registryService = createNewRegistryService(server.getPort());
    ImagePullManager imagePullManager = ImagePullManager.createImagePullManager("Always", "true", new Properties());
    BuildConfiguration buildConfiguration = BuildConfiguration.builder().build();
    RegistryConfig registryConfig = createNewRegistryConfig(null);

    // When
    registryService.pullImageWithPolicy(imageName, imagePullManager, registryConfig, buildConfiguration);

    // Then
    verify(kitLogger).info(eq("Pulled %s in %s"), eq(imageName), anyString());
  }

  @ParameterizedTest(name = "pull {0}, when registry present both in image name and registry config, then registry taken from image name {1}")
  @CsvSource({
      "word.word/word/word:tag,/v1.18/images/create?fromImage=word.word%2Fword%2Fword&tag=tag",
      "word:5000/word:tag,/v1.18/images/create?fromImage=word%3A5000%2Fword&tag=tag",
      "word.word:5000/word:tag,/v1.18/images/create?fromImage=word.word%3A5000%2Fword&tag=tag",
      "word.word/word.word/word:tag,/v1.18/images/create?fromImage=word.word%2Fword.word%2Fword&tag=tag",
      "word.word.word/word/word:tag,/v1.18/images/create?fromImage=word.word.word%2Fword%2Fword&tag=tag"
  })
  void pullImageWithPolicy_whenRegistryInBothImageNameAndRegistryConfig_thenUseRegistryInImageName(String imageName, String pullImageEndpoint) throws IOException {
    // Given
    server.expect().post()
        .withPath(pullImageEndpoint)
        .andReturn(200, Collections.singletonMap("message", "OK"))
        .once();
    RegistryService registryService = createNewRegistryService(server.getPort());
    ImagePullManager imagePullManager = ImagePullManager.createImagePullManager("Always", "true", new Properties());
    BuildConfiguration buildConfiguration = BuildConfiguration.builder().build();
    RegistryConfig registryConfig = createNewRegistryConfig("quay.io");

    // When
    registryService.pullImageWithPolicy(imageName, imagePullManager, registryConfig, buildConfiguration);

    // Then
    verify(kitLogger).info(eq("Pulled %s in %s"), eq(imageName), anyString());
  }

  private static Stream<Arguments> pullImageImageNameRegistryOverriddenWithRegistryFromRegistryConfigParams() {
    return Stream.of(
        Arguments.of("word:word",
            "/v1.18/images/create?fromImage=quay.io%2Fword&tag=word",
            "/v1.18/images/quay.io%2Fword%3Aword/tag?force=0&repo=word&tag=word"),
        Arguments.of("word/word:tag",
            "/v1.18/images/create?fromImage=quay.io%2Fword%2Fword&tag=tag",
            "/v1.18/images/quay.io%2Fword%2Fword%3Atag/tag?force=0&repo=word%2Fword&tag=tag"),
        Arguments.of("word.word/word:tag",
            "/v1.18/images/create?fromImage=quay.io%2Fword.word%2Fword&tag=tag",
            "/v1.18/images/quay.io%2Fword.word%2Fword%3Atag/tag?force=0&repo=word.word%2Fword&tag=tag")
    );
  }

  @ParameterizedTest(name = "pull {0} and registry from registry config, then registry prepended to image name {1}")
  @MethodSource("pullImageImageNameRegistryOverriddenWithRegistryFromRegistryConfigParams")
  void pullImageWithPolicy_whenRegistryFromRegistryConfig_thenRegistryPrependedToImageName(String imageName, String pullImageEndpoint, String createTagEndpoint) throws IOException {
    // Given
    server.expect().post()
        .withPath(pullImageEndpoint)
        .andReturn(200, Collections.singletonMap("message", "OK"))
        .once();
    server.expect().post()
        .withPath(createTagEndpoint)
        .andReturn(201, Collections.singletonMap("message", "OK"))
        .once();
    RegistryService registryService = createNewRegistryService(server.getPort());
    ImagePullManager imagePullManager = ImagePullManager.createImagePullManager("Always", "true", new Properties());
    BuildConfiguration buildConfiguration = BuildConfiguration.builder().build();
    RegistryConfig registryConfig = createNewRegistryConfig("quay.io");

    // When
    registryService.pullImageWithPolicy(imageName, imagePullManager, registryConfig, buildConfiguration);

    // Then
    verify(kitLogger).info(eq("Pulled %s in %s"), eq(imageName), anyString());
  }

  @Test
  @DisplayName("pull, when ImagePullPolicy=IfNotPresent and image already present, then no image pull")
  void pullImageWithPolicy_whenIfNotPresentPullPolicyAndImageAlreadyPresent_thenNoImagePull() throws IOException {
    // Given
    server.expect().get()
        .withPath("/v1.18/images/example.org%2Ffoo%2Fbar%3Alatest/json")
        .andReturn(200, Collections.singletonMap("Id", "sha256:testname"))
        .once();
    RegistryService registryService = createNewRegistryService(server.getPort());
    ImagePullManager imagePullManager = ImagePullManager.createImagePullManager("IfNotPresent", "true", new Properties());
    RegistryConfig registryConfig = createNewRegistryConfig(null);
    BuildConfiguration buildConfiguration = BuildConfiguration.builder().build();

    // When
    registryService.pullImageWithPolicy("example.org/foo/bar:latest", imagePullManager, registryConfig, buildConfiguration);

    // Then
    verify(kitLogger, times(0)).info(anyString(), anyString(), anyString());
  }

  @Test
  @DisplayName("pull, when ImagePullPolicy=Never and image absent, then throw exception")
  void pullImageWithPolicy_whenNeverPullPolicyAndImageAbsent_thenThrowException() throws IOException {
    // Given
    RegistryService registryService = createNewRegistryService(server.getPort());
    ImagePullManager imagePullManager = ImagePullManager.createImagePullManager("Never", "true", new Properties());
    RegistryConfig registryConfig = createNewRegistryConfig(null);
    BuildConfiguration buildConfiguration = BuildConfiguration.builder().build();

    // When
    assertThatIOException()
        .isThrownBy(() -> registryService.pullImageWithPolicy("example.org/foo/bar:latest", imagePullManager, registryConfig, buildConfiguration))
        .withMessage("No image 'example.org/foo/bar:latest' found and pull policy 'Never' is set. Please chose another pull policy or pull the image yourself)");
  }

  @Test
  @DisplayName("pull, when ImagePullPolicy=Never and image already present, then no image pull")
  void pullImageWithPolicy_whenNeverPullPolicyAndImageAlreadyPresent_thenNoImagePull() throws IOException {
    // Given
    server.expect().get()
        .withPath("/v1.18/images/example.org%2Ffoo%2Fbar%3Alatest/json")
        .andReturn(200, Collections.singletonMap("Id", "sha256:testname"))
        .once();
    RegistryService registryService = createNewRegistryService(server.getPort());
    ImagePullManager imagePullManager = ImagePullManager.createImagePullManager("Never", "true", new Properties());
    RegistryConfig registryConfig = createNewRegistryConfig(null);
    BuildConfiguration buildConfiguration = BuildConfiguration.builder().build();

    // When
    registryService.pullImageWithPolicy("example.org/foo/bar:latest", imagePullManager, registryConfig, buildConfiguration);

    // Then
    verify(kitLogger, times(0)).info(anyString(), anyString(), anyString());
  }

  @Test
  @DisplayName("pull, when pull rest call failed, then throw exception")
  void pullImageWithPolicy_whenPullFailure_thenThrowException() throws IOException {
    // Given
    server.expect().post()
        .withPath("/v1.18/images/create?fromImage=example.org%2Ffoo%2Fbar&tag=latest")
        .andReturn(500, Collections.singletonMap("message", "ERROR"))
        .once();
    RegistryService registryService = createNewRegistryService(server.getPort());
    ImagePullManager imagePullManager = ImagePullManager.createImagePullManager("Always", "true", new Properties());
    RegistryConfig registryConfig = createNewRegistryConfig(null);
    BuildConfiguration buildConfiguration = BuildConfiguration.builder().build();

    // When
    assertThatExceptionOfType(DockerAccessException.class)
        .isThrownBy(() -> registryService.pullImageWithPolicy("example.org/foo/bar:latest", imagePullManager, registryConfig, buildConfiguration))
        .withMessage("Unable to pull 'example.org/foo/bar:latest' from registry 'example.org' : {\"message\":\"ERROR\"} (Server Error: 500)");
  }

  private RegistryConfig createNewRegistryConfig(String registry) {
    RegistryConfig.RegistryConfigBuilder registryConfigBuilder = RegistryConfig.builder();
    if (StringUtils.isNotBlank(registry)) {
      registryConfigBuilder.registry(registry);
    }
    registryConfigBuilder.settings(Collections.emptyList());
    return registryConfigBuilder.build();
  }

  private RegistryService createNewRegistryService(int port) throws IOException {
    DockerAccess docker = new DockerAccessWithHcClient(String.format("http://127.0.0.1:%d", port), null, 1, kitLogger);
    QueryService queryService = new QueryService(docker);
    return new RegistryService(docker, queryService, kitLogger);
  }
}
