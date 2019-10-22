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
package io.jkube.maven.plugin.generator;

import io.jkube.generator.api.Generator;
import io.jkube.generator.api.GeneratorContext;
import io.jkube.kit.build.service.docker.ImageConfiguration;
import io.jkube.kit.common.KitLogger;
import io.jkube.kit.common.util.ClassUtil;
import io.jkube.kit.common.util.PluginServiceFactory;
import io.jkube.kit.config.resource.ProcessorConfig;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;

import java.util.List;

/**
 * Manager responsible for finding and calling generators
 * @author roland
 * @since 15/05/16
 */
public class GeneratorManager {

    public static List<ImageConfiguration> generate(List<ImageConfiguration> imageConfigs,
                                                    GeneratorContext genCtx,
                                                    boolean prePackagePhase) throws MojoExecutionException {

        List<ImageConfiguration> ret = imageConfigs;

        PluginServiceFactory<GeneratorContext> pluginFactory =
            null;
        try {
            pluginFactory = genCtx.isUseProjectClasspath() ?
            new PluginServiceFactory<GeneratorContext>(genCtx, ClassUtil.createProjectClassLoader(genCtx.getProject().getCompileClasspathElements(), genCtx.getLogger())) :
            new PluginServiceFactory<GeneratorContext>(genCtx);
        } catch (DependencyResolutionRequiredException e) {
        }

        List<Generator> generators =
            pluginFactory.createServiceObjects("META-INF/jkube/generator-default",
                                               "META-INF/jkube/jkube-generator-default",
                                               "META-INF/jkube/generator",
                                               "META-INF/jkube-generator");
        ProcessorConfig config = genCtx.getConfig();
        KitLogger log = genCtx.getLogger();
        List<Generator> usableGenerators = config.prepareProcessors(generators, "generator");
        log.verbose("Generators:");
        for (Generator generator : usableGenerators) {
            log.verbose(" - %s",generator.getName());
            if (generator.isApplicable(ret)) {
                log.info("Running generator %s", generator.getName());
                ret = generator.customize(ret, prePackagePhase);
            }
        }
        return ret;
    }
}
