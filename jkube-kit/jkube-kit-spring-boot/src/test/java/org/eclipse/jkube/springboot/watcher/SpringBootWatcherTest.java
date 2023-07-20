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

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.Plugin;
import org.eclipse.jkube.kit.common.util.KubernetesHelper;
import org.eclipse.jkube.kit.common.util.SpringBootUtil;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.service.PortForwardService;
import org.eclipse.jkube.watcher.api.WatcherContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SpringBootWatcherTest {

    private NamespacedKubernetesClient kubernetesClient;

    private WatcherContext watcherContext;

    @BeforeEach
    void setup(@TempDir Path project) throws IOException {
        watcherContext = WatcherContext.builder()
          .logger(new KitLogger.SilentLogger())
          .buildContext(JKubeConfiguration.builder()
            .project(JavaProject.builder()
              .outputDirectory(Files.createDirectory(project.resolve("target")).toFile())
              .build())
            .build())
          .build();
        kubernetesClient = mock(NamespacedKubernetesClient.class);
    }

    @Test
    void getPortForwardUrl() {
        try (MockedStatic<SpringBootUtil> springBootUtilMockedStatic = Mockito.mockStatic(SpringBootUtil.class);
             MockedStatic<KubernetesHelper> kubernetesHelperMockedStatic = Mockito.mockStatic(KubernetesHelper.class);
             MockedConstruction<PortForwardService> portForwardServiceMockedConstruction = Mockito.mockConstruction(PortForwardService.class)) {
            // Given
            Properties properties = new Properties();
            properties.put("server.port", "9001");
            springBootUtilMockedStatic.when(() -> SpringBootUtil.getSpringBootApplicationProperties(any())).thenReturn(properties);
            List<HasMetadata> resources = new ArrayList<>();
            resources.add(new DeploymentBuilder().withNewMetadata().withName("d1").endMetadata()
                    .withNewSpec()
                    .withNewSelector()
                    .withMatchLabels(Collections.singletonMap("foo", "bar"))
                    .endSelector()
                    .endSpec()
                    .build());
            resources.add(new ServiceBuilder().withNewMetadata().withName("s1").endMetadata().build());
            kubernetesHelperMockedStatic.when(() -> KubernetesHelper.extractPodLabelSelector(resources))
                .thenReturn(new LabelSelectorBuilder().build());
            SpringBootWatcher springBootWatcher = new SpringBootWatcher(watcherContext);

            // When
            String portForwardUrl = springBootWatcher.getPortForwardUrl(kubernetesClient, resources);
            // Then
            assertThat(portForwardUrl).contains("http://localhost:");
            assertThat(portForwardServiceMockedConstruction.constructed()).hasSize(1);
            verify(portForwardServiceMockedConstruction.constructed().get(0))
                .forwardPortAsync(eq(kubernetesClient), any(), eq(9001), anyInt());
        }
    }

    @DisplayName("isApplicable")
    @ParameterizedTest(name = "with ''{0}'' should return ''{2}''")
    @MethodSource("plugins")
    void isApplicable(String pluginType, List<Plugin> plugins, boolean expected) {
        // Given
        watcherContext = watcherContext.toBuilder()
                .buildContext(watcherContext.getBuildContext().toBuilder()
                .project(JavaProject.builder().plugins(plugins).build())
                .build())
          .build();
        SpringBootWatcher springBootWatcher = new SpringBootWatcher(watcherContext);

        // When
        boolean isApplicable = springBootWatcher.isApplicable(Collections.emptyList(), Collections.emptyList(), PlatformMode.kubernetes);

        // Then
        assertThat(isApplicable).isEqualTo(expected);
    }

    static Stream<Arguments> plugins() {
      return Stream.of(
          arguments("spring boot maven plugin", Collections.singletonList(Plugin.builder().artifactId("spring-boot-maven-plugin").build()), true),
          arguments("spring boot gradle plugin", Collections.singletonList(Plugin.builder().artifactId("org.springframework.boot.gradle.plugin").build()), true),
          arguments("no plugin", Collections.emptyList(), false));
    }
}
