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
package io.jshift.generator.wildflyswarm;

import java.util.List;
import java.util.Map;

import io.jshift.kit.build.service.docker.ImageConfiguration;
import io.jshift.kit.common.util.MavenUtil;
import io.jshift.generator.api.GeneratorContext;
import io.jshift.generator.javaexec.JavaExecGenerator;
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