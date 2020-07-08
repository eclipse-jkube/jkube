/**
 * Copyright (c) 2020 Red Hat, Inc.
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
package org.eclipse.jkube.wildfly.jar.generator;

import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import mockit.Expectations;
import mockit.Mocked;
import org.eclipse.jkube.kit.common.JavaProject;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * @author roland
 */
public class WildflyJARGeneratorTest {

    @Mocked
    private GeneratorContext context;

    @Mocked
    private JavaProject project;

    @Test
    public void notApplicable() throws IOException {
        WildflyJARGenerator generator = new WildflyJARGenerator(createGeneratorContext());
        assertFalse(generator.isApplicable((List<ImageConfiguration>) Collections.EMPTY_LIST));
    }

    // To be revisited if we enable jolokia and prometheus.
    @Test
    public void getEnv() throws IOException {
        WildflyJARGenerator generator = new WildflyJARGenerator(createGeneratorContext());
        Map<String, String> extraEnv = generator.getEnv(true);
        assertNotNull(extraEnv);
        assertEquals(3, extraEnv.size());
    }

    private GeneratorContext createGeneratorContext() throws IOException {
        new Expectations() {{
            context.getProject(); result = project;
            String tempDir = Files.createTempDirectory("wildfly-jar-test-project").toFile().getAbsolutePath();
            project.getBuildDirectory(); result = tempDir;
            project.getOutputDirectory(); result = tempDir;
            project.getPlugins(); result = Collections.EMPTY_LIST; minTimes = 0;
            project.getVersion(); result = "1.0.0"; minTimes = 0;
        }};
        return context;
    }
}
