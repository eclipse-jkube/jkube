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

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class OpenshiftHelperStatusTest {
  @Parameterized.Parameters(name = "OpenShift Status: ''{0}''")
  public static Collection<Object[]> data() {
    return Arrays.asList(
        // input, isFinished, isCancelled, isFailed, isCompleted
        new Object[] { "Complete", true, false, false, true },
        new Object[] { "Error", true, false, true, false },
        new Object[] { "Cancelled", true, true, false, false },
        new Object[] { "not Complete", false, false, false, false },
        new Object[] { null, false, false, false, false });
  }

  @Parameterized.Parameter
  public String input;
  @Parameterized.Parameter(1)
  public boolean isFinished;
  @Parameterized.Parameter(2)
  public boolean isCancelled;
  @Parameterized.Parameter(3)
  public boolean isFailed;
  @Parameterized.Parameter(4)
  public boolean isCompleted;

  @Test
  public void testIsFinished() {
    boolean result = OpenshiftHelper.isFinished(input);
    assertThat(result).isEqualTo(isFinished);
  }

  @Test
  public void testIsCancelled() {
    boolean result = OpenshiftHelper.isCancelled(input);
    assertThat(result).isEqualTo(isCancelled);
  }

  @Test
  public void testIsFailed() {
    boolean result = OpenshiftHelper.isFailed(input);
    assertThat(result).isEqualTo(isFailed);
  }

  @Test
  public void testIsCompleted() {
    boolean result = OpenshiftHelper.isCompleted(input);
    assertThat(result).isEqualTo(isCompleted);
  }
}
