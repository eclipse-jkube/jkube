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
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.Plugin;
import org.eclipse.jkube.kit.common.util.KubernetesHelper;
import org.eclipse.jkube.kit.common.util.SpringBootUtil;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.service.PortForwardService;
import org.eclipse.jkube.watcher.api.WatcherContext;
import org.junit.Before;
import org.junit.Test;

import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@SuppressWarnings({"ResultOfMethodCallIgnored", "AccessStaticViaInstance", "unused"})
public class SpringBootWatcherTest {
    @Mocked
    private PortForwardService portForwardService;

    @Mocked
    private NamespacedKubernetesClient kubernetesClient;

    @Mocked
    private WatcherContext watcherContext;

    @Mocked
    private JKubeConfiguration jkubeConfiguration;

    @Mocked
    private JavaProject javaProject;

    @Mocked
    private KubernetesHelper kubernetesHelper;

    @Mocked
    private SpringBootUtil springBootUtil;

    @Before
    public void setup() {
        // @formatter:off
        new Expectations() {{
            watcherContext.getBuildContext();
            result = jkubeConfiguration;

            jkubeConfiguration.getProject();
            result = javaProject;

            javaProject.getProperties();
            result = new Properties();
        }};
        // @formatter:on
    }

    @Test
    public void testGetPortForwardUrl() {
        // Given
        Properties properties = new Properties();
        properties.put("server.port", "9001");
        // @formatter:off
        new Expectations() {{
            javaProject.getCompileClassPathElements();
            result = Collections.singletonList("/foo");

            javaProject.getOutputDirectory().getAbsolutePath();
            result = "target/classes";

            springBootUtil.getSpringBootApplicationProperties((URLClassLoader) any);
            result = properties;
        }};
        // @formatter:on
        List<HasMetadata> resources = new ArrayList<>();
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
        new Verifications() {{
            portForwardService.forwardPortAsync(kubernetesClient, (LabelSelector) any, 9001, anyInt);
        }};
    }

    @Test
    public void isApplicable_whenDetectsSpringBootMavenPlugin_thenReturnsTrue() {
        // Given
        // @formatter:off
        new Expectations() {{
            javaProject.getPlugins();
            result = Collections.singletonList(Plugin.builder().artifactId("spring-boot-maven-plugin").build());
        }};
        // @formatter:on
        SpringBootWatcher springBootWatcher = new SpringBootWatcher(watcherContext);

        // When
        boolean isApplicable = springBootWatcher.isApplicable(Collections.emptyList(), Collections.emptyList(), PlatformMode.kubernetes);

        // Then
        assertTrue(isApplicable);
    }

    @Test
    public void isApplicable_whenDetectsSpringBootGradlePlugin_thenReturnsTrue() {
        // Given
        // @formatter:off
        new Expectations() {{
            javaProject.getPlugins();
            result = Collections.singletonList(Plugin.builder().artifactId("org.springframework.boot.gradle.plugin").build());
        }};
        // @formatter:on
        SpringBootWatcher springBootWatcher = new SpringBootWatcher(watcherContext);

        // When
        boolean isApplicable = springBootWatcher.isApplicable(Collections.emptyList(), Collections.emptyList(), PlatformMode.kubernetes);

        // Then
        assertTrue(isApplicable);
    }

    @Test
    public void isApplicable_whenNoPluginProvided_thenReturnsFalse() {
        // Given
        // @formatter:off
        new Expectations() {{
            javaProject.getPlugins();
            result = Collections.emptyList();
        }};
        // @formatter:on
        SpringBootWatcher springBootWatcher = new SpringBootWatcher(watcherContext);

        // When
        boolean isApplicable = springBootWatcher.isApplicable(Collections.emptyList(), Collections.emptyList(), PlatformMode.kubernetes);

        // Then
        assertFalse(isApplicable);
    }
}
