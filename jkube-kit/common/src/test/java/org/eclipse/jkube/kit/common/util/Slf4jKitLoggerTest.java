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
package org.eclipse.jkube.kit.common.util;

import org.eclipse.jkube.kit.common.KitLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class Slf4jKitLoggerTest {

  private Logger logger;
  private KitLogger kitLogger;

  @BeforeEach
  public void setUp() {
    logger = mock(Logger.class);
    kitLogger = new Slf4jKitLogger(logger);
  }

  @Test
  void debug() {
    // When
    kitLogger.debug("Something to debug %s", "replaced");
    // Then
    verify(logger).debug("Something to debug replaced");
  }

  @Test
  void info() {
    // When
    kitLogger.info("Something to info %-1.1s", "replaced");
    // Then
    verify(logger).info("Something to info r");
  }

  @Test
  void warn() {
    // When
    kitLogger.warn("Something to warn %-2.2s", "r");
    // Then
    verify(logger).warn("Something to warn r ");
  }

  @Test
  void error() {
    // When
    kitLogger.error("Something to error %-2.2s", "replaced");
    // Then
    verify(logger).error("Something to error re");
  }

  @Test
  void isDebugEnabled() {
    // Given
    when(logger.isDebugEnabled()).thenReturn(true);
    // When
    final boolean result = kitLogger.isDebugEnabled();
    // Then
    assertThat(result).isTrue();
  }
}