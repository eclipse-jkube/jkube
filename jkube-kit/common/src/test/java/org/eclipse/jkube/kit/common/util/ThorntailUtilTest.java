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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

class ThorntailUtilTest {

  @Test
  void testReadThorntailPort() {
    Properties props = YamlUtil.getPropertiesFromYamlResource(ThorntailUtilTest.class.getResource("/util/project-default.yml"));
    assertThat(props).isNotNull()
            .containsOnly(entry("thorntail.http.port", "8082"));
  }

}
