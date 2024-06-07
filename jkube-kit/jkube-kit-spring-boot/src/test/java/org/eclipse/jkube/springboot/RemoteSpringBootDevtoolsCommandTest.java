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
package org.eclipse.jkube.springboot;

import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.EnvUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThatIOException;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class RemoteSpringBootDevtoolsCommandTest {
  private RemoteSpringBootDevtoolsCommand remoteSpringBootDevtoolsCommand;
  private KitLogger kitLogger;
  @TempDir
  private Path temporaryFolder;

  @BeforeEach
  void setUp() {
    kitLogger = spy(new KitLogger.SilentLogger());
    remoteSpringBootDevtoolsCommand = new RemoteSpringBootDevtoolsCommand(temporaryFolder.resolve("tmp.jar").toString(), "remote-secret", "https://test-url", kitLogger);
  }

  @Nested
  @DisplayName("execute")
  class Execute {

    @Test
    @EnabledOnOs(OS.LINUX)
    @DisplayName("when command successful, then verify exec called with correct arguments")
    void execute_whenCommandSucceeds_thenVerifyProcessCalledWithExpectedArguments() throws IOException {
      try {
        // Given
        Path testJavaHome = prepareJavaHomeWithJavaBinary("/dummy-java");
        Map<String, String> propMap = createMapOfOverriddenSystemProperties(testJavaHome);
        EnvUtil.overridePropertyGetter(propMap::get);
        // When
        remoteSpringBootDevtoolsCommand.execute();

        // Then
        ArgumentCaptor<String> javaProcessArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(kitLogger).debug(javaProcessArgumentCaptor.capture());
        assertThat(javaProcessArgumentCaptor.getValue())
          .contains(String.format("Running: " +
            "%s/bin/java -cp " +
            "%s/tmp.jar " +
            "-Dspring.devtools.remote.secret=remote-secret " +
            "org.springframework.boot.devtools.RemoteSpringApplication " +
            "https://test-url", testJavaHome, temporaryFolder));
      } finally {
        EnvUtil.overridePropertyGetter(System::getProperty);
      }
    }

    @Test
    @DisplayName("when command fails, then throw exception")
    void execute_whenCommandFails_thenExceptionThrown() throws IOException {
      try {
        // Given
        Path testJavaHome = prepareJavaHomeWithJavaBinary("/failing-java");
        Map<String, String> propMap = createMapOfOverriddenSystemProperties(testJavaHome);
        EnvUtil.overridePropertyGetter(propMap::get);
        // When + Then
        assertThatIOException()
          .isThrownBy(() -> remoteSpringBootDevtoolsCommand.execute())
          .withMessageContaining("Failed to start");
      } finally {
        EnvUtil.overridePropertyGetter(System::getProperty);
      }
    }

    private Path prepareJavaHomeWithJavaBinary(String resource) throws IOException {
      File dummyJavaArtifact =  new File(Objects.requireNonNull(RemoteSpringBootDevtoolsCommandTest.class.getResource(resource)).getFile());
      Path testJavaHome = temporaryFolder.resolve("java-home");
      Files.createDirectory(testJavaHome);
      Files.createDirectory(testJavaHome.resolve("bin"));
      Files.copy(dummyJavaArtifact.toPath(), testJavaHome.resolve("bin").resolve("java"), StandardCopyOption.COPY_ATTRIBUTES);
      return testJavaHome;
    }

    private Map<String, String> createMapOfOverriddenSystemProperties(Path overriddenJavaHome) {
      Map<String, String> propMap = new HashMap<>();
      propMap.put("java.home", overriddenJavaHome.toString());
      propMap.put("os.name", "linux");
      return propMap;
    }
  }


  @Test
  @DisplayName("should log process output via prefixed logger at info level")
  void processLine_shouldLogOutputWithPrefix() {
    // Given + When
    remoteSpringBootDevtoolsCommand.processLine("Test Output");
    // Then
    verify(kitLogger).info("Spring-Remote: %s", "Test Output");
  }

  @Test
  @DisplayName("should log process error via prefixed logger at error level")
  void processError_shouldLogOutputWithPrefix() {
    // Given + When
    remoteSpringBootDevtoolsCommand.processError("Test Error");
    // Then
    verify(kitLogger).error("Spring-Remote: %s", "Test Error");
  }
}
