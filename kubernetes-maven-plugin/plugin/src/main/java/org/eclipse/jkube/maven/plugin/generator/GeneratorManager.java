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
package org.eclipse.jkube.maven.plugin.generator;

import org.eclipse.jkube.generator.api.Generator;
import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.ClassUtil;
import org.eclipse.jkube.kit.common.util.PluginServiceFactory;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;

import java.util.List;

/**
 * Manager responsible for finding and calling generators
 * @author roland
 */
public class GeneratorManager {

    private GeneratorManager() {}

    public static List<ImageConfiguration> generate(List<ImageConfiguration> imageConfigs,
                                                    GeneratorContext genCtx,
                                                    boolean prePackagePhase) {

        List<ImageConfiguration> ret = imageConfigs;

        PluginServiceFactory<GeneratorContext> pluginFactory =
            genCtx.isUseProjectClasspath() ?
                new PluginServiceFactory<>(genCtx, ClassUtil.createProjectClassLoader(genCtx.getProject().getCompileClassPathElements(), genCtx.getLogger())) :
                new PluginServiceFactory<>(genCtx);

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
