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

import static org.eclipse.jkube.kit.common.util.TemplateUtil.escapeYamlTemplate;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class TemplateUtilTest {

  @Test
  public void escapeYamlTemplateTest() {
    assertThat(escapeYamlTemplate("abcd"), is("abcd"));
    assertThat(escapeYamlTemplate("abc{de}f}"), is("abc{de}f}"));
    assertThat(escapeYamlTemplate("abc{{de}f"), is("abc{{\"{{\"}}de}f"));
    assertThat(escapeYamlTemplate("abc{{de}f}}"), is("abc{{\"{{\"}}de}f{{\"}}\"}}"));
  }
}