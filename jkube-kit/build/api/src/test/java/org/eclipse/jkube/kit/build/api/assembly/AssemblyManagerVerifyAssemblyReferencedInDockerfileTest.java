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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.stream.Stream;

import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class AssemblyManagerVerifyAssemblyReferencedInDockerfileTest {

    private KitLogger logger;

    @BeforeEach
    void setUp(){
        logger=spy(KitLogger.SilentLogger.class);
    }

    static Stream<Arguments> data() {
      return Stream.of(
          arguments("Valid File", "/docker/Dockerfile_assembly_verify_copy_valid.test", 0),
          arguments("Invalid File", "/docker/Dockerfile_assembly_verify_copy_invalid.test", 1),
          arguments("chown File", "/docker/Dockerfile_assembly_verify_copy_chown_valid.test", 0));
    }

    @DisplayName("verify, assembly referenced in dockerfile when docker file is provided")
    @ParameterizedTest(name = "{0}: verifyDockerFile logs {2} warnings for {1}")
    @MethodSource("data")
    void verifyAssemblyReferencedInDockerfile_whenDockerfileProvided_thenLogsNWarnings(String description, String dockerFile,
        int expectedLogWarnings) throws IOException {
      AssemblyConfiguration assemblyConfiguration = AssemblyConfiguration.builder()
          .name("maven")
          .targetDir("/maven")
          .build();
      BuildConfiguration buildConfig = createBuildConfig(assemblyConfiguration);

      AssemblyManager.verifyAssemblyReferencedInDockerfile(
          new File(getClass().getResource(dockerFile).getPath()),
          buildConfig.getFilter(), assemblyConfiguration, new Properties(),
          logger);
      verify(logger, times(expectedLogWarnings)).warn(anyString(), any());
    }

    private BuildConfiguration createBuildConfig(AssemblyConfiguration assemblyConfiguration) {
      return BuildConfiguration.builder()
          .assembly(assemblyConfiguration)
          .build();
    }
}
