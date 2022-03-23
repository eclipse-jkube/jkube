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
package org.eclipse.jkube.kit.common;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class ConfigsAsBooleanTest {

  @Parameterized.Parameters(name = "{0}: asBoolean({1})={2}")
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {
        { "Unsupported String", " 1 2 1337", false },
        { "One", "1", false },
        { "Zero", "0", false },
        { "FalseMixedCase", "fALsE", false },
        { "False", "false", false },
        { "True", "true", true },
        { "TrueMixedCase", "TrUE", true },
        { "TrueUpperCase", "TRUE", true }
    });
  }

  @Parameterized.Parameter
  public String description;
  @Parameterized.Parameter(1)
  public String input;
  @Parameterized.Parameter(2)
  public Boolean expected;

  @Test
  public void testUsingParametrizedTest() {
    assertThat(Configs.asBoolean(input)).isEqualTo(expected);
  }
}