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

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class CommandlineTest {

    List<String> result = new ArrayList<>();
    List<String> expected = new ArrayList<>();

    @Test
    public void simpleEmptyTest(){
        result = CommandLine.translateCommandline("");
        assertEquals(expected,result);
    }

    @Test
    public void simpleNullTest(){
        result = CommandLine.translateCommandline(null);
        assertEquals(expected,result);
    }

    @Test
    public void simpleCommandTest(){
        expected.clear();
        expected.add("cd");
        expected.add("/tmp");
        result = CommandLine.translateCommandline("cd /tmp");
        assertEquals(expected,result);
    }

    @Test
    public void CommandWithDoubleQuoteTest(){
        expected.clear();
        expected.add("echo");
        expected.add("Hello! World");
        result = CommandLine.translateCommandline("echo \"Hello! World\"");
        assertEquals(expected,result);
    }

    @Test
    public void commandWithBothTypeofQuotesTest(){
        expected.clear();
        expected.add("echo");
        expected.add("Hello! World");
        expected.add("Hello Java Folks");
        result = CommandLine.
                translateCommandline("echo \"Hello! World\" 'Hello Java Folks'");
        assertEquals(expected,result);
    }

    @Test
    public void commandWithNestedQuotesTest(){
        expected.clear();
        expected.add("echo");
        expected.add("Hello! World 'Hello Java Folks'");
        result = CommandLine.
                translateCommandline("echo \"Hello! World 'Hello Java Folks'\"");
        assertEquals(expected,result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidDoubleQuoteCommandTest(){
        result = CommandLine.
                translateCommandline("echo \"Hello! World\" 'Hello Java Folks");
    }
    @Test(expected = IllegalArgumentException.class)
    public void invalidSingleQuoteCommandTest(){
        result = CommandLine.
                translateCommandline("echo \"Hello! World 'Hello Java Folks'");
    }
}