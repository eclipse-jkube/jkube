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

import java.util.Collections;
import java.util.Map;

import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.Plugin;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.image.build.OpenShiftBuildStrategy;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import mockit.Expectations;
import mockit.Mocked;
import org.junit.Test;

import static org.eclipse.jkube.kit.config.image.build.OpenShiftBuildStrategy.s2i;
import static org.eclipse.jkube.kit.config.image.build.OpenShiftBuildStrategy.docker;
import static org.eclipse.jkube.kit.config.resource.RuntimeMode.openshift;
import static org.junit.Assert.assertEquals;

/**
 * @author roland
 * @since 12/08/16
 */
public class FromSelectorTest {

    @Mocked
    JavaProject project;

    @Mocked
    Plugin plugin;

    @Mocked
    KitLogger logger;

    @Test
    public void simple() {
        final Object[] data = new Object[] {
                openshift, s2i, "1.2.3.redhat-00009", "redhat-s2i-prop", "redhat-istag-prop",
                openshift, docker, "1.2.3.redhat-00009", "redhat-docker-prop", "redhat-istag-prop",
                openshift, s2i, "1.2.3.fuse-00009", "redhat-s2i-prop", "redhat-istag-prop",
                openshift, docker, "1.2.3.fuse-00009", "redhat-docker-prop", "redhat-istag-prop",
                openshift, s2i, "1.2.3.foo-00009", "s2i-prop", "istag-prop",
                openshift, docker, "1.2.3.foo-00009", "docker-prop", "istag-prop",
                openshift, s2i, "1.2.3", "s2i-prop", "istag-prop",
                openshift, docker, "1.2.3", "docker-prop", "istag-prop",
                null, s2i, "1.2.3.redhat-00009", "redhat-docker-prop", "redhat-istag-prop",
                null, docker, "1.2.3.redhat-00009", "redhat-docker-prop", "redhat-istag-prop",
                null, s2i, "1.2.3.fuse-00009", "redhat-docker-prop", "redhat-istag-prop",
                null, docker, "1.2.3.fuse-00009", "redhat-docker-prop", "redhat-istag-prop",
                null, s2i, "1.2.3.foo-00009", "docker-prop", "istag-prop",
                null, docker, "1.2.3.foo-00009", "docker-prop", "istag-prop",
                null, s2i, "1.2.3", "docker-prop", "istag-prop",
                null, docker, "1.2.3", "docker-prop", "istag-prop",
                openshift, null, "1.2.3.redhat-00009", "redhat-docker-prop", "redhat-istag-prop",
                openshift, null, "1.2.3.fuse-00009", "redhat-docker-prop", "redhat-istag-prop",
                openshift, null, "1.2.3.foo-00009", "docker-prop", "istag-prop",
                openshift, null, "1.2.3", "docker-prop", "istag-prop"
        };

        for (int i = 0; i < data.length; i += 5) {
            GeneratorContext ctx = new GeneratorContext.Builder()
                    .project(project)
                    .config(new ProcessorConfig())
                    .logger(logger)
                    .runtimeMode((RuntimeMode) data[i])
                    .strategy((OpenShiftBuildStrategy) data[i + 1])
                    .build();

            final String version = (String) data[i + 2];
            new Expectations() {{
                project.getPlugins(); result = Collections.singletonList(plugin);
                plugin.getGroupId(); result = "org.eclipse.jkube";
                plugin.getArtifactId(); result = "openshift-maven-plugin";
                plugin.getVersion(); result = version;
            }};

            FromSelector selector = new FromSelector.Default(ctx, "test");
            assertEquals(data[i + 3], selector.getFrom());
            Map<String, String> fromExt = selector.getImageStreamTagFromExt();
            assertEquals(fromExt.size(),3);
            assertEquals(fromExt.get(OpenShiftBuildStrategy.SourceStrategy.kind.key()), "ImageStreamTag");
            assertEquals(fromExt.get(OpenShiftBuildStrategy.SourceStrategy.namespace.key()), "openshift");
            assertEquals(fromExt.get(OpenShiftBuildStrategy.SourceStrategy.name.key()), data[i + 4]);
        }
    }

}
