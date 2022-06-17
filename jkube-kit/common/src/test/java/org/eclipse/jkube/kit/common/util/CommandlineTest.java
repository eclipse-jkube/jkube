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
package org.eclipse.jkube.kit.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CommandlineTest {

    private List<String> result;

    @Test
    void simpleEmptyTest(){
        result = CommandLine.translateCommandline("");
        assertThat(result).isEmpty();
    }

    @Test
    void simpleNullTest(){
        result = CommandLine.translateCommandline(null);
        assertThat(result).isEmpty();
    }

    @Test
    void commandWithBothTypeofQuotesTest(){
        result = CommandLine.
                translateCommandline("echo \"Hello! World\" 'Hello Java Folks'");
        assertThat(result).containsExactly("echo", "Hello! World", "Hello Java Folks");
    }

    @Test
    void invalidDoubleQuoteCommandTest(){
        assertThrows(IllegalArgumentException.class, () -> result = CommandLine.
                translateCommandline("echo \"Hello! World\" 'Hello Java Folks"));
    }
    @Test
    void invalidSingleQuoteCommandTest(){
        assertThrows(IllegalArgumentException.class, () -> result = CommandLine.
                translateCommandline("echo \"Hello! World 'Hello Java Folks'"));
    }

    @DisplayName("Command Translation Tests")
    @ParameterizedTest(name = "{0} ''{1}'' should return ''{2}''")
    @MethodSource("translateCmdTestData")
    void translateCmd(String testDesc, String toProcess, List<String> expected){
        result = CommandLine.translateCommandline(toProcess);
        assertThat(result).isEqualTo(expected);
    }

    public static Stream<Arguments> translateCmdTestData(){
        return Stream.of(
                Arguments.of("simpleCommand", "cd /tmp", Arrays.asList("cd", "/tmp")),
                Arguments.of("commandWithDoubleQuote", "echo \"Hello! World\"", Arrays.asList("echo", "Hello! World")),
                Arguments.of("commandWithNestedQuotes", "echo \"Hello! World 'Hello Java Folks'\"", Arrays.asList("echo", "Hello! World 'Hello Java Folks'"))
        );
    }
}
