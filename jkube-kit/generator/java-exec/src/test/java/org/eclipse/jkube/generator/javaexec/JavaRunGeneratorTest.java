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
package org.eclipse.jkube.generator.javaexec;

import java.io.IOException;
import java.net.URL;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;

import org.eclipse.jkube.kit.common.JKubeProject;
import org.eclipse.jkube.kit.common.JKubeProjectPlugin;
import org.eclipse.jkube.kit.config.image.build.OpenShiftBuildStrategy;
import org.eclipse.jkube.generator.api.FromSelector;
import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import mockit.Expectations;
import mockit.Mocked;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author roland
 * @since 22/09/16
 */
public class JavaRunGeneratorTest {

    @Mocked
    GeneratorContext ctx;

    @Mocked
    JKubeProject project;

    @Mocked
    JKubeProjectPlugin plugin;

    @Test
    @Ignore // TODO: Fix this test
    public void fromSelector() throws IOException {
        Object[] data = {
            "3.1.123", RuntimeMode.kubernetes, null, "java.upstream.docker",
            "3.1.redhat-101", RuntimeMode.kubernetes, null, "java.redhat.docker",
            "3.1.123", RuntimeMode.openshift, OpenShiftBuildStrategy.docker, "java.upstream.docker",
            "3.1.redhat-101", RuntimeMode.openshift, OpenShiftBuildStrategy.docker, "java.redhat.docker",
            "3.1.123", RuntimeMode.openshift, OpenShiftBuildStrategy.s2i, "java.upstream.s2i",
            "3.1.redhat-101", RuntimeMode.openshift, OpenShiftBuildStrategy.s2i, "java.redhat.s2i",
        };

        Properties imageProps = getDefaultImageProps();

        for (int i = 0; i < data.length; i += 4) {
            prepareExpectation((String) data[i], (RuntimeMode) data[i+1], (OpenShiftBuildStrategy) data[i+2]);
            final GeneratorContext context = ctx;
            FromSelector selector = new FromSelector.Default(context, "java");
            String from = selector.getFrom();
            assertEquals(imageProps.getProperty((String) data[i+3]), from);
        }
    }

    private Expectations prepareExpectation(final String version, final RuntimeMode mode, final OpenShiftBuildStrategy strategy) {
        return new Expectations() {{
            ctx.getProject(); result = new JKubeProject.Builder()
              .plugins(Arrays.asList(new JKubeProjectPlugin.Builder().groupId("org.eclipse.jkube").artifactId("jkube-kit-parent").version(version).configuration(Collections.emptyMap()).build()))
              .build();

            ctx.getRuntimeMode();result = mode;
            ctx.getStrategy(); result = strategy;
        }};
    }

    private Properties getDefaultImageProps() throws IOException {
        Properties props = new Properties();
        Enumeration<URL> resources = getClass().getClassLoader().getResources("META-INF/jkube/default-images.properties");
        while (resources.hasMoreElements()) {
            props.load(resources.nextElement().openStream());
        }
        return props;
    }
}
