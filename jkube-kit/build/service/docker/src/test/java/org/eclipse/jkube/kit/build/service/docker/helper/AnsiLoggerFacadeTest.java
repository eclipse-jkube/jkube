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
package org.eclipse.jkube.kit.build.service.docker.helper;

import org.eclipse.jkube.kit.common.KitLogger;
import org.fusesource.jansi.Ansi;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author roland
 * @since 07/10/16
 */
public class AnsiLoggerFacadeTest {

    @Test
    public void emphasize() {
        TestLog testLog = new TestLog();
        AnsiLoggerFacade logger = new AnsiLoggerFacade(testLog, true, false, false, "T>");
        Ansi ansi = Ansi.ansi();
        logger.info("Yet another [[*]]Test[[*]] %s","emphasis");
        assertEquals(ansi.a("T>")
                        .fg(AnsiLoggerFacade.COLOR_INFO)
                        .a("Yet another ")
                        .fgBright(AnsiLoggerFacade.COLOR_EMPHASIS)
                        .a("Test")
                        .fg(AnsiLoggerFacade.COLOR_INFO)
                        .a(" emphasis")
                        .reset().toString(),
                testLog.getMessage());
    }


    private class TestLog extends KitLogger.StdoutLogger {
        private String message;

        @Override
        public void info(String content, Object ... args) {
            this.message = content;
            super.info(content);
        }

        void reset() {
            message = null;
        }

        public String getMessage() {
            return message;
        }
    }

}

