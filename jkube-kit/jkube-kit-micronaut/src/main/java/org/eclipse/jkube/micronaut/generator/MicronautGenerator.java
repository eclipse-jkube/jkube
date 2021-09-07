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
package org.eclipse.jkube.micronaut.generator;

import static org.eclipse.jkube.kit.common.util.JKubeProjectUtil.getClassLoader;
import static org.eclipse.jkube.micronaut.MicronautUtils.extractPort;
import static org.eclipse.jkube.micronaut.MicronautUtils.getMicronautConfiguration;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.generator.javaexec.JavaExecGenerator;
import org.eclipse.jkube.kit.common.util.JKubeProjectUtil;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;

public class MicronautGenerator extends JavaExecGenerator {

    public MicronautGenerator(GeneratorContext context) {
        super(context, "micronaut");
    }

    @Override
    public boolean isApplicable(List<ImageConfiguration> configs) {
        return shouldAddGeneratedImageConfiguration(configs)
                && JKubeProjectUtil.hasPlugin(getProject(), "io.micronaut.build", "micronaut-maven-plugin");
    }

    @Override
    protected List<String> extractPorts() {
        final List<String> ports = new ArrayList<>();
        addPortIfValid(ports, getConfig(Config.WEB_PORT, extractPort(
            getMicronautConfiguration(getClassLoader(getProject())), null
        )));
        addPortIfValid(ports, getConfig(Config.JOLOKIA_PORT));
        addPortIfValid(ports, getConfig(Config.PROMETHEUS_PORT));
        return ports;
    }
}
