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
package org.eclipse.jkube.kit.build.api.assembly;

import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;

@RunWith(Parameterized.class)
public class AssemblyManagerVerifyAssemblyReferencedInDockerfileTest {

    KitLogger logger;
    @Before
    public void setUp(){
        logger=spy(KitLogger.SilentLogger.class);
    }
    @Parameterized.Parameters(name = "{0}: verifyDockerFile({1})={2}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { "Valid File", "/docker/Dockerfile_assembly_verify_copy_valid.test", 0 },
                { "Invalid File", "/docker/Dockerfile_assembly_verify_copy_invalid.test", 1 },
                { "chown File", "/docker/Dockerfile_assembly_verify_copy_chown_valid.test", 0 }
        });
    }
    @Parameterized.Parameter
    public String description;
    @Parameterized.Parameter(1)
    public String dockerFile;
    @Parameterized.Parameter(2)
    public Integer expectedLogWarnings;
    @Test
    public void verifyAssemblyReferencedInDockerfile_logsWarning() throws IOException {
        BuildConfiguration buildConfig = createBuildConfig();

        AssemblyManager.verifyAssemblyReferencedInDockerfile(
                new File(getClass().getResource(dockerFile).getPath()),
                buildConfig, new Properties(),
                logger);
        verify(logger,times(expectedLogWarnings)).warn(anyString(), any());
    }
    private BuildConfiguration createBuildConfig() {
        return BuildConfiguration.builder()
                .assembly(AssemblyConfiguration.builder()
                        .name("maven")
                        .targetDir("/maven")
                        .build())
                .build();
    }
}
