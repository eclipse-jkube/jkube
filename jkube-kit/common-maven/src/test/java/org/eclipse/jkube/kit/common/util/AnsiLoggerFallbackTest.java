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
import org.fusesource.jansi.AnsiConsole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockConstructionWithAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class AnsiLoggerFallbackTest {

  private MockedConstruction<KitLogger.StdoutLogger> mockedLogger;

  @BeforeEach
  void setUp() {
    while(AnsiConsole.isInstalled()) {
      AnsiConsole.systemUninstall();
    }
    mockedLogger = mockConstructionWithAnswer(KitLogger.StdoutLogger.class, CALLS_REAL_METHODS);
  }

  @AfterEach
  void tearDown() {
    mockedLogger.close();
    if (!AnsiConsole.isInstalled()) {
      AnsiConsole.systemInstall();
    }
  }

  @Test
  void infoUsesFallbackWhenAnsiIsUninstalled() {
    // Given
    final AnsiLoggerTest.TestLog testLog = new AnsiLoggerTest.TestLog(true);
    final AnsiLogger logger = new AnsiLogger(testLog, true, null, false);
    // When
    logger.info("Greetings professor Falken");
    // Then
    assertThat(testLog.getMessage()).isNull();
    verify(mockedLogger.constructed().iterator().next(), times(1))
      .info("Greetings professor Falken");
  }
}
