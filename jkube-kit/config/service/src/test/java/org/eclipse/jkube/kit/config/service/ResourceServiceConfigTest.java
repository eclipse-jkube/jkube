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
package org.eclipse.jkube.kit.config.service;

import mockit.Mock;
import mockit.Mocked;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.ResourceFileType;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ResourceServiceConfigTest {
    @Mocked
    private JavaProject javaProject;

    @Mocked
    private ResourceConfig resourceConfig;

    @Test
    public void testGetResourceServiceConfig() {
        // Given
        File resourceDir = new File("src/main/jkube");
        File targetDir = new File("target");
        ResourceFileType resourceFileType = ResourceFileType.yaml;
        ResourceService.ResourceFileProcessor resourceFileProcessor = resources -> resources;

        // When
        ResourceServiceConfig resourceServiceConfig = ResourceServiceConfig.getResourceServiceConfig(javaProject, resourceDir, targetDir, resourceFileType, resourceConfig, resourceFileProcessor, false);

        // Then
        assertNotNull(resourceServiceConfig);
        assertEquals("src/main/jkube", resourceServiceConfig.getResourceDir().getPath());
        assertEquals("target", resourceServiceConfig.getTargetDir().getPath());
        assertEquals(ResourceFileType.yaml, resourceServiceConfig.getResourceFileType());
    }
}
