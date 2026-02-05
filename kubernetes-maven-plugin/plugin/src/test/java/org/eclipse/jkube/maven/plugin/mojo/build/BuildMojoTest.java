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

import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.resource.ContainerResourcesConfig;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.config.service.BuildServiceConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BuildMojoTest {
    private MavenProject mavenProject;
    @BeforeEach
    void setUp() {
        mavenProject = mock(MavenProject.class,RETURNS_DEEP_STUBS);
        when(mavenProject.getBuild().getDirectory()).thenReturn("target");
    }

    @Test
    void buildServiceConfigBuilder_shouldReturnNonNullResourceConfigIfConfigured() {
        // Given
        BuildMojo buildMojo = new BuildMojo();
        buildMojo.project = mavenProject;
        buildMojo.resources = ResourceConfig.builder()
                .openshiftBuildConfig(ContainerResourcesConfig.builder()
                        .limit("cpu", "200m")
                        .request("memory", "1Gi")
                        .build())
                .build();
        buildMojo.resourceDir = new File("src/main/jkube");

        // When
        BuildServiceConfig.BuildServiceConfigBuilder buildServiceConfigBuilder = buildMojo.buildServiceConfigBuilder();
        // Then
        assertThat(buildServiceConfigBuilder.build()).isNotNull()
            .returns(new File("src/main/jkube"), BuildServiceConfig::getResourceDir)
            .extracting(BuildServiceConfig::getResourceConfig)
            .extracting(ResourceConfig::getOpenshiftBuildConfig)
            .returns("200m", c -> c.getLimits().get("cpu"))
            .returns("1Gi", c -> c.getRequests().get("memory"));
    }

    @Test
    void execute_whenSkipBuildTrue_shouldDoNothing() throws MojoExecutionException, MojoFailureException {
        // Given
        KitLogger kitLogger = spy(new KitLogger.SilentLogger());
        Settings mockSettings = mock(Settings.class);
        MojoExecution mockExecution = new MojoExecution(new MojoDescriptor());
        mockExecution.getMojoDescriptor().setPluginDescriptor(new PluginDescriptor());
        mockExecution.getMojoDescriptor().setGoal("build");
        mockExecution.getMojoDescriptor().getPluginDescriptor().setGoalPrefix("k8s");

        BuildMojo buildMojo = new BuildMojo() {
            @Override
            protected void init() {
                log = kitLogger;
            }
        };
        buildMojo.project = mavenProject;
        buildMojo.settings = mockSettings;
        buildMojo.skipBuild = true;
        buildMojo.mojoExecution = mockExecution;

        // When
        buildMojo.execute();
        // Then
        verify(kitLogger).info("`%s` goal is skipped.", "k8s:build");
    }
}
