/*
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

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.generator.javaexec.JavaExecGenerator;
import org.eclipse.jkube.kit.common.Arguments;
import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;

import static org.eclipse.jkube.micronaut.MicronautUtils.extractPort;
import static org.eclipse.jkube.micronaut.MicronautUtils.getMicronautConfiguration;
import static org.eclipse.jkube.micronaut.MicronautUtils.hasMicronautPlugin;

public class MicronautGenerator extends JavaExecGenerator {

    private final MicronautNestedGenerator nestedGenerator;

    public MicronautGenerator(GeneratorContext context) {
        super(context, "micronaut");
        this.nestedGenerator = MicronautNestedGenerator.from(context, getGeneratorConfig());
    }

    @Override
    public boolean isApplicable(List<ImageConfiguration> configs) {
        return shouldAddGeneratedImageConfiguration(configs) && hasMicronautPlugin(getProject());
    }

    @Override
    protected Map<String, String> getEnv(boolean prePackagePhase) {
        return nestedGenerator.getEnv(ppp -> super.getEnv(ppp), prePackagePhase);
    }

    @Override
    protected String getDefaultJolokiaPort() {
        return nestedGenerator.getDefaultJolokiaPort();
    }

    @Override
    protected String getDefaultPrometheusPort() {
        return nestedGenerator.getDefaultPrometheusPort();
    }

    @Override
    protected String getBuildWorkdir() {
        return nestedGenerator.getBuildWorkdir();
    }

    @Override
    protected String getFromAsConfigured() {
        return Optional.ofNullable(super.getFromAsConfigured()).orElse(nestedGenerator.getFrom());
    }

    @Override
    protected Arguments getBuildEntryPoint() {
        return nestedGenerator.getBuildEntryPoint();
    }

    @Override
    protected AssemblyConfiguration createAssembly() {
        return Optional.ofNullable(nestedGenerator.createAssemblyConfiguration(addAdditionalFiles()))
          .orElse(super.createAssembly());
    }

    @Override
    protected String getDefaultWebPort() {
        return extractPort(
            getMicronautConfiguration(getProject()), super.getDefaultWebPort()
        );
    }
}
