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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.jkube.kit.common.KitLogger;

import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.StringUtils;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;

import static org.eclipse.jkube.kit.common.util.AnsiUtil.format;
import static org.eclipse.jkube.kit.common.util.AnsiUtil.Color.ERROR;
import static org.eclipse.jkube.kit.common.util.AnsiUtil.Color.INFO;
import static org.eclipse.jkube.kit.common.util.AnsiUtil.Color.PROGRESS_BAR;
import static org.eclipse.jkube.kit.common.util.AnsiUtil.Color.PROGRESS_ID;
import static org.eclipse.jkube.kit.common.util.AnsiUtil.Color.PROGRESS_STATUS;
import static org.eclipse.jkube.kit.common.util.AnsiUtil.Color.WARNING;
import static org.fusesource.jansi.Ansi.ansi;
import static org.fusesource.jansi.Ansi.Color.BLACK;

/**
 * Simple log handler for printing used during the maven build
 *
 * @author roland
 * @since 31.03.14
 */
public class AnsiLogger implements KitLogger {
  // prefix used for console output
  public static final String DEFAULT_LOG_PREFIX = "DOCKER> ";
  private static final int NON_ANSI_UPDATE_PERIOD = 80;

  private final Log log;
  private final String prefix;
  private final boolean batchMode;

  private final KitLogger fallbackLogger;

  private boolean isVerbose = false;
  private List<LogVerboseCategory> verboseModes = null;

  // Map remembering lines
  private final ThreadLocal<Map<String, Integer>> imageLines = new ThreadLocal<>();
  private final ThreadLocal<AtomicInteger> updateCount = new ThreadLocal<>();

  // Whether to use ANSI codes
  private boolean useAnsi;


  public AnsiLogger(Log log, boolean useColor, String verbose) {
    this(log, useColor, verbose, false);
  }

  public AnsiLogger(Log log, boolean useColor, String verbose, boolean batchMode) {
    this(log, useColor, verbose, batchMode, DEFAULT_LOG_PREFIX);
  }

  public AnsiLogger(Log log, boolean useColor, String verbose, boolean batchMode, String prefix) {
    this.log = log;
    this.prefix = prefix;
    this.batchMode = batchMode;
    this.fallbackLogger = new StdoutLogger();
    checkVerboseLoggingEnabled(verbose);
    initializeColor(useColor);
  }

  /** {@inheritDoc} */
  public void debug(String message, Object ... params) {
    if (isDebugEnabled()) {
      withFallback(
        () -> log.debug(prefix + format(message, params)),
        () -> fallbackLogger.debug(message, params));
    }
  }

  /** {@inheritDoc} */
  public void info(String message, Object ... params) {
    withFallback(
      () -> log.info(colored(message, INFO, params)),
      () -> fallbackLogger.info(message, params));
  }

  /** {@inheritDoc} */
  public void verbose(LogVerboseCategory logVerboseCategory, String message, Object ... params) {
    if (isVerbose && verboseModes != null && verboseModes.contains(logVerboseCategory)) {
      withFallback(
        () -> log.info(ansi().fgBright(BLACK).a(prefix).a(format(message, params)).reset().toString()),
        () -> fallbackLogger.info(message, params));
    }
  }

  /** {@inheritDoc} */
  public void warn(String format, Object ... params) {
    withFallback(
      () -> log.warn(colored(format, WARNING, params)),
      () -> fallbackLogger.warn(format, params));
  }

  /** {@inheritDoc} */
  public void error(String message, Object ... params) {
    withFallback(
      () -> log.error(colored(message, ERROR, params)),
      () -> fallbackLogger.error(message, params));
  }

  /**
   * Whether debugging is enabled.
   */
  public boolean isDebugEnabled() {
    return log.isDebugEnabled();
  }

  @Override
  public boolean isVerboseEnabled() {
    return isVerbose;
  }

  /**
   * Start a progress bar
   */
  @Override
  public void progressStart() {
    // A progress indicator is always written out to standard out if a tty is enabled.
    if (!batchMode && log.isInfoEnabled()) {
      imageLines.remove();
      updateCount.remove();
      imageLines.set(new HashMap<>());
      updateCount.set(new AtomicInteger());
    }
  }

  /**
   * Update the progress
   */
  @Override
  public void progressUpdate(String layerId, String status, String progressMessage) {
    if (!batchMode && log.isInfoEnabled() && StringUtils.isNotEmpty(layerId)) {
      if (useAnsi && isAnsiConsoleInstalled()) {
        updateAnsiProgress(layerId, status, progressMessage);
      } else {
        updateNonAnsiProgress();
      }
      flush();
    }
  }

  private void updateAnsiProgress(String imageId, String status, String progressMessage) {
    Map<String,Integer> imgLineMap = imageLines.get();
    Integer line = imgLineMap.get(imageId);

    int diff = 0;
    if (line == null) {
      line = imgLineMap.size();
      imgLineMap.put(imageId, line);
    } else {
      diff = imgLineMap.size() - line;
    }

    if (diff > 0) {
      print(ansi().cursorUp(diff).eraseLine(Ansi.Erase.ALL).toString());
    }

    // Status with progress bars: (max length = 11, hence pad to 11)
    // Extracting
    // Downloading
    String progress = progressMessage != null ? progressMessage : "";
    String msg =
      ansi()
        .fg(PROGRESS_ID.ansiColor).a(imageId).reset().a(": ")
        .fg(PROGRESS_STATUS.ansiColor).a(StringUtils.rightPad(status, 11) + " ")
        .fg(PROGRESS_BAR.ansiColor).a(progress).toString();
    println(msg);

    if (diff > 0) {
      // move cursor back down to bottom
      print(ansi().cursorDown(diff - 1).toString());
    }
  }

  private void updateNonAnsiProgress() {
    AtomicInteger count = updateCount.get();
    int nr = count.getAndIncrement();
    if (nr % NON_ANSI_UPDATE_PERIOD == 0) {
      print("#");
    }
    if (nr > 0 && nr % (80 * NON_ANSI_UPDATE_PERIOD) == 0) {
      print("\n");
    }
  }

  /**
   * Finis progress meter. Must be always called if {@link #progressStart()} has been used.
   */
  @Override
  public void progressFinished() {
    if (!batchMode && log.isInfoEnabled()) {
      imageLines.remove();
      print(ansi().reset().toString());
      if (!useAnsi) {
        println("");
      }
    }
  }

  private void flush() {
    System.out.flush();
  }

  private void initializeColor(boolean useColor) {
    this.useAnsi = useColor && !log.isDebugEnabled();
    if (useAnsi) {
      AnsiConsole.systemInstall();
      Ansi.setEnabled(true);
    }
    else {
      Ansi.setEnabled(false);
    }
  }

  private void println(String txt) {
    System.out.println(txt);
  }

  private void print(String txt) {
    System.out.print(txt);
  }

  private String colored(String message, AnsiUtil.Color color, Object... params) {
    return AnsiUtil.colored(prefix + message, color, params);
  }


  private void checkVerboseLoggingEnabled(String verbose) {
    if (verbose == null || verbose.equalsIgnoreCase("false")) {
      this.isVerbose = false;
      return;
    }
    if (verbose.equalsIgnoreCase("all")) {
      this.isVerbose = true;
      this.verboseModes = Arrays.asList(LogVerboseCategory.values());
      return;
    }
    if (verbose.isEmpty() || verbose.equalsIgnoreCase("true")) {
      this.isVerbose = true;
      this.verboseModes = Collections.singletonList(LogVerboseCategory.BUILD);
      return;
    }

    this.verboseModes = getVerboseModesFromString(verbose);
    this.isVerbose = true;
  }

  private List<LogVerboseCategory> getVerboseModesFromString(String groups) {
    List<LogVerboseCategory> ret = new ArrayList<>();
    for (String group : groups.split(",")) {
      try {
        ret.add(LogVerboseCategory.valueOf(group.toUpperCase()));
      } catch (Exception exp) {
        log.info("log: Unknown verbosity group " + groups + ". Ignoring...");
      }
    }
    return ret;
  }

  private static void withFallback(Runnable ansiFunc, Runnable fallbackFunc) {
    try {
      if (isAnsiConsoleInstalled()) {
        ansiFunc.run();
        return;
      }
    } catch (Exception ignore) {}
    fallbackFunc.run();
  }

  private static boolean isAnsiConsoleInstalled() {
    // Maven 3.6.3 uses jansi 1.17 which doesn't include this method
    try {
      AnsiConsole.class.getMethod("isInstalled");
      return AnsiConsole.isInstalled();
    } catch (NoSuchMethodException ex) {
      // Assume AnsiConsole is always installed in case the method doesn't exist
      return true;
    }
  }
}
