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
package org.eclipse.jkube.kit.enricher.api;

import mockit.Mocked;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class JKubeEnricherContextTest {
    @Mocked
    private JavaProject javaProject;

    @Mocked
    private ProcessorConfig processorConfig;

    @Mocked
    private ResourceConfig resourceConfig;

    @Mocked
    private KitLogger log;

    @Test
    public void testGetEnricherContext() {
        // Given
        List<ImageConfiguration> imageConfigurationList = new ArrayList<>();
        imageConfigurationList.add(ImageConfiguration.builder()
                .name("test")
                .build(BuildConfiguration.builder()
                        .from("test-from:0.0.1")
                        .build())
                .build());

        // When
        JKubeEnricherContext jKubeEnricherContext = JKubeEnricherContext.getEnricherContext(javaProject, processorConfig, imageConfigurationList, resourceConfig, log).build();

        // Then
        assertNotNull(jKubeEnricherContext);
        assertEquals(1, jKubeEnricherContext.getConfiguration().getImages().size());
    }
}
