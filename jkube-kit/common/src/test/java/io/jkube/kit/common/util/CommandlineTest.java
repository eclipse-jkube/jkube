package io.jkube.kit.common.util;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class CommandlineTest {

    CommandLine commandline = new CommandLine();
    List<String> result = new ArrayList<>();
    List<String> expected = new ArrayList<>();

    @Test
    public void simpleEmptyTest(){
        result = commandline.translateCommandline("");
        assertEquals(expected,result);
    }

    @Test
    public void simpleNullTest(){
        result = commandline.translateCommandline(null);
        assertEquals(expected,result);
    }

    @Test
    public void simpleCommandTest(){
        expected.clear();
        expected.add("cd");
        expected.add("/tmp");
        result = commandline.translateCommandline("cd /tmp");
        assertEquals(expected,result);
    }

    @Test
    public void CommandWithDoubleQuoteTest(){
        expected.clear();
        expected.add("echo");
        expected.add("Hello! World");
        result = commandline.translateCommandline("echo \"Hello! World\"");
        assertEquals(expected,result);
    }

    @Test
    public void commandWithBothTypeofQuotesTest(){
        expected.clear();
        expected.add("echo");
        expected.add("Hello! World");
        expected.add("Hello Java Folks");
        result = commandline.
                translateCommandline("echo \"Hello! World\" \'Hello Java Folks\'");
        assertEquals(expected,result);
    }

    @Test
    public void commandWithNestedQuotesTest(){
        expected.clear();
        expected.add("echo");
        expected.add("Hello! World \'Hello Java Folks\'");
        result = commandline.
                translateCommandline("echo \"Hello! World \'Hello Java Folks\'\"");
        assertEquals(expected,result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidDoubleQuoteCommandTest(){
        result = commandline.
                translateCommandline("echo \"Hello! World\" \'Hello Java Folks");
    }
    @Test(expected = IllegalArgumentException.class)
    public void invalidSingleQuoteCommandTest(){
        result = commandline.
                translateCommandline("echo \"Hello! World \'Hello Java Folks\'");
    }
}