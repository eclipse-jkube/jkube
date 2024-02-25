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
package org.eclipse.jkube.kit.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class ExternalCommandTest {
  private KitLogger kitLogger;

  @BeforeEach
  void setUp() {
    kitLogger = spy(new KitLogger.SilentLogger());
  }

  @Test
  @DisabledOnOs(OS.WINDOWS)
  void execute_whenCommandCompletedSuccessfully_thenPrintResult() throws IOException {
    // Given
    TestCommand testCommand = new TestCommand(kitLogger, new String[] {"echo", "hello"});

    // When
    testCommand.execute();

    // Then
    assertThat(testCommand.getResult()).isEqualTo("hello");
  }

  @Test
  @DisabledOnOs(OS.WINDOWS)
  void execute_whenCommandFailed_thenThrowException() {
    // Given
    TestCommand testCommand = new TestCommand(kitLogger, new String[] {"ls", "idontexist"});

    // When + Then
    assertThatIOException()
        .isThrownBy(testCommand::execute)
        .withMessage("Process 'ls idontexist' exited with status 2");
  }

  @Test
  void execute_whenCommandFailed_thenLoggerError() {
    // Given
    TestCommand testCommand = new TestCommand(kitLogger, new String[] {"java", "idontexist"});

    // When + Then
    assertThatIOException()
        .isThrownBy(testCommand::execute)
        .withMessage("Process 'java idontexist' exited with status 1");
    verify(kitLogger).warn("Error: Could not find or load main class idontexist");
  }

  @Test
  void execute_whenCommandFailedButProcessErrorOverridden_thenErrorNotBlank() {
    // Given
    final StringBuilder errorBuilder = new StringBuilder();
    TestCommand overriddenProcessErrorTestCommand = new TestCommand(kitLogger, new String[] {"java", "idontexist"}, null) {
      @Override
      protected void processError(String errorLine) {
        errorBuilder.append(errorLine);
      }
    };

    // When + Then
    assertThatIOException()
        .isThrownBy(overriddenProcessErrorTestCommand::execute)
        .withMessage("Process 'java idontexist' exited with status 1");
    assertThat(errorBuilder.toString()).contains("Error: Could not find or load main class idontexist");
  }

  @Test
  @DisabledOnOs(OS.WINDOWS)
  void execute_whenWorkDirProvided_thenUseWorkDir(@TempDir File temporaryFolder) throws IOException {
    // Given
    TestCommand testCommand = new TestCommand(kitLogger, new String[] {"touch", "foo"}, temporaryFolder);

    // When
    testCommand.execute();

    // Then
    assertThat(new File(temporaryFolder, "foo")).exists();
  }

  private static class TestCommand extends ExternalCommand {
    private String result;
    private final String[] args;
    public TestCommand(KitLogger kitLogger, String[] args) {
      this(kitLogger, args, null);
    }

    public TestCommand(KitLogger kitLogger, String[] args, File dir) {
      super(kitLogger, dir);
      this.args = args;
    }

    @Override
    protected String[] getArgs() {
      return args;
    }

    @Override
    protected void processLine(String line) {
      result = line;
    }

    public String getResult() {
      return result;
    }
  }
}
