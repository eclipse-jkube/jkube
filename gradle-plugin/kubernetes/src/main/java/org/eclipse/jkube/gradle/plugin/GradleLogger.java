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
package org.eclipse.jkube.gradle.plugin;

import org.eclipse.jkube.kit.common.util.AnsiUtil;
import org.eclipse.jkube.kit.common.util.Slf4jKitLogger;

import org.fusesource.jansi.Ansi;
import org.gradle.api.logging.Logger;

import static org.eclipse.jkube.kit.common.util.AnsiUtil.colored;
import static org.eclipse.jkube.kit.common.util.AnsiUtil.format;
import static org.eclipse.jkube.kit.common.util.AnsiUtil.Color.ERROR;
import static org.eclipse.jkube.kit.common.util.AnsiUtil.Color.INFO;
import static org.eclipse.jkube.kit.common.util.AnsiUtil.Color.WARNING;

public class GradleLogger extends Slf4jKitLogger {

  private final Logger delegate;
  private final String prefix;

  public GradleLogger(Logger delegate, boolean ansiEnabled, String prefix) {
    super(delegate);
    this.delegate = delegate;
    this.prefix = prefix;
    Ansi.setEnabled(ansiEnabled);
  }

  @Override
  public void info(String format, Object... params) {
    delegate.lifecycle(withAnsi(INFO, format, params));
  }

  @Override
  public void debug(String message, Object... params) {
    delegate.debug(format(message, params));
  }

  @Override
  public void warn(String format, Object... params) {
    delegate.warn(withAnsi(WARNING, format, params));
  }

  @Override
  public void error(String format, Object... params) {
    delegate.error(withAnsi(ERROR, format, params));
  }

  private String withAnsi(AnsiUtil.Color color, String message, Object... params) {
    return colored(withPrefix(message), color, params);
  }

  private String withPrefix(String message) {
    return prefix + message;
  }
}
