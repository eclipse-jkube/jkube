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

import java.util.Properties;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.eclipse.jkube.kit.common.util.PropertiesUtil.getPropertiesFromResource;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class PropertiesUtilTest {

  @Test
  public void testPropertiesParsing() {
    // When
    Properties result = getPropertiesFromResource(PropertiesUtilTest.class.getResource("/util/test-application.properties"));
    // Then
    assertThat(result).containsOnly(
        entry("management.port", "8081"),
        entry("spring.datasource.url", "jdbc:mysql://127.0.0.1:3306"),
        entry("example.nested.items[0].name", "item0"),
        entry("example.nested.items[0].value", "value0"),
        entry("example.nested.items[1].name", "item1"),
        entry("example.nested.items[1].value", "value1")
    );
  }

  @Test
  public void testNonExistentPropertiesParsing() {
    // When
    Properties result = getPropertiesFromResource(PropertiesUtilTest.class.getResource("/this-file-does-not-exist"));
    // Then
    assertThat(result).isEmpty();
  }
}
