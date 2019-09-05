package io.jshift.kit.build.service.docker.helper;
/*
 *
 * Copyright 2016 Roland Huss
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import io.jshift.kit.common.KitLogger;
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

