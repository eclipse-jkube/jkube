/*
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
package org.eclipse.jkube.kit.enricher.api;

import java.util.Collections;
import java.util.Map;
import org.eclipse.jkube.kit.common.Configs;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author roland
 */
class EnricherConfigTest {

  private enum Config implements Configs.Config {
    TYPE
  }
  @Test
  void simple() {

    Map<String, Map<String, Object>> configMap = Collections.singletonMap("default.service",
        Collections.singletonMap("TYPE", "LoadBalancer"));
    EnricherContext context = JKubeEnricherContext.builder()
            .project(JavaProject.builder()
                    .groupId("org.eclipse.jkube")
                    .artifactId("test-project")
                    .version("0.0.1")
                    .build())
            .processorConfig(new ProcessorConfig(null, null, configMap))
            .build();
    EnricherConfig config = new EnricherConfig("default.service", context);
    assertThat(config.get(EnricherConfigTest.Config.TYPE)).isEqualTo("LoadBalancer");
  }
}
