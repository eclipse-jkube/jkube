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
package org.eclipse.jkube.kit.config.image.build;

import org.junit.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

public class ArgumentsTest {

    @Test
    public void testShellArgWithSpaceEscape() {
      String[] testSubject = { "java", "-jar", "$HOME/name with space.jar" };
      Arguments arg = Arguments.builder().shell("java -jar $HOME/name\\ with\\ space.jar").build();
      assertThat(arg.asStrings()).containsExactly(testSubject);
    }
}
