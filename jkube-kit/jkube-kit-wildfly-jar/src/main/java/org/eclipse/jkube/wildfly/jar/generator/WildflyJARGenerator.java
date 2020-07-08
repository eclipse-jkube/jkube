/**
 * Copyright (c) 2020 Red Hat, Inc.
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
package org.eclipse.jkube.wildfly.jar.generator;

import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.generator.javaexec.JavaExecGenerator;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.common.util.JKubeProjectUtil;

import java.util.List;
import java.util.Map;
import org.eclipse.jkube.wildfly.jar.enricher.WildflyJARHealthCheckEnricher;

public class WildflyJARGenerator extends JavaExecGenerator {

    public WildflyJARGenerator(GeneratorContext context) {
        super(context, "wildfly-jar");
    }

    @Override
    public boolean isApplicable(List<ImageConfiguration> configs) {
        return shouldAddGeneratedImageConfiguration(configs)
                && JKubeProjectUtil.hasPlugin(getProject(),
                        WildflyJARHealthCheckEnricher.BOOTABLE_JAR_GROUP_ID, WildflyJARHealthCheckEnricher.BOOTABLE_JAR_ARTIFACT_ID);
    }

    @Override
    protected Map<String, String> getEnv(boolean isPrepackagePhase) {
        Map<String, String> ret = super.getEnv(isPrepackagePhase);
        // Switch off Prometheus agent until logging issue with WildFly Swarm is resolved
        // See:
        // - https://github.com/fabric8io/fabric8-maven-plugin/issues/1173
        // - https://issues.jboss.org/browse/THORN-1859
        ret.put("AB_PROMETHEUS_OFF", "true");
        ret.put("AB_OFF", "true");

        // In addition, there is no proper fix in Jolokia to detect that the Bootable JAR is started.
        // Can be workarounded by setting JAVA_OPTIONS to contain -Djboss.modules.system.pkgs=org.jboss.byteman
        ret.put("AB_JOLOKIA_OFF", "true");
        return ret;
    }
}
