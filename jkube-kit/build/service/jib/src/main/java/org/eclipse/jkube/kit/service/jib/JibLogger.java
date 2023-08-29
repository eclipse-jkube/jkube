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
package org.eclipse.jkube.kit.service.jib;

import com.google.cloud.tools.jib.api.LogEvent;
import com.google.cloud.tools.jib.event.progress.ProgressEventHandler;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jkube.kit.common.KitLogger;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import static org.fusesource.jansi.Ansi.ansi;

public class JibLogger implements Consumer<LogEvent> {

  /**
   * Line above progress bar.
   */
  private static final String HEADER = "Executing tasks:";

  /**
   * Maximum number of bars in the progress display.
   */
  private static final int PROGRESS_BAR_COUNT = 30;
  public static final String JIB_LOG_PREFIX = "JIB> ";

  private final KitLogger logger;
  private final PrintStream out;

  public JibLogger(KitLogger logger) {
    this(logger, System.out);
  }

  public JibLogger(KitLogger logger, PrintStream out) {
    this.logger = logger;
    this.out = out;
  }

  @Override
  public void accept(LogEvent logEvent) {
    if (logEvent.getLevel() != LogEvent.Level.DEBUG || logger.isVerboseEnabled() || logger.isDebugEnabled()) {
      out.println(ansi().cursorUpLine(1).eraseLine().a(JIB_LOG_PREFIX)
        .a(StringUtils.rightPad(logEvent.getMessage(), 120)).a("\n"));
    }
  }

  ProgressEventHandler progressEventHandler() {
    return new ProgressEventHandler(update -> {
      final List<String> progressDisplay =
        generateProgressDisplay(update.getProgress(), update.getUnfinishedLeafTasks());
      if (progressDisplay.size() > 2 && progressDisplay.stream().allMatch(Objects::nonNull)) {
        final String progressBar = progressDisplay.get(1);
        final String task = progressDisplay.get(2);
        out.println(ansi().cursorUpLine(1).eraseLine().a(JIB_LOG_PREFIX).a(progressBar).a(" ").a(task));
      }
    });
  }

  void updateFinished() {
    out.println(JIB_LOG_PREFIX + generateProgressBar(1.0F));
  }

  /**
   * Generates a progress display.
   *
   * Taken from https://github.com/GoogleContainerTools/jib/blob/master/jib-plugins-common/src/main/java/com/google/cloud/tools/jib/plugins/common/logging/ProgressDisplayGenerator.java#L47
   *
   * @param progress the overall progress, with {@code 1.0} meaning fully complete
   * @param unfinishedLeafTasks the unfinished leaf tasks
   * @return the progress display as a list of lines
   */
  private static List<String> generateProgressDisplay(double progress, List<String> unfinishedLeafTasks) {
    List<String> lines = new ArrayList<>();

    lines.add(HEADER);
    lines.add(generateProgressBar(progress));
    for (String task : unfinishedLeafTasks) {
      lines.add("> " + task);
    }

    return lines;
  }

  /**
   * Generates the progress bar line.
   *
   * Taken from https://github.com/GoogleContainerTools/jib/blob/master/jib-plugins-common/src/main/java/com/google/cloud/tools/jib/plugins/common/logging/ProgressDisplayGenerator.java#L66
   *
   * @param progress the overall progress, with {@code 1.0} meaning fully complete
   * @return the progress bar line
   */
  private static String generateProgressBar(double progress) {
    StringBuilder progressBar = new StringBuilder();
    progressBar.append('[');

    int barsToDisplay = (int) Math.round(PROGRESS_BAR_COUNT * progress);
    for (int barIndex = 0; barIndex < PROGRESS_BAR_COUNT; barIndex++) {
      progressBar.append(barIndex < barsToDisplay ? '=' : ' ');
    }

    return progressBar
      .append(']')
      .append(String.format(" %.1f", progress * 100))
      .append("% complete")
      .toString();
  }


}
