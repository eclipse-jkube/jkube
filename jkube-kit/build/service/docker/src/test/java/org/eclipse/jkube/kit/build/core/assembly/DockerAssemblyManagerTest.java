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
package org.eclipse.jkube.kit.build.core.assembly;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.eclipse.jkube.kit.build.core.JKubeBuildContext;
import org.eclipse.jkube.kit.build.core.config.JKubeAssemblyConfiguration;
import org.eclipse.jkube.kit.build.core.config.JKubeBuildConfiguration;
import org.eclipse.jkube.kit.common.JKubeProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.PrefixedLogger;
import org.eclipse.jkube.kit.config.image.build.DockerFileBuilder;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Mocked;
import mockit.Tested;
import mockit.Verifications;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class DockerAssemblyManagerTest {

    @Mocked
    private PrefixedLogger prefixedLogger;

    @Tested
    private DockerAssemblyManager assemblyManager;

    @Test
    public void testNoAssembly() {
        JKubeBuildConfiguration buildConfig = new JKubeBuildConfiguration.Builder().build();
        JKubeAssemblyConfiguration assemblyConfig = buildConfig.getAssemblyConfiguration();

        DockerFileBuilder builder = assemblyManager.createDockerFileBuilder(buildConfig, assemblyConfig);
        String content = builder.content();

        assertFalse(content.contains("COPY"));
        assertFalse(content.contains("VOLUME"));
    }

    @Test
    public void assemblyFiles(@Injectable final JKubeBuildContext mojoParams,
                              @Injectable final JKubeProject project,
                              @Injectable final File assembly) throws IllegalAccessException, IOException {


        new Expectations() {{
            mojoParams.getProject();
            project.getBaseDirectory();

            project.getBuildDirectory();
            result = "target/";
            result = ".";

        }};

        JKubeBuildConfiguration buildConfig = createBuildConfig();

        new File("./target/testImage/build/maven").mkdirs();
        AssemblyFiles assemblyFiles = assemblyManager.getAssemblyFiles("testImage", buildConfig, mojoParams, prefixedLogger);
        assertNotNull(assemblyFiles);
    }

    @Test
    public void testCopyValidVerifyGivenDockerfile(@Injectable final KitLogger logger) throws IOException {
        JKubeBuildConfiguration buildConfig = createBuildConfig();

        assemblyManager.verifyGivenDockerfile(
                new File(getClass().getResource("/docker/Dockerfile_assembly_verify_copy_valid.test").getPath()),
                buildConfig, new Properties(),
                logger);

        new Verifications() {{
            logger.warn(anyString, (Object []) any); times = 0;
        }};

    }

    @Test
    public void testCopyInvalidVerifyGivenDockerfile(@Injectable final KitLogger logger) throws IOException {
        JKubeBuildConfiguration buildConfig = createBuildConfig();

        assemblyManager.verifyGivenDockerfile(
                new File(getClass().getResource("/docker/Dockerfile_assembly_verify_copy_invalid.test").getPath()),
                buildConfig, new Properties(),
                logger);

        new Verifications() {{
            logger.warn(anyString, (Object []) any); times = 1;
        }};

    }

    @Test
    public void testCopyChownValidVerifyGivenDockerfile(@Injectable final KitLogger logger) throws IOException {
        JKubeBuildConfiguration buildConfig = createBuildConfig();

        assemblyManager.verifyGivenDockerfile(
                new File(getClass().getResource("/docker/Dockerfile_assembly_verify_copy_chown_valid.test").getPath()),
                buildConfig,
                new Properties(),
                logger);

        new Verifications() {{
            logger.warn(anyString, (Object []) any); times = 0;
        }};
    }

    private JKubeBuildConfiguration createBuildConfig() {
        return new JKubeBuildConfiguration.Builder()
                .assembly(new JKubeAssemblyConfiguration.Builder()
                        .descriptorRef("artifact")
                        .build())
                .build();
    }

}

