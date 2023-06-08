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
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class RegistryServicePushIT {
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

  @ParameterizedTest(name = "push {0} when no registry from any source, then push to specified endpoint {1}")
  @CsvSource({
      "word:word,/v1.18/images/word/push?force=1&tag=word",
      "word/word:tag,/v1.18/images/word%2Fword/push?force=1&tag=tag",
      "word.word/word:tag,/v1.18/images/word.word%2Fword/push?force=1&tag=tag",
      "word.word/word/word:tag,/v1.18/images/word.word%2Fword%2Fword/push?force=1&tag=tag",
      "word.word/word.word/word:tag,/v1.18/images/word.word%2Fword.word%2Fword/push?force=1&tag=tag",
      "word:5000/word:tag,/v1.18/images/word%3A5000%2Fword/push?force=1&tag=tag",
      "word.word:5000/word:tag,/v1.18/images/word.word%3A5000%2Fword/push?force=1&tag=tag",
      "word.word.word/word:tag,/v1.18/images/word.word.word%2Fword/push?force=1&tag=tag",
      "word.word.word/word/word:tag,/v1.18/images/word.word.word%2Fword%2Fword/push?force=1&tag=tag"
  })
  void pushImage_whenNoRegistryProvidedFromAnySource_thenRequestImagePushToDockerAPI(String imageName, String pushEndpoint) throws IOException {
    // Given
    server.expect().post()
        .withPath(pushEndpoint)
        .andReturn(200, Collections.singletonMap("message", "OK"))
        .once();
    RegistryService registryService = createNewRegistryService(server.getPort());
    RegistryConfig registryConfig = createNewRegistryConfig(null);
    ImageConfiguration imageConfiguration = createNewImageConfiguration(imageName, null, Collections.emptyList());

    // When
    registryService.pushImage(imageConfiguration, 0, registryConfig, true);

    // Then
    verify(kitLogger).info(eq("Pushed %s in %s"), eq(imageName), anyString());
  }

  @ParameterizedTest(name = "push {0} when registry present both in image name and registry config, then registry taken from image name {1}")
  @MethodSource("pushImageNameRegistryNotPrependedParams")
  void pushImage_whenRegistryInBothImageNameAndRegistryConfig_thenUseRegistryInImageName(String imageName, String pushEndpoint) throws IOException {
    // Given
    server.expect().post()
        .withPath(pushEndpoint)
        .andReturn(200, Collections.singletonMap("message", "OK"))
        .once();
    RegistryService registryService = createNewRegistryService(server.getPort());
    RegistryConfig registryConfig = createNewRegistryConfig("quay.io");
    ImageConfiguration imageConfiguration = createNewImageConfiguration(imageName, null, Collections.emptyList());

    // When
    registryService.pushImage(imageConfiguration, 0, registryConfig, true);

    // Then
    verify(kitLogger).info(eq("Pushed %s in %s"), eq(imageName), anyString());
  }

  private static Stream<Arguments> pushImageNameRegistryNotPrependedParams() {
    return Stream.of(
        Arguments.of("word.word/word/word:tag", "/v1.18/images/word.word%2Fword%2Fword/push?force=1&tag=tag"),
        Arguments.of("word.word/word.word/word:tag", "/v1.18/images/word.word%2Fword.word%2Fword/push?force=1&tag=tag"),
        Arguments.of("word:5000/word:tag", "/v1.18/images/word%3A5000%2Fword/push?force=1&tag=tag"),
        Arguments.of("word.word:5000/word:tag", "/v1.18/images/word.word%3A5000%2Fword/push?force=1&tag=tag"),
        Arguments.of("word.word.word/word/word:tag", "/v1.18/images/word.word.word%2Fword%2Fword/push?force=1&tag=tag")
    );
  }

  @ParameterizedTest(name = "push {0} when registry present both in image name and image config, then registry taken from image name {1}")
  @MethodSource("pushImageNameRegistryNotPrependedParams")
  void pushImage_whenRegistryInBothImageNameAndImageConfig_thenUseRegistryFromImageName(String imageName, String pushEndpoint) throws IOException {
    // Given
    server.expect().post()
        .withPath(pushEndpoint)
        .andReturn(200, Collections.singletonMap("message", "OK"))
        .once();
    RegistryService registryService = createNewRegistryService(server.getPort());
    RegistryConfig registryConfig = createNewRegistryConfig(null);
    ImageConfiguration imageConfiguration = createNewImageConfiguration(imageName, "quay.io", Collections.emptyList());

    // When
    registryService.pushImage(imageConfiguration, 0, registryConfig, true);

    // Then
    verify(kitLogger).info(eq("Pushed %s in %s"), eq(imageName), anyString());
  }

  @ParameterizedTest(name = "push {0} when registry from image config, then registry prepended to image name {1}")
  @MethodSource("pushImageNameRegistryPrependedParams")
  void pushImage_whenRegistryFromImageConfig_thenRegistryPrependedToImageName(String imageName, String temporaryTagEndpoint, String pushEndpoint, String temporaryTagDeleteEndpoint) throws IOException {
    // Given
    server.expect().post()
        .withPath(temporaryTagEndpoint)
        .andReturn(201, Collections.singletonMap("message", "OK"))
        .once();
    server.expect().post()
        .withPath(pushEndpoint)
        .andReturn(200, Collections.singletonMap("message", "OK"))
        .once();
    server.expect().delete()
        .withPath(temporaryTagDeleteEndpoint)
        .andReturn(200, Collections.singletonMap("message", "OK"))
        .once();
    RegistryService registryService = createNewRegistryService(server.getPort());
    RegistryConfig registryConfig = createNewRegistryConfig(null);
    ImageConfiguration imageConfiguration = createNewImageConfiguration(imageName, "quay.io", Collections.emptyList());

    // When
    registryService.pushImage(imageConfiguration, 0, registryConfig, true);

    // Then
    verify(kitLogger).info(eq("Pushed %s in %s"), eq(imageName), anyString());
  }

  private static Stream<Arguments> pushImageNameRegistryPrependedParams() {
    return Stream.of(
        Arguments.of("word:word",
            "/v1.18/images/word%3Aword/tag?force=0&repo=quay.io%2Fword&tag=word",
            "/v1.18/images/quay.io%2Fword/push?force=1&tag=word",
            "/v1.18/images/quay.io%2Fword%3Aword?force=1"),
        Arguments.of("word/word:tag",
            "/v1.18/images/word%2Fword%3Atag/tag?force=0&repo=quay.io%2Fword%2Fword&tag=tag",
            "/v1.18/images/quay.io%2Fword%2Fword/push?force=1&tag=tag",
            "/v1.18/images/quay.io%2Fword%2Fword%3Atag?force=1"),
        Arguments.of("word.word/word:tag",
            "/v1.18/images/word.word%2Fword%3Atag/tag?force=0&repo=quay.io%2Fword.word%2Fword&tag=tag",
            "/v1.18/images/quay.io%2Fword.word%2Fword/push?force=1&tag=tag",
            "/v1.18/images/quay.io%2Fword.word%2Fword%3Atag?force=1")
    );
  }

  @ParameterizedTest(name = "push {0} when registry from registry config, registry prepended to image name {1}")
  @MethodSource("pushImageNameRegistryPrependedParams")
  void pushImage_whenRegistryFromRegistryConfig_thenRegistryPrependedToImageName(String imageName, String temporaryTagEndpoint, String pushEndpoint, String temporaryTagDeleteEndpoint) throws IOException {
    // Given
    server.expect().post()
        .withPath(temporaryTagEndpoint)
        .andReturn(201, Collections.singletonMap("message", "OK"))
        .once();
    server.expect().post()
        .withPath(pushEndpoint)
        .andReturn(200, Collections.singletonMap("message", "OK"))
        .once();
    server.expect().delete()
        .withPath(temporaryTagDeleteEndpoint)
        .andReturn(200, Collections.singletonMap("message", "OK"))
        .once();
    RegistryService registryService = createNewRegistryService(server.getPort());
    RegistryConfig registryConfig = createNewRegistryConfig("quay.io");
    ImageConfiguration imageConfiguration = createNewImageConfiguration(imageName, null, Collections.emptyList());

    // When
    registryService.pushImage(imageConfiguration, 0, registryConfig, true);

    // Then
    verify(kitLogger).info(eq("Pushed %s in %s"), eq(imageName), anyString());
  }

  @Test
  @DisplayName("push, when push rest call failure, then throw exception")
  void pushImage_whenPushFailed_thenThrowException() throws IOException {
    // Given
    server.expect().post()
        .withPath("/v1.18/images/example.org%2Ffoo%2Fbar/push?force=1&tag=latest")
        .andReturn(500, Collections.singletonMap("message", "ERROR"))
        .once();
    RegistryService registryService = createNewRegistryService(server.getPort());
    RegistryConfig registryConfig = createNewRegistryConfig(null);
    ImageConfiguration imageConfiguration = createNewImageConfiguration("example.org/foo/bar:latest", null, Arrays.asList("t1", "t2"));

    // When
    assertThatExceptionOfType(DockerAccessException.class)
        .isThrownBy(() -> registryService.pushImage(imageConfiguration, 0, registryConfig, false))
        .withMessage("Unable to push 'example.org/foo/bar:latest' to registry 'example.org' : {\"message\":\"ERROR\"} (Server Error: 500)");
  }

  @Test
  @DisplayName("push, when additional tags in build configuration, then push tag specified in image name and build configuration")
  void pushImage_whenTagsInBuildConfigAndSkipTagsFalse_thenAlsoPushTagsSpecifiedInImageConfiguration() throws IOException {
    // Given
    server.expect().post()
        .withPath("/v1.18/images/example.org%2Ffoo%2Fbar/push?force=1&tag=latest")
        .andReturn(200, Collections.singletonMap("message", "OK"))
        .once();
    server.expect().post()
        .withPath("/v1.18/images/example.org%2Ffoo%2Fbar/push?force=1&tag=t1")
        .andReturn(200, Collections.singletonMap("message", "OK"))
        .once();
    server.expect().post()
        .withPath("/v1.18/images/example.org%2Ffoo%2Fbar/push?force=1&tag=t2")
        .andReturn(200, Collections.singletonMap("message", "OK"))
        .once();
    RegistryService registryService = createNewRegistryService(server.getPort());
    RegistryConfig registryConfig = createNewRegistryConfig(null);
    ImageConfiguration imageConfiguration = createNewImageConfiguration("example.org/foo/bar:latest", null, Arrays.asList("t1", "t2"));

    // When
    registryService.pushImage(imageConfiguration, 0, registryConfig, false);

    // Then
    verify(kitLogger).info(eq("Pushed %s in %s"), eq("example.org/foo/bar:latest"), anyString());
  }

  private ImageConfiguration createNewImageConfiguration(String name, String registry, List<String> tags) {
    ImageConfiguration.ImageConfigurationBuilder imageConfigurationBuilder = ImageConfiguration.builder();
    imageConfigurationBuilder.name(name);
    if (StringUtils.isNotBlank(registry)) {
      imageConfigurationBuilder.registry(registry);
    }
    imageConfigurationBuilder.build(BuildConfiguration.builder().tags(tags).build());
    return imageConfigurationBuilder.build();
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
