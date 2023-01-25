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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jkube.kit.common.util.TemplateUtil.escapeYamlTemplate;

class TemplateUtilTest {

    public static Stream<Object[]> data() {
        return Stream.of(new Object[][]{
                {"abcd", "abcd"},
                {"abc{de}f}", "abc{de}f}"},
                {"abc{{de}f", "abc{{\"{{\"}}de}f"},
                {"abc{{de}f}}", "abc{{\"{{\"}}de}f{{\"}}\"}}"}
        });
    }

    @ParameterizedTest(name = "{0} is {1}")
    @MethodSource("data")
    void escapeYamlTemplateTest(String input, String expected) {
        assertThat(escapeYamlTemplate(input)).isEqualTo(expected);
    }
}