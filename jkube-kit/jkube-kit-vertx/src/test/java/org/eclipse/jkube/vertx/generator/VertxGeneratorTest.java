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
package org.eclipse.jkube.vertx.generator;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eclipse.jkube.kit.common.JkubeProject;
import org.eclipse.jkube.kit.common.JkubeProjectDependency;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.generator.api.GeneratorContext;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Mocked;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class VertxGeneratorTest {

    @Injectable
    private KitLogger logger;

    private DefaultArtifactHandler handler = new DefaultArtifactHandler("jar");


    private final JkubeProjectDependency dropwizard = new JkubeProjectDependency("io.vertx", "vertx-dropwizard-metrics", "3.4.2");
    private final JkubeProjectDependency core = new JkubeProjectDependency("io.vertx", "vertx-core", "3.4.2");
    private final JkubeProjectDependency infinispan = new JkubeProjectDependency("io.vertx", "vertx-infinispan", "3.4.2");

    @Test
    public void testDefaultOptions(@Mocked final JkubeProject project) {
        new Expectations() {{
            project.getBuildDirectory(); result = new File("target/tmp").getAbsolutePath();
            project.getOutputDirectory(); result = new File("target/tmp/target").getAbsolutePath();
        }};

        GeneratorContext context = new GeneratorContext.Builder()
                .logger(logger)
                .project(project)
                .build();
        VertxGenerator generator = new VertxGenerator(context);
        List<String> list = generator.getExtraJavaOptions();

        assertThat(list).containsOnly("-Dvertx.cacheDirBase=/tmp", "-Dvertx.disableDnsResolver=true");
    }

    @Test
    public void testWithMetrics(@Mocked final JkubeProject project) {

        new Expectations() {{
            project.getBuildDirectory(); result = new File("target/tmp").getAbsolutePath();
            project.getOutputDirectory(); result = new File("target/tmp/target").getAbsolutePath();
            project.getDependencies(); result = Arrays.asList(dropwizard, core);
        }};

        GeneratorContext context = new GeneratorContext.Builder()
                .logger(logger)
                .project(project)
                .build();
        VertxGenerator generator = new VertxGenerator(context);
        List<String> list = generator.getExtraJavaOptions();

        assertThat(list).containsOnly(
                // Default entries
                "-Dvertx.cacheDirBase=/tmp", "-Dvertx.disableDnsResolver=true",
                // Metrics entries
                "-Dvertx.metrics.options.enabled=true", "-Dvertx.metrics.options.jmxEnabled=true", "-Dvertx.metrics.options.jmxDomain=vertx");
    }

    @Test
    public void testWithInfinispanClusterManager(@Mocked final JkubeProject project) throws MojoExecutionException {
        new Expectations() {{
            project.getBuildDirectory(); result = new File("target/tmp").getAbsolutePath();
            project.getOutputDirectory(); result = new File("target/tmp/target").getAbsolutePath();
            project.getDependencies(); result = Arrays.asList(infinispan, core);
            //project.getArtifacts(); result = ImmutableSet.of(infinispan, core);
        }};

        GeneratorContext context = new GeneratorContext.Builder()
                .logger(logger)
                .project(project)
                .build();
        VertxGenerator generator = new VertxGenerator(context);
        Map<String, String> env = generator.getEnv(true);

        assertThat(env).contains(entry("JAVA_OPTIONS", "-Dvertx.cacheDirBase=/tmp -Dvertx.disableDnsResolver=true " +
                // Force IPv4
                "-Djava.net.preferIPv4Stack=true"));
        assertThat(env).contains(entry("JAVA_ARGS", "-cluster"));
    }


}
