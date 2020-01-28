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

import org.eclipse.jkube.kit.build.core.JkubeBuildContext;
import org.eclipse.jkube.kit.build.core.config.JkubeAssemblyConfiguration;
import org.eclipse.jkube.kit.build.core.config.JkubeBuildConfiguration;
import org.eclipse.jkube.kit.common.JkubeProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.PrefixedLogger;
import org.eclipse.jkube.kit.config.image.build.DockerFileBuilder;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Mocked;
import mockit.Tested;
import mockit.Verifications;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.util.ReflectionUtils;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class DockerAssemblyManagerTest {

    @Mocked
    private PrefixedLogger prefixedLogger;

    @Tested
    private DockerAssemblyManager assemblyManager;

    @Injectable
    private ArchiverManager archiverManager;

    @Injectable
    private MappingTrackArchiver trackArchiver;

    @Test
    public void testNoAssembly() {
        JkubeBuildConfiguration buildConfig = new JkubeBuildConfiguration.Builder().build();
        JkubeAssemblyConfiguration assemblyConfig = buildConfig.getAssemblyConfiguration();

        DockerFileBuilder builder = assemblyManager.createDockerFileBuilder(buildConfig, assemblyConfig);
        String content = builder.content();

        assertFalse(content.contains("COPY"));
        assertFalse(content.contains("VOLUME"));
    }

    @Test
    public void assemblyFiles(@Injectable final JkubeBuildContext mojoParams,
                              @Injectable final JkubeProject project,
                              @Injectable final File assembly) throws IllegalAccessException, IOException {

        ReflectionUtils.setVariableValueInObject(assemblyManager, "trackArchiver", trackArchiver);

        new Expectations() {{
            mojoParams.getOutputDirectory();
            result = "target/"; times = 5;

            mojoParams.getProject();
            project.getBaseDirectory();
            result = ".";

        }};

        JkubeBuildConfiguration buildConfig = createBuildConfig();

        new File("./target/testImage/build/maven").mkdirs();
        AssemblyFiles assemblyFiles = assemblyManager.getAssemblyFiles("testImage", buildConfig, mojoParams, prefixedLogger);
        assertNotNull(assemblyFiles);
    }

    @Test
    public void testCopyValidVerifyGivenDockerfile(@Injectable final KitLogger logger) throws IOException {
        JkubeBuildConfiguration buildConfig = createBuildConfig();

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
        JkubeBuildConfiguration buildConfig = createBuildConfig();

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
        JkubeBuildConfiguration buildConfig = createBuildConfig();

        assemblyManager.verifyGivenDockerfile(
                new File(getClass().getResource("/docker/Dockerfile_assembly_verify_copy_chown_valid.test").getPath()),
                buildConfig,
                new Properties(),
                logger);

        new Verifications() {{
            logger.warn(anyString, (Object []) any); times = 0;
        }};
    }

    private JkubeBuildConfiguration createBuildConfig() {
        return new JkubeBuildConfiguration.Builder()
                .assembly(new JkubeAssemblyConfiguration.Builder()
                        .descriptorRef("artifact")
                        .build())
                .build();
    }

}

