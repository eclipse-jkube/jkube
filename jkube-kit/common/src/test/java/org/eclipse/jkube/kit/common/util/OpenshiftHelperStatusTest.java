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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class OpenshiftHelperStatusTest {
    @Parameterized.Parameters(name = "{0}: isFinished({1})={2}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { "IsCompleteTrue", "Complete", true },
                { "IsErrorTrue", "Error", true },
                { "IsCancelledTrue", "Cancelled", true },
                { "IsNotCompleteFalse", "not Complete", false }
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
        assertThat(OpenshiftHelper.isFinished(input)).isEqualTo(expected);
    }

}
