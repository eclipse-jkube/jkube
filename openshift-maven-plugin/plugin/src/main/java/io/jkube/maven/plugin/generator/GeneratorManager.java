/**
 * Copyright 2016 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
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
