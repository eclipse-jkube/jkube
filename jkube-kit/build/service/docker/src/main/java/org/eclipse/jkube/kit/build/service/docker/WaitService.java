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
package org.eclipse.jkube.kit.build.service.docker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.eclipse.jkube.kit.build.api.model.Container;
import org.eclipse.jkube.kit.build.service.docker.access.DockerAccess;
import org.eclipse.jkube.kit.build.service.docker.access.DockerAccessException;
import org.eclipse.jkube.kit.build.service.docker.access.log.DefaultLogCallback;
import org.eclipse.jkube.kit.build.service.docker.access.log.LogDispatcher;
import org.eclipse.jkube.kit.build.service.docker.access.log.LogOutputSpec;
import org.eclipse.jkube.kit.build.service.docker.wait.ExitCodeChecker;
import org.eclipse.jkube.kit.build.service.docker.wait.HealthCheckChecker;
import org.eclipse.jkube.kit.build.service.docker.wait.HttpPingChecker;
import org.eclipse.jkube.kit.build.service.docker.wait.LogWaitChecker;
import org.eclipse.jkube.kit.build.service.docker.wait.PreconditionFailedException;
import org.eclipse.jkube.kit.build.service.docker.wait.TcpPortChecker;
import org.eclipse.jkube.kit.build.service.docker.wait.WaitChecker;
import org.eclipse.jkube.kit.build.service.docker.wait.WaitTimeoutException;
import org.eclipse.jkube.kit.build.service.docker.wait.WaitUtil;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.WaitConfiguration;

import org.apache.commons.text.StringSubstitutor;

/**
 * @author roland
 */
public class WaitService {

    private final QueryService queryService;
    private DockerAccess dockerAccess;
    private KitLogger log;

    public WaitService(DockerAccess dockerAccess, QueryService queryService, KitLogger log) {
        this.dockerAccess = dockerAccess;
        this.log = log;
        this.queryService = queryService;
    }

    // ========================================================================================================

    public void wait(ImageConfiguration imageConfig, Properties projectProperties, String containerId) throws IOException {
        List<WaitChecker> checkers = prepareWaitCheckers(imageConfig, projectProperties, containerId);
        int timeout = getTimeOut(imageConfig);

        if (checkers.isEmpty()) {
            if (timeout > 0) {
                log.info("%s: Pausing for %d ms", imageConfig.getDescription(), timeout);
                WaitUtil.sleep(timeout);
            }
            return;
        }

        String logLine = extractCheckerLog(checkers);
        ContainerRunningPrecondition precondition = new ContainerRunningPrecondition(dockerAccess, containerId);
        try {
            long waited = WaitUtil.wait(precondition, timeout, checkers);
            log.info("%s: Waited %s %d ms", imageConfig.getDescription(), logLine, waited);
        } catch (WaitTimeoutException exp) {
            String desc = String.format("%s: Timeout after %d ms while waiting %s",
                                        imageConfig.getDescription(), exp.getWaited(),
                                        logLine);
            log.error(desc);
            throw new IOException(desc);
        } catch (PreconditionFailedException exp) {
            String desc = String.format("%s: Container stopped with exit code %d unexpectedly after %d ms while waiting %s",
                                        imageConfig.getDescription(), precondition.getExitCode(), exp.getWaited(),
                                        logLine);
            log.error(desc);
            throw new IOException(desc);
        }
    }

    private int getTimeOut(ImageConfiguration imageConfig) {
        WaitConfiguration wait = getWaitConfiguration(imageConfig);
        return wait != null && wait.getTime() != null ? wait.getTime() : 0;
    }

    private String extractCheckerLog(List<WaitChecker> checkers) {
        List<String> logOut = new ArrayList<>();
        for (WaitChecker checker : checkers) {
            logOut.add(checker.getLogLabel());
        }
        return String.join(" and ", logOut);
    }

    private List<WaitChecker> prepareWaitCheckers(ImageConfiguration imageConfig, Properties projectProperties, String containerId) throws IOException {
        WaitConfiguration wait = getWaitConfiguration(imageConfig);

        if (wait == null) {
            return Collections.emptyList();
        }

        List<WaitChecker> checkers = new ArrayList<>();

        if (wait.getUrl() != null) {
            checkers.add(getUrlWaitChecker(imageConfig.getDescription(), projectProperties, wait));
        }

        if (wait.getLog() != null) {
            log.debug("LogWaitChecker: Waiting on %s", wait.getLog());
            checkers.add(new LogWaitChecker(wait.getLog(), dockerAccess, containerId, log));
        }

        if (wait.getTcp() != null) {
            try {
                Container container = queryService.getMandatoryContainer(containerId);
                checkers.add(getTcpWaitChecker(container, imageConfig.getDescription(), projectProperties, wait.getTcp()));
            } catch (DockerAccessException e) {
                throw new IOException("Unable to access container " + containerId, e);
            }
        }

        if (wait.getHealthy() == Boolean.TRUE) {
            checkers.add(new HealthCheckChecker(dockerAccess, containerId, imageConfig.getDescription(), log));
        }

        if (wait.getExit() != null) {
            checkers.add(new ExitCodeChecker(wait.getExit(), queryService, containerId));
        }
        return checkers;
    }

    private WaitConfiguration getWaitConfiguration(ImageConfiguration imageConfig) {
        return imageConfig.getRunConfiguration().getWait();
    }

    // =================================================================================================================

    private WaitChecker getUrlWaitChecker(String imageConfigDesc,
                                          Properties projectProperties,
                                          WaitConfiguration wait) {
        String waitUrl = StringSubstitutor.replace(wait.getUrl(), projectProperties);
        WaitConfiguration.HttpConfiguration httpConfig = wait.getHttp();
        HttpPingChecker checker;
        if (httpConfig != null) {
            checker = new HttpPingChecker(waitUrl, httpConfig.getMethod(), httpConfig.getStatus(), httpConfig.isAllowAllHosts());
            log.info("%s: Waiting on url %s with method %s for status %s.",
                     imageConfigDesc, waitUrl, httpConfig.getMethod(), httpConfig.getStatus());
        } else {
            checker = new HttpPingChecker(waitUrl);
            log.info("%s: Waiting on url %s.", imageConfigDesc, waitUrl);
        }
        return checker;
    }

    private WaitChecker getTcpWaitChecker(Container container,
                                          String imageConfigDesc,
                                          Properties projectProperties,
                                          WaitConfiguration.TcpConfiguration tcpConfig) {
        List<Integer> ports = new ArrayList<>();

        List<Integer> portsConfigured = getTcpPorts(tcpConfig);
        String host = getTcpHost(tcpConfig, projectProperties);
        WaitConfiguration.TcpConfigMode mode = getTcpMode(tcpConfig, host);

        if (mode == WaitConfiguration.TcpConfigMode.mapped) {
            for (int port : portsConfigured) {
                Container.PortBinding binding = container.getPortBindings().get(port + "/tcp");
                if (binding == null) {
                    throw new IllegalArgumentException(
                        String.format("Cannot watch on port %d, since there is no network binding", port));
                }
                ports.add(binding.getHostPort());
            }
            log.info("%s: Waiting for mapped ports %s on host %s", imageConfigDesc, ports, host);
        } else {
            final String networkMode = container.getNetworkMode();
            log.info("%s: Network mode: %s", imageConfigDesc, networkMode);
            if (networkMode == null || networkMode.isEmpty() || "bridge".equals(networkMode)) {
                // Safe mode when network mode is not present
                host = container.getIPAddress();
            } else if (!"host".equals(networkMode)) {
                // Custom network
                host = container.getCustomNetworkIpAddresses().get(networkMode);
            }
            ports = portsConfigured;
            log.info("%s: Waiting for ports %s directly on container with IP (%s).",
                     imageConfigDesc, ports, host);
        }
        return new TcpPortChecker(host, ports);
    }

    private List<Integer> getTcpPorts(WaitConfiguration.TcpConfiguration tcpConfig) {
        List<Integer> portsConfigured = tcpConfig.getPorts();
        if (portsConfigured == null || portsConfigured.isEmpty()) {
            throw new IllegalArgumentException("TCP wait config given but no ports to wait on");
        }
        return portsConfigured;
    }

    private WaitConfiguration.TcpConfigMode getTcpMode(WaitConfiguration.TcpConfiguration tcpConfig, String host) {
        WaitConfiguration.TcpConfigMode mode = tcpConfig.getMode();
        if (mode == null) {
            return "localhost".equals(host) ? WaitConfiguration.TcpConfigMode.direct : WaitConfiguration.TcpConfigMode.mapped;
        } else {
            return mode;
        }
    }

    private String getTcpHost(WaitConfiguration.TcpConfiguration tcpConfig, Properties projectProperties) {
        String host = tcpConfig.getHost();
        if (host == null) {
            // Host defaults to ${docker.host.address}.
            host = projectProperties.getProperty("docker.host.address");
        }
        return host;
    }

    private class ContainerRunningPrecondition implements WaitUtil.Precondition {
        private final String containerId;
        private final DockerAccess dockerAccess;
        private Integer exitCode;

        ContainerRunningPrecondition(DockerAccess dockerAccess, String containerId) {
            this.dockerAccess = dockerAccess;
            this.containerId = containerId;
        }

        @Override
        public boolean isOk() {
            try {
                exitCode = dockerAccess.getContainer(containerId).getExitCode();
                return exitCode == null;
            } catch (DockerAccessException e) {
                return false;
            }
        }

        @Override
        public void cleanup() {
            if (exitCode != null && log.isVerboseEnabled()) {
                // if not running, probably something went wrong during startup: spit out logs
                new LogDispatcher(dockerAccess).fetchContainerLog(containerId, LogOutputSpec.DEFAULT);
                dockerAccess.getLogSync(containerId, new DefaultLogCallback(
                    LogOutputSpec.builder()
                        .colorString("black", true)
                        .prefix(containerId.substring(0, 6))
                        .useColor(true)
                        .logStdout(true)
                        .build()
                ));

            }
        }

        Integer getExitCode() {
            return exitCode;
        }
    }

}
