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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
@RunWith(MockitoJUnitRunner.class)
public class Slf4jKitLoggerTest {

  @Mock
  private Logger logger;

  private KitLogger kitLogger;


  @Before
  public void setUp() {
    kitLogger = new Slf4jKitLogger(logger);
  }

  @Test
  public void debug() {
    // When
    kitLogger.debug("Something to debug %s", "replaced");
    // Then
    verify(logger).debug("Something to debug replaced");
  }

  @Test
  public void info() {
    // When
    kitLogger.info("Something to info %-1.1s", "replaced");
    // Then
    verify(logger).info("Something to info r");
  }

  @Test
  public void warn() {
    // When
    kitLogger.warn("Something to warn %-2.2s", "r");
    // Then
    verify(logger).warn("Something to warn r ");
  }

  @Test
  public void error() {
    // When
    kitLogger.error("Something to error %-2.2s", "replaced");
    // Then
    verify(logger).error("Something to error re");
  }

  @Test
  public void isDebugEnabled() {
    // Given
    when(logger.isDebugEnabled()).thenReturn(true);
    // When
    final boolean result = kitLogger.isDebugEnabled();
    // Then
    assertTrue(result);
  }
}