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
package org.eclipse.jkube.kit.config.service.kubernetes;

import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.common.service.SummaryService;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SummaryServiceUtilTest {
  private JKubeServiceHub mockedJKubeServiceHub;
  private SummaryService mockedSummaryService;

  @BeforeEach
  public void setUp() {
    JKubeConfiguration mockedJKubeConfiguration = mock(JKubeConfiguration.class);
    when(mockedJKubeConfiguration.getBasedir()).thenReturn(new File("test"));
    mockedJKubeServiceHub = mock(JKubeServiceHub.class);
    mockedSummaryService = mock(SummaryService.class);
    when(mockedJKubeServiceHub.getSummaryService()).thenReturn(mockedSummaryService);
    when(mockedJKubeServiceHub.getConfiguration()).thenReturn(mockedJKubeConfiguration);
  }

  @Test
  void printSummary_whenNullJKubeServiceHub_thenDoesNothing() {
    // Given + When
    SummaryServiceUtil.printSummary(null);

    // Then
    verify(mockedJKubeServiceHub, times(0)).getSummaryService();
  }

  @Test
  void printSummary_whenJKubeServiceHub_thenPrintsSummary() {
    // Given + When
    SummaryServiceUtil.printSummary(mockedJKubeServiceHub);

    // Then
    verifySummaryPrinted(2);
  }

  @Test
  void printSummaryIfLastExecuting_whenLastExecutingNull_thenPrintsNothing() {
    // Given + When
    SummaryServiceUtil.printSummaryIfLastExecuting(mockedJKubeServiceHub, "resource", null);

    // Then
    verify(mockedJKubeServiceHub, times(0)).getSummaryService();
  }

  @Test
  void printSummaryIfLastExecuting_whenLastExecutingAndCurrentDifferent_thenPrintsNothing() {
    // Given + When
    SummaryServiceUtil.printSummaryIfLastExecuting(mockedJKubeServiceHub, "resource", "apply");

    // Then
    verify(mockedJKubeServiceHub, times(0)).getSummaryService();
  }

  @Test
  void printSummaryIfLastExecuting_whenLastExecutingAndCurrentSame_thenPrintsSummary() {
    // Given + When
    SummaryServiceUtil.printSummaryIfLastExecuting(mockedJKubeServiceHub, "resource",  "resource");

    // Then
    verifySummaryPrinted(2);
  }

  @Test
  void handleExceptionAndSummary_whenInvoked_thenSetsFailureAndPrintsSummary() {
    // Given + When
    SummaryServiceUtil.handleExceptionAndSummary(mockedJKubeServiceHub, new IOException("failure"));

    // Then
    verifySummaryPrinted(3);
    verify(mockedSummaryService).setFailureAndCause("failure");
  }


  private void verifySummaryPrinted(int summaryServiceInvocations) {
    verify(mockedJKubeServiceHub, times(summaryServiceInvocations)).getSummaryService();
    verify(mockedSummaryService).printSummary(any());
    verify(mockedSummaryService).clear();
  }

}
