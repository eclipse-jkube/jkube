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
package org.eclipse.jkube.generator.api;

import mockit.Mocked;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.service.ArtifactResolverService;
import org.eclipse.jkube.kit.config.image.build.JKubeBuildStrategy;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class GeneratorContextTest {
    @Mocked
    private ProcessorConfig generatorConfig;

    @Mocked
    private JavaProject javaProject;

    @Mocked
    private KitLogger log;

    @Test
    public void testGeneratorContextBuilder() {
        // Given
        RuntimeMode runtimeMode = RuntimeMode.KUBERNETES;
        boolean shouldUseProjectClassPath = false;
        ArtifactResolverService artifactResolverService = (groupId, artifactId, version, type) -> null;
        JKubeBuildStrategy jKubeBuildStrategy = JKubeBuildStrategy.docker;

        // When
        GeneratorContext generatorContext = GeneratorContext.generatorContextBuilder(generatorConfig, javaProject, log, runtimeMode, shouldUseProjectClassPath, artifactResolverService, jKubeBuildStrategy).build();

        // Then
        assertNotNull(generatorContext);
        assertEquals(runtimeMode, generatorContext.getRuntimeMode());
        assertEquals(jKubeBuildStrategy, generatorContext.getStrategy());
        assertFalse(generatorContext.isUseProjectClasspath());
    }
}
