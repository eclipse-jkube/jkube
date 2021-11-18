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
package org.eclipse.jkube.generator.api;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.image.build.JKubeBuildStrategy;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;

import java.util.Optional;

/**
 * @author roland
 */
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
@Getter
@EqualsAndHashCode
public class GeneratorContext {
    private JavaProject project;
    private ProcessorConfig config;
    private KitLogger logger;
    private RuntimeMode runtimeMode;
    private JKubeBuildStrategy strategy;

    private boolean useProjectClasspath;
    private boolean prePackagePhase;

    private GeneratorMode generatorMode;


    public GeneratorMode getGeneratorMode() {
        return Optional.ofNullable(generatorMode).orElse(GeneratorMode.BUILD);
    }
}
