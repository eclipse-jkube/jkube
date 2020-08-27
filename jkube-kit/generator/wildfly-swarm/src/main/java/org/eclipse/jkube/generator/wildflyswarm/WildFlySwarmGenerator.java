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
package org.eclipse.jkube.generator.wildflyswarm;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.common.util.JKubeProjectUtil;
import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.generator.javaexec.JavaExecGenerator;

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
        return shouldAddGeneratedImageConfiguration(configs) && JKubeProjectUtil.hasPlugin(getProject(), "org.wildfly.swarm", "wildfly-swarm-plugin");
    }

    @Override
    protected Map<String, String> getEnv(boolean isPrepackagePhase) {
        Map<String, String> ret = super.getEnv(isPrepackagePhase);
        // Switch off Prometheus agent until logging issue with WildFly Swarm is resolved
        // See:
        // - https://github.com/fabric8io/fabric8-maven-plugin/issues/1173
        // - https://issues.jboss.org/browse/SWARM-1859
        ret.put("AB_PROMETHEUS_OFF", "true");
        ret.put("AB_OFF", "true");
        return ret;
    }

    @Override
    protected List<String> getExtraJavaOptions() {
        return Collections.singletonList("-Djava.net.preferIPv4Stack=true");
    }
}