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
package io.jkube.thorntail.v2.generator;

import io.jkube.generator.api.GeneratorContext;
import io.jkube.generator.javaexec.JavaExecGenerator;
import io.jkube.kit.build.service.docker.ImageConfiguration;
import io.jkube.kit.common.util.MavenUtil;
import org.apache.maven.plugin.MojoExecutionException;

import java.util.List;
import java.util.Map;

public class ThorntailV2Generator extends JavaExecGenerator {

    public ThorntailV2Generator(GeneratorContext context) {
        super(context, "thorntail-v2");
    }

    @Override
    public boolean isApplicable(List<ImageConfiguration> configs) {
        return shouldAddImageConfiguration(configs)
                && MavenUtil.hasPlugin(getProject(), "io.thorntail", "thorntail-maven-plugin")
                // if there's thorntail-kernel, it's Thorntail v4
                && !MavenUtil.hasDependency(getProject(), "io.thorntail", "thorntail-kernel");
    }

    @Override
    protected Map<String, String> getEnv(boolean isPrepackagePhase) throws MojoExecutionException {
        Map<String, String> ret = super.getEnv(isPrepackagePhase);
        // Switch off Prometheus agent until logging issue with WildFly Swarm is resolved
        // See:
        // - https://github.com/fabric8io/fabric8-maven-plugin/issues/1173
        // - https://issues.jboss.org/browse/THORN-1859
        ret.put("AB_PROMETHEUS_OFF", "true");
        return ret;
    }
}