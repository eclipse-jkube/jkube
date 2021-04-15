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
package org.eclipse.jkube.maven.plugin.mojo.build;

import mockit.Expectations;
import mockit.Mocked;
import org.apache.maven.project.MavenProject;
import org.eclipse.jkube.kit.config.resource.OpenshiftBuildConfig;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.config.service.BuildServiceConfig;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

public class BuildMojoTest {
    @Mocked
    private MavenProject mavenProject;

    @Before
    public void setUp() {
        // @formatter:off
        new Expectations(){{
            mavenProject.getBuild().getDirectory(); result = "target";
        }};
        // @formatter:on
    }

    @Test
    public void testBuildServiceConfigBuilderReturnsNonNullResourceConfigIfConfigured() {
        // Given
        BuildMojo buildMojo = new BuildMojo();
        buildMojo.project = mavenProject;
        buildMojo.resources = ResourceConfig.builder()
                .openshiftBuildConfig(OpenshiftBuildConfig.builder()
                        .limit("cpu", "200m")
                        .request("memory", "1Gi")
                        .build())
                .build();
        buildMojo.resourceDir = new File("src/main/jkube");

        // When
        BuildServiceConfig.BuildServiceConfigBuilder buildServiceConfigBuilder = buildMojo.buildServiceConfigBuilder();

        // Then
        assertThat(buildServiceConfigBuilder).isNotNull();
        BuildServiceConfig buildServiceConfig = buildServiceConfigBuilder.build();
        assertThat(buildServiceConfig).isNotNull();
        assertThat(buildServiceConfig.getResourceConfig()).isNotNull();
        assertThat(buildServiceConfig.getResourceConfig().getOpenshiftBuildConfig()).isNotNull();
        assertThat(buildServiceConfig.getResourceConfig().getOpenshiftBuildConfig().getLimits()).containsEntry("cpu", "200m");
        assertThat(buildServiceConfig.getResourceConfig().getOpenshiftBuildConfig().getRequests()).containsEntry("memory", "1Gi");
        assertThat(buildServiceConfig.getResourceDir().getPath()).isEqualTo("src/main/jkube");
    }
}
