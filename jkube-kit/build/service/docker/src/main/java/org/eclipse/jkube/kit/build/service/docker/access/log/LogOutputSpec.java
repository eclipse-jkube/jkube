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
package org.eclipse.jkube.kit.build.service.docker.access.log;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.eclipse.jkube.kit.build.service.docker.helper.Timestamp;
import org.fusesource.jansi.Ansi;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

import static org.fusesource.jansi.Ansi.Color.BLACK;
import static org.fusesource.jansi.Ansi.Color.BLUE;
import static org.fusesource.jansi.Ansi.Color.CYAN;
import static org.fusesource.jansi.Ansi.Color.GREEN;
import static org.fusesource.jansi.Ansi.Color.MAGENTA;
import static org.fusesource.jansi.Ansi.Color.RED;
import static org.fusesource.jansi.Ansi.Color.YELLOW;
import static org.fusesource.jansi.Ansi.ansi;

@Getter
@EqualsAndHashCode
public class LogOutputSpec {

    public static final LogOutputSpec DEFAULT = new LogOutputSpec("", YELLOW, false , null, null, true, true);
    private static final String DEFAULT_TIMESTAMP_PATTERN = "HH:mm:ss.SSS";
    private final boolean useColor;
    private final boolean logStdout;
    private final boolean fgBright;
    private final String prefix;
    private final Ansi.Color color;
    private final DateTimeFormatter timeFormatter;
    private final String file;

    // Palette used for prefixing the log output
    private static final Ansi.Color[] COLOR_PALETTE = {
            YELLOW,CYAN,MAGENTA,GREEN,RED,BLUE
    };
    private static int globalColorIdx = 0;

    @Builder(toBuilder = true)
    private LogOutputSpec(
        String prefix, Ansi.Color color, boolean fgBright, DateTimeFormatter timeFormatter, String file,
        boolean useColor, boolean logStdout) {

        this.prefix = prefix;
        this.color = color;
        this.fgBright = fgBright;
        this.timeFormatter = timeFormatter;
        this.file = file;
        this.useColor = useColor;
        this.logStdout = logStdout;
    }

    public boolean isUseColor() {
        return useColor && (getFile() == null || isLogStdout());
    }

    public String getPrompt(boolean withColor,Timestamp timestamp) {
        return formatTimestamp(timestamp,withColor) + formatPrefix(prefix, withColor);
    }

    private String formatTimestamp(Timestamp timestamp,boolean withColor) {
        if (timeFormatter == null) {
            return "";
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DEFAULT_TIMESTAMP_PATTERN);
        LocalDateTime localDateTime = LocalDateTime.from(formatter.parse(timestamp.toString()));

        return (withColor ?
                ansi().fgBright(BLACK).a(localDateTime).reset().toString() :
                localDateTime) + " ";
    }

    private String formatPrefix(String prefix, boolean withColor) {
        if (withColor) {
            Ansi ansi = ansi();
            if (fgBright) {
                ansi.fgBright(color);
            } else {
                ansi.fg(color);
            }
            return ansi.a(prefix).reset().toString();
        } else {
            return prefix;
        }
    }

    public static class LogOutputSpecBuilder {
        public LogOutputSpecBuilder colorString(String color) {
            return colorString(color, false);
        }

        public LogOutputSpecBuilder colorString(String color, boolean fgBright) {
            if (color == null) {
                this.color = COLOR_PALETTE[globalColorIdx++ % COLOR_PALETTE.length];
            } else {
                try {
                    this.color = Ansi.Color.valueOf(color.toUpperCase());
                    this.fgBright = fgBright;
                } catch (IllegalArgumentException exp) {
                    throw new IllegalArgumentException(
                            "Invalid color '" + color +
                                    "'. Color must be one of YELLOW, CYAN, MAGENTA, GREEN, RED, BLUE or BLACK");
                }
            }
            return this;
        }

        public LogOutputSpecBuilder timeFormatterString(String formatOrConstant) {
            if (formatOrConstant == null || formatOrConstant.equalsIgnoreCase("NONE")
                    || formatOrConstant.equalsIgnoreCase("FALSE")) {
                timeFormatter = null;
            } else if (formatOrConstant.length() == 0 || formatOrConstant.equalsIgnoreCase("DEFAULT")) {
                timeFormatter = DateTimeFormatter.ofPattern(DEFAULT_TIMESTAMP_PATTERN);
            } else if (formatOrConstant.equalsIgnoreCase("ISO8601")) {
                timeFormatter = DateTimeFormatter.ISO_DATE_TIME;
            } else if (formatOrConstant.equalsIgnoreCase("SHORT")) {
                timeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT);
            } else if (formatOrConstant.equalsIgnoreCase("MEDIUM")) {
                timeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM);
            } else if (formatOrConstant.equalsIgnoreCase("LONG")) {
                timeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG);
            } else if (formatOrConstant.equalsIgnoreCase("FULL")) {
                timeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL);
            } else {
                try {
                    timeFormatter = DateTimeFormatter.ofPattern(formatOrConstant);
                } catch (IllegalArgumentException exp) {
                    throw new IllegalArgumentException(
                            "Cannot parse log date specification '" + formatOrConstant + "'." +
                                    "Must be either DEFAULT, NONE, ISO8601, SHORT, MEDIUM, LONG, FULL or a " +
                                    "format string parseable by DateTimeFormat. See " +
                                    "https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html");
                }
            }
            return this;
        }
    }
}