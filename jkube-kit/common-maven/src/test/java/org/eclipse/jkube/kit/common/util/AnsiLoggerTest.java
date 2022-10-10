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

import org.eclipse.jkube.kit.common.KitLogger;

import org.apache.maven.monitor.logging.DefaultLog;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.fusesource.jansi.Ansi.Color.BLUE;
import static org.fusesource.jansi.Ansi.Color.GREEN;
import static org.fusesource.jansi.Ansi.Color.RED;
import static org.fusesource.jansi.Ansi.Color.YELLOW;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author roland
 * @since 07/10/16
 */
class AnsiLoggerTest {
    @BeforeAll
    public static void installAnsi() {
        AnsiConsole.systemInstall();
    }

    @BeforeEach
    public void forceAnsiPassthrough() {
        // Because the AnsiConsole keeps a per-VM counter of calls to systemInstall, it is
        // difficult to force it to pass through escapes to stdout during test.
        // Additionally, running the test in a suite (e.g. with mvn test) means other
        // code may have already initialized or manipulated the AnsiConsole.
        // Hence we just reset the stdout/stderr references to those captured by AnsiConsole
        // during its static initialization and restore them after tests.
        System.setOut(AnsiConsole.sysOut());
        System.setErr(AnsiConsole.sysErr());
    }

    @AfterAll
    public static void restoreAnsiPassthrough() {
        AnsiConsole.systemUninstall();
        System.setOut(AnsiConsole.out());
        System.setErr(AnsiConsole.err());
    }

    @Test
    void emphasizeDebug() {
        TestLog testLog = new TestLog() {
            @Override
            public boolean isDebugEnabled() {
                return true;
            }
        };

        AnsiLogger logger = new AnsiLogger(testLog, true, null, false, "T>");
        logger.debug("Debug messages do not interpret [[*]]%s[[*]]", "emphasis");
        assertThat(testLog.getMessage()).isEqualTo("T>Debug messages do not interpret [[*]]emphasis[[*]]");
    }

    @Test
    void emphasizeInfoWithDebugEnabled() {
        TestLog testLog = new TestLog() {
            @Override
            public boolean isDebugEnabled() {
                return true;
            }
        };

        AnsiLogger logger = new AnsiLogger(testLog, true, null, false, "T>");
        logger.info("Info messages do not apply [[*]]%s[[*]] when debug is enabled", "color codes");
        assertThat(testLog.getMessage()).isEqualTo("T>Info messages do not apply color codes when debug is enabled");
    }

    @Test
    void verboseEnabled() {
        String[] data = {
                "build", "Test",
                "api", null,
                "bla", "log: Unknown verbosity group bla. Ignoring...",
                "all", "Test",
                "", "Test",
                "true", "Test",
                "false", null
        };
        for (int i = 0; i < data.length; i += 2) {
            TestLog testLog = new TestLog();
            AnsiLogger logger = new AnsiLogger(testLog, false, data[i], false, "");
            logger.verbose( KitLogger.LogVerboseCategory.BUILD, "Test");
            assertThat(testLog.getMessage()).isEqualTo(data[i+1]);
        }
    }

    @Test
    void emphasizeInfo() {
        TestLog testLog = new TestLog();
        AnsiLogger logger = new AnsiLogger(testLog, true, null, false, "T>");
        Ansi ansi = Ansi.ansi();
        logger.info("Yet another [[*]]Test[[*]] %s", "emphasis");
        assertThat(testLog.getMessage()).isEqualTo(
                ansi.fg(GREEN)
                        .a("T>")
                        .a("Yet another ")
            .fgBright(BLUE)
                        .a("Test")
            .fg(GREEN)
                        .a(" emphasis")
                        .reset().toString()
        );
    }

    @Test
    void emphasizeInfoSpecificColor() {
        TestLog testLog = new TestLog();
        AnsiLogger logger = new AnsiLogger(testLog, true, null, false, "T>");
        Ansi ansi = new Ansi();
        logger.info("Specific [[C]]color[[C]] %s","is possible");
        assertThat(testLog.getMessage()).isEqualTo(
                ansi.fg(GREEN)
                        .a("T>")
                        .a("Specific ")
                        .fg(Ansi.Color.CYAN)
                        .a("color")
            .fg(GREEN)
                        .a(" is possible")
                        .reset().toString()
        );
    }

    @Test
    void emphasizeInfoIgnoringEmpties() {
        TestLog testLog = new TestLog();
        AnsiLogger logger = new AnsiLogger(testLog, true, null, false, "T>");
        Ansi ansi = new Ansi();
        // Note that the closing part of the emphasis does not need to match the opening.
        // E.g. [[b]]Blue[[*]] works just like [[b]]Blue[[b]]
        logger.info("[[b]][[*]]Skip[[*]][[*]]ping [[m]]empty strings[[/]] %s[[*]][[c]][[c]][[*]]","is possible");
        assertThat(testLog.getMessage()).isEqualTo(
                ansi.fg(GREEN)
                        .a("T>")
                        .a("Skipping ")
                        .fgBright(Ansi.Color.MAGENTA)
                        .a("empty strings")
            .fg(GREEN)
                        .a(" is possible")
                        .reset().toString()
        );
    }

    @Test
    void emphasizeInfoSpecificBrightColor() {
        TestLog testLog = new TestLog();
        AnsiLogger logger = new AnsiLogger(testLog, true, null, false, "T>");
        Ansi ansi = new Ansi();
        logger.info("Lowercase enables [[c]]bright version[[c]] of %d colors",Ansi.Color.values().length - 1);
        assertThat(testLog.getMessage()).isEqualTo(
                ansi.fg(GREEN)
                        .a("T>")
                        .a("Lowercase enables ")
                        .fgBright(Ansi.Color.CYAN)
                        .a("bright version")
            .fg(GREEN)
                        .a(" of 8 colors")
                        .reset().toString()
        );
    }

    @Test
    void emphasizeInfoWithoutColor() {
        TestLog testLog = new TestLog();
        AnsiLogger logger = new AnsiLogger(testLog, false, null, false, "T>");
        logger.info("Disabling color causes logger to [[*]]interpret and remove[[*]] %s","emphasis");
        assertThat(testLog.getMessage()).isEqualTo("T>Disabling color causes logger to interpret and remove emphasis");
    }

    @Test
    void emphasizeWarning() {
        TestLog testLog = new TestLog();
        AnsiLogger logger = new AnsiLogger(testLog, true, null, false, "T>");
        Ansi ansi = new Ansi();
        logger.warn("%s messages support [[*]]emphasis[[*]] too","Warning");
        assertThat(testLog.getMessage()).isEqualTo(
                ansi.fg(YELLOW)
                        .a("T>")
                        .a("Warning messages support ")
            .fgBright(BLUE)
                        .a("emphasis")
            .fg(YELLOW)
                        .a(" too")
                        .reset().toString()
        );
    }

    @Test
    void emphasizeError() {
        TestLog testLog = new TestLog();
        AnsiLogger logger = new AnsiLogger(testLog, true, null, false, "T>");
        Ansi ansi = new Ansi();
        logger.error("Error [[*]]messages[[*]] could emphasise [[*]]%s[[*]]","many things");
        assertThat(testLog.getMessage()).isEqualTo(
                ansi.fg(RED)
                        .a("T>")
                        .a("Error ")
            .fgBright(BLUE)
                        .a("messages")
            .fg(RED)
                        .a(" could emphasise ")
            .fgBright(BLUE)
                        .a("many things")
                        .reset()
                        .toString()
        );
    }


    private class TestLog extends DefaultLog {
        private String message;

        public TestLog() {
            super(new ConsoleLogger(1, "console"));
        }

        @Override
        public void debug(CharSequence content) {
            this.message = content.toString();
            super.debug(content);
        }

        @Override
        public void info(CharSequence content) {
            this.message = content.toString();
            super.info(content);
        }

        @Override
        public void warn(CharSequence content) {
            this.message = content.toString();
            super.warn(content);
        }

        @Override
        public void error(CharSequence content) {
            this.message = content.toString();
            super.error(content);
        }

        void reset() {
            message = null;
        }

        public String getMessage() {
            return message;
        }
    }

}

