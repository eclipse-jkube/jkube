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
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Enclosed.class)
public class OpenshiftHelperStatusTest {
    @RunWith(Parameterized.class)
    public static class Finished {
        @Parameterized.Parameters(name = "{0}: isFinished({1})={2}")
        public static Collection<Object[]> data() {
            return Arrays.asList(new Object[][]{
                    {"IsCompleteTrue", "Complete", true},
                    {"IsErrorTrue", "Error", true},
                    {"IsCancelledTrue", "Cancelled", true},
                    {"IsNotCompleteFalse", "not Complete", false}
            });
        }

        @Parameterized.Parameter
        public String description;
        @Parameterized.Parameter(1)
        public String input;
        @Parameterized.Parameter(2)
        public Boolean expected;

        @Test
        public void testIsFinishedUsingParametrizedTest() {
            assertThat(OpenshiftHelper.isFinished(input)).isEqualTo(expected);
        }
    }
    @RunWith(Parameterized.class)
    public static class Cancelled {
        @Parameterized.Parameters(name = "{0}: isCancelled({1})={2}")
        public static Collection<Object[]> data() {
            return Arrays.asList(new Object[][]{
                    {"IsCancelledTrue", "Cancelled", true},
                    {"IsNotCancelledFalse", "not Cancelled", false}
            });
        }

        @Parameterized.Parameter
        public String description;
        @Parameterized.Parameter(1)
        public String input;
        @Parameterized.Parameter(2)
        public Boolean expected;

        @Test
        public void testIsCancelledUsingParametrizedTest() {
            assertThat(OpenshiftHelper.isCancelled(input)).isEqualTo(expected);
        }
    }
    @RunWith(Parameterized.class)
    public static class Failed {
        @Parameterized.Parameters(name = "{0}: isFailed({1})={2}")
        public static Collection<Object[]> data() {
            return Arrays.asList(new Object[][]{
                    {"IsFailTrue", "Fail", true},
                    {"IsErrorTrue", "Error", true},
                    {"IsNullFalse","null",false}
            });
        }

        @Parameterized.Parameter
        public String description;
        @Parameterized.Parameter(1)
        public String input;
        @Parameterized.Parameter(2)
        public Boolean expected;

        @Test
        public void testIsFailedUsingParametrizedTest() {
            assertThat(OpenshiftHelper.isFailed(input)).isEqualTo(expected);
        }
    }
    @RunWith(Parameterized.class)
    public static class Completed {
        @Parameterized.Parameters(name = "{0}: isCompleted({1})={2}")
        public static Collection<Object[]> data() {
            return Arrays.asList(new Object[][]{
                    {"IsCompleteTue", "Complete", true},
                    {"IsNotCompleteFalse", "not Complete", false}
            });
        }

        @Parameterized.Parameter
        public String description;
        @Parameterized.Parameter(1)
        public String input;
        @Parameterized.Parameter(2)
        public Boolean expected;

        @Test
        public void testIsCompletedUsingParametrizedTest() {
            assertThat(OpenshiftHelper.isCompleted(input)).isEqualTo(expected);
        }
    }


}
