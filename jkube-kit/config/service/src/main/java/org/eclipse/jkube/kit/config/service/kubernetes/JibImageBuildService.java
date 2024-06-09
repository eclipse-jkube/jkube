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
package org.eclipse.jkube.kit.config.service.kubernetes;

import org.eclipse.jkube.kit.build.service.docker.auth.DockerAuthConfigFactory;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.config.image.build.JKubeBuildStrategy;
import org.eclipse.jkube.kit.config.service.AbstractImageBuildService;
import org.eclipse.jkube.kit.config.service.BuildServiceConfig;
import org.eclipse.jkube.kit.config.service.JKubeServiceException;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;
import org.eclipse.jkube.kit.service.jib.JibLogger;
import org.eclipse.jkube.kit.service.jib.JibService;

import java.io.File;
import java.util.Objects;

/**
 * AbstractImageBuildService implementation for JIB build strategy.
 * <p>
 * Relies on Jib to perform the build and push operations.
 */
public class JibImageBuildService extends AbstractImageBuildService {

    private final KitLogger kitLogger;
    private final JibLogger jibLogger;
    private final DockerAuthConfigFactory authConfigFactory;
    private final BuildServiceConfig buildServiceConfig;
    private final JKubeConfiguration configuration;

    public JibImageBuildService(JKubeServiceHub jKubeServiceHub) {
        this(jKubeServiceHub,
          new JibLogger(Objects.requireNonNull(jKubeServiceHub.getLog(), "Log is required")));
    }

    public JibImageBuildService(JKubeServiceHub jKubeServiceHub, JibLogger jibLogger) {
        super(jKubeServiceHub);
        this.jibLogger = jibLogger;
        this.kitLogger = jKubeServiceHub.getLog();
        authConfigFactory = new DockerAuthConfigFactory(kitLogger);
        buildServiceConfig = Objects.requireNonNull(jKubeServiceHub.getBuildServiceConfig(),
            "BuildServiceConfig is required");
        configuration = Objects.requireNonNull(jKubeServiceHub.getConfiguration(),
            "JKubeConfiguration is required");
    }

    @Override
    public boolean isApplicable() {
        return buildServiceConfig.getJKubeBuildStrategy() == JKubeBuildStrategy.jib;
    }

    @Override
    public void buildSingleImage(ImageConfiguration imageConfiguration) throws JKubeServiceException {
        if (imageConfiguration.getBuildConfiguration().isDockerFileMode()) {
            throw new JKubeServiceException("Dockerfile mode is not supported with JIB build strategy");
        }
        kitLogger.info("[[B]]JIB[[B]] image build started");
        try (JibService jibService = new JibService(jibLogger, authConfigFactory, configuration, imageConfiguration)) {
            for (final File dockerTarArchive : jibService.build()) {
                kitLogger.info(" %s successfully built", dockerTarArchive.getAbsolutePath());
            }
        } catch (Exception ex) {
            throw new JKubeServiceException("Error when building JIB image", ex);
        }
    }

    @Override
    protected void pushSingleImage(ImageConfiguration imageConfiguration, int retries, boolean skipTag) throws JKubeServiceException {
        try (JibService jibService = new JibService(jibLogger, authConfigFactory, configuration, imageConfiguration)) {
            kitLogger.info("Pushing image: %s", jibService.getImageName().getFullName());
            jibService.push();
        } catch (Exception ex) {
            throw new JKubeServiceException("Error when pushing JIB image", ex);
        }
    }

    @Override
    public void postProcess() {
        // No post-processing required
    }
}
