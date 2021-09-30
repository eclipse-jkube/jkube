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

import org.fusesource.jansi.Ansi;

import java.util.HashMap;
import java.util.Map;

import static org.fusesource.jansi.Ansi.Color.BLACK;
import static org.fusesource.jansi.Ansi.Color.BLUE;
import static org.fusesource.jansi.Ansi.Color.CYAN;
import static org.fusesource.jansi.Ansi.Color.DEFAULT;
import static org.fusesource.jansi.Ansi.Color.GREEN;
import static org.fusesource.jansi.Ansi.Color.MAGENTA;
import static org.fusesource.jansi.Ansi.Color.RED;
import static org.fusesource.jansi.Ansi.Color.WHITE;
import static org.fusesource.jansi.Ansi.Color.YELLOW;
import static org.fusesource.jansi.Ansi.ansi;

public class AnsiUtil {

  // ANSI escapes for various colors (or empty strings if no coloring is used)
  public enum Color {
    ERROR(RED),
    INFO(GREEN),
    WARNING(YELLOW),
    PROGRESS_ID(YELLOW),
    PROGRESS_STATUS(GREEN),
    PROGRESS_BAR(CYAN),
    EMPHASIS(BLUE);

    final Ansi.Color ansiColor;

    Color(Ansi.Color ansiColor) {
      this.ansiColor = ansiColor;
    }
  }

  private static final Map<String, Ansi.Color> COLOR_MAP = new HashMap<>();

  static {
    COLOR_MAP.put("*", Color.EMPHASIS.ansiColor);
    COLOR_MAP.put("B", BLUE);
    COLOR_MAP.put("C", CYAN);
    COLOR_MAP.put("Y", YELLOW);
    COLOR_MAP.put("G", GREEN);
    COLOR_MAP.put("M", MAGENTA);
    COLOR_MAP.put("R", RED);
    COLOR_MAP.put("W", WHITE);
    COLOR_MAP.put("S", BLACK);
    COLOR_MAP.put("D", DEFAULT);
  }

  private AnsiUtil() {
  }

  public static String colored(String message, Color color, Object... params) {
    Ansi ansi = ansi().fg(color.ansiColor);
    return ansi.a(format(evaluateEmphasis(message, color), params)).reset().toString();
  }

  // Use parameters when given, otherwise we use the string directly
  public static String format(String message, Object[] params) {
    if (params.length == 0) {
      return message;
    } else if (params.length == 1 && params[0] instanceof Throwable) {
      // We print only the message here since breaking exception will bubble up
      // anyway
      return message + ": " + params[0];
    } else {
      return String.format(message, params);
    }
  }

  // Emphasize parts encloses in "[[*]]" tags
  private static String evaluateEmphasis(String message, Color color) {
    // Split but keep the content by splitting on [[ and ]] separately when they
    // are followed or preceded by their counterpart. This lets the split retain
    // the character in the center.
    String[] parts = message.split("(\\[\\[(?=.]])|(?<=\\[\\[.)]])");
    if (parts.length == 1) {
      return message;
    }
    // The split up string is comprised of a leading plain part, followed
    // by groups of colorization that are <SET> color-part <RESET> plain-part.
    // To avoid emitting needless color changes, we skip the set or reset
    // if the subsequent part is empty.
    String msgColorS = ansi().fg(color.ansiColor).toString();
    StringBuilder ret = new StringBuilder(parts[0]);

    for (int i = 1; i < parts.length; i += 4) {
      boolean colorPart = i + 1 < parts.length && parts[i + 1].length() > 0;
      boolean plainPart = i + 3 < parts.length && parts[i + 3].length() > 0;

      if (colorPart) {
        ret.append(computeEmphasisColor(parts[i]));
        ret.append(parts[i + 1]);
        if (plainPart) {
          ret.append(msgColorS);
        }
      }
      if (plainPart) {
        ret.append(parts[i + 3]);
      }
    }
    return ret.toString();
  }

  private static String computeEmphasisColor(String id) {
    Ansi.Color color = COLOR_MAP.get(id.toUpperCase());
    if (color != null) {
      return id.equals(id.toLowerCase()) ?
      // lower case letter means bright color ...
          ansi().fgBright(color).toString() : ansi().fg(color).toString();
    } else {
      return "";
    }
  }
}
