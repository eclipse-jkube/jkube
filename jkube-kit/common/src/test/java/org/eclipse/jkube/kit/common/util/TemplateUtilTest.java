/**
 * Copyright (c) 2019 Red Hat, Inc.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at:
 * <p>
 * https://www.eclipse.org/legal/epl-2.0/
 * <p>
 * SPDX-License-Identifier: EPL-2.0
 * <p>
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.jkube.kit.common.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jkube.kit.common.util.TemplateUtil.escapeYamlTemplate;

@RunWith(Parameterized.class)
public class TemplateUtilTest {

    @Parameterized.Parameters(name = "{0} is {1}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"abcd", "abcd"},
                {"abc{de}f}", "abc{de}f}"},
                {"abc{{de}f", "abc{{\"{{\"}}de}f"},
                {"abc{{de}f}}", "abc{{\"{{\"}}de}f{{\"}}\"}}"}
        });
    }

    private final String input;

    private final String expected;

    public TemplateUtilTest(String input, String expected) {
        this.input = input;
        this.expected = expected;
    }

    @Test
    public void escapeYamlTemplateTest() {
        assertThat(escapeYamlTemplate(input)).isEqualTo(expected);
    }
}