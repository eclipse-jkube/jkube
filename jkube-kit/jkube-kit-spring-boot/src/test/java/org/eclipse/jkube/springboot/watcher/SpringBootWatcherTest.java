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

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.Plugin;
import org.eclipse.jkube.kit.common.util.SpringBootUtil;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.service.PortForwardService;
import org.eclipse.jkube.watcher.api.WatcherContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SpringBootWatcherTest {
    private PortForwardService portForwardService;

    private NamespacedKubernetesClient kubernetesClient;

    private WatcherContext watcherContext;

    private JKubeConfiguration jkubeConfiguration;

    private JavaProject javaProject;




    @Before
    public void setup() {
        javaProject = mock(JavaProject.class,RETURNS_DEEP_STUBS);
        jkubeConfiguration = mock(JKubeConfiguration.class);
        watcherContext = mock(WatcherContext.class);
        kubernetesClient = mock(NamespacedKubernetesClient.class);
        portForwardService = mock(PortForwardService.class);
        when(watcherContext.getBuildContext()).thenReturn(jkubeConfiguration);
        when(watcherContext.getLogger()).thenReturn(new KitLogger.SilentLogger());
        when(jkubeConfiguration.getProject()).thenReturn(javaProject);
        when(javaProject.getProperties()).thenReturn(new Properties());
    }

    @Test
    public void testGetPortForwardUrl() {
        try (MockedStatic<SpringBootUtil> mockStatic = Mockito.mockStatic(SpringBootUtil.class)) {
            // Given
            Properties properties = new Properties();
            properties.put("server.port", "9001");
            when(javaProject.getCompileClassPathElements()).thenReturn(Collections.singletonList("/foo"));
            when(javaProject.getOutputDirectory().getAbsolutePath()).thenReturn("target/classes");
            mockStatic.when(() -> SpringBootUtil.getSpringBootApplicationProperties(any())).thenReturn(properties);            List<HasMetadata> resources = new ArrayList<>();
            resources.add(new DeploymentBuilder().withNewMetadata().withName("d1").endMetadata()
                    .withNewSpec()
                    .withNewSelector()
                    .withMatchLabels(Collections.singletonMap("foo", "bar"))
                    .endSelector()
                    .endSpec()
                    .build());
            resources.add(new ServiceBuilder().withNewMetadata().withName("s1").endMetadata().build());
            SpringBootWatcher springBootWatcher = new SpringBootWatcher(watcherContext);

            // When
            String portForwardUrl = springBootWatcher.getPortForwardUrl(kubernetesClient, resources);
            // Then
            assertTrue(portForwardUrl.contains("http://localhost:"));
            verify(portForwardService).forwardPortAsync(kubernetesClient, any(), 9001, anyInt());
        }
    }

    @Test
    public void isApplicable_whenDetectsSpringBootMavenPlugin_thenReturnsTrue() {
        // Given
        when(javaProject.getPlugins()).thenReturn(Collections.singletonList(Plugin.builder().artifactId("spring-boot-maven-plugin").build()));
        SpringBootWatcher springBootWatcher = new SpringBootWatcher(watcherContext);

        // When
        boolean isApplicable = springBootWatcher.isApplicable(Collections.emptyList(), Collections.emptyList(), PlatformMode.kubernetes);

        // Then
        assertTrue(isApplicable);
    }

    @Test
    public void isApplicable_whenDetectsSpringBootGradlePlugin_thenReturnsTrue() {
        // Given
        when(javaProject.getPlugins()).thenReturn(Collections.singletonList(Plugin.builder().artifactId("org.springframework.boot.gradle.plugin").build()));
        SpringBootWatcher springBootWatcher = new SpringBootWatcher(watcherContext);

        // When
        boolean isApplicable = springBootWatcher.isApplicable(Collections.emptyList(), Collections.emptyList(), PlatformMode.kubernetes);

        // Then
        assertTrue(isApplicable);
    }

    @Test
    public void isApplicable_whenNoPluginProvided_thenReturnsFalse() {
        // Given
        when(javaProject.getPlugins()).thenReturn(Collections.emptyList());
        SpringBootWatcher springBootWatcher = new SpringBootWatcher(watcherContext);

        // When
        boolean isApplicable = springBootWatcher.isApplicable(Collections.emptyList(), Collections.emptyList(), PlatformMode.kubernetes);

        // Then
        assertFalse(isApplicable);
    }
}
