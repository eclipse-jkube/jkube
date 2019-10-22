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
package io.jkube.generator.wildflyswarm;

import java.util.List;
import java.util.Map;

import io.jkube.kit.build.service.docker.ImageConfiguration;
import io.jkube.kit.common.util.MavenUtil;
import io.jkube.generator.api.GeneratorContext;
import io.jkube.generator.javaexec.JavaExecGenerator;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * Created by ceposta
 * <a href="http://christianposta.com/blog">http://christianposta.com/blog</a>.
 */
public class WildFlySwarmGenerator extends JavaExecGenerator {

    public WildFlySwarmGenerator(GeneratorContext context) {
        super(context, "wildfly-swarm");
    }

    @Override
    public boolean isApplicable(List<ImageConfiguration> configs) {
        return shouldAddImageConfiguration(configs) && MavenUtil.hasPlugin(getProject(), "org.wildfly.swarm", "wildfly-swarm-plugin");
    }

    @Override
    protected Map<String, String> getEnv(boolean isPrepackagePhase) throws MojoExecutionException {
        Map<String, String> ret = super.getEnv(isPrepackagePhase);
        // Switch off Prometheus agent until logging issue with WildFly Swarm is resolved
        // See:
        // - https://github.com/fabric8io/fabric8-maven-plugin/issues/1173
        // - https://issues.jboss.org/browse/SWARM-1859
        ret.put("AB_PROMETHEUS_OFF", "true");
        return ret;
    }
}