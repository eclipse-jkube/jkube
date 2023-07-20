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

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class ArgumentsTest {

  @Test
  void testShellArgWithSpaceEscape() {
    String[] testSubject = { "java", "-jar", "$HOME/name with space.jar" };
    Arguments arg = Arguments.builder().shell("java -jar $HOME/name\\ with\\ space.jar").build();
    assertThat(arg.asStrings()).containsExactly(testSubject);
  }

  @Test
  void set_whenInvoked_shouldSetShell() {
    // Given
    Arguments arg = new Arguments();

    // When
    arg.set("sleep 10");

    // Then
    assertThat(arg.getShell()).isEqualTo("sleep 10");
    assertThat(arg.asStrings()).containsExactly("sleep", "10");
  }

  @Test
  void exec_withSpaceEscapeArg_shouldReturnCorrectCmd() {
    // Given
    Arguments arg = Arguments.builder().exec(Arrays.asList("wget", "-O", "/work-dir/index.html", "http://info.cern.ch")).build();

    // When
    List<String> result = arg.asStrings();

    // Then
    assertThat(result).containsExactly("wget", "-O", "/work-dir/index.html", "http://info.cern.ch");
  }

  @Test
  void execInlined_withSpaceEscapeArg_shouldReturnCorrectCmd() {
    // Given
    List<String> execInline = Arrays.asList("wget", "-O", "/work-dir/index.html", "http://info.cern.ch");
    Arguments arg = new Arguments(null, null, execInline);

    // When
    List<String> result = arg.asStrings();

    // Then
    assertThat(arg.getExecInlined()).isEqualTo(execInline);
    assertThat(result).containsExactly("wget", "-O", "/work-dir/index.html", "http://info.cern.ch");
  }

  @Test
  void getExec_whenInvoked_thenReturnsEitherExecOrExecInlined() {
    // Given
    Arguments arg = new Arguments(null, null, Arrays.asList("wget", "-O", "/work-dir/index.html", "http://info.cern.ch"));

    // When
    List<String> exec = arg.getExec();

    // Then
    assertThat(exec).containsExactly("wget", "-O", "/work-dir/index.html", "http://info.cern.ch");
  }

  @Test
  void validate_withValidObjects_thenNoExceptionThrown() {
    // Given
    Arguments shell = new Arguments("sleep 10", null, null);
    Arguments exec = new Arguments(null, Arrays.asList("sleep", "10"), null);
    Arguments execInlined = new Arguments(null, null, Arrays.asList("sleep", "10"));

    // When
    shell.validate();
    exec.validate();
    execInlined.validate();

    // Then
    assertThat(shell).isNotNull();
    assertThat(exec).isNotNull();
    assertThat(execInlined).isNotNull();
  }

  @Test
  void validate_whenWrongObjectProvided_thenThrowException() {
    // Given
    Arguments arg = new Arguments();

    // When + Then
    assertThatIllegalArgumentException()
        .isThrownBy(arg::validate)
        .withMessage("Argument conflict: either shell or args should be specified and only in one form.");
  }

  @Test
  void equalsAndHashCodeShouldMatch() {
    // Given
    Arguments  a1 = Arguments.builder().shell("sleep 10").build();
    Arguments a2 = Arguments.builder().shell("sleep 10").build();
    // When + Then
    assertThat(a1)
        .isEqualTo(a2)
        .hasSameHashCodeAs(a2);
  }
}
