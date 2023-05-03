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
package org.eclipse.jkube.kit.build.service.docker;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.eclipse.jkube.kit.build.service.docker.access.DockerAccess;
import org.eclipse.jkube.kit.build.service.docker.access.DockerConnectionDetector;
import org.eclipse.jkube.kit.build.service.docker.access.DockerMachine;
import org.eclipse.jkube.kit.build.service.docker.access.hc.DockerAccessWithHcClient;
import org.eclipse.jkube.kit.build.service.docker.config.DockerMachineConfiguration;
import org.eclipse.jkube.kit.common.KitLogger;

public class DockerAccessFactory {

    public DockerAccess createDockerAccess(DockerAccessContext dockerAccessContext) {

        try {
            DockerConnectionDetector dockerConnectionDetector = createDockerConnectionDetector(dockerAccessContext, dockerAccessContext.getLog());
            DockerConnectionDetector.ConnectionParameter connectionParam =
                    dockerConnectionDetector.detectConnectionParameter(dockerAccessContext.getDockerHost(), dockerAccessContext.getCertPath());
            DockerAccess access = new DockerAccessWithHcClient(connectionParam.getUrl(),
                    connectionParam.getCertPath(),
                    dockerAccessContext.getMaxConnections(),
                    dockerAccessContext.getLog());
            access.start();
            setDockerHostAddressProperty(dockerAccessContext, connectionParam.getUrl());
            return access;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create docker access object ", e);
        }

    }

    private DockerConnectionDetector createDockerConnectionDetector(DockerAccessContext dockerAccessContext, KitLogger log) {
        return new DockerConnectionDetector(getDockerHostProviders(dockerAccessContext, log));
    }

    private List<DockerConnectionDetector.DockerHostProvider> getDockerHostProviders(DockerAccessContext dockerAccessContext, KitLogger log) {
        if (dockerAccessContext.getDockerHostProviders() != null) {
            return dockerAccessContext.getDockerHostProviders();
        }

        return getDefaultDockerHostProviders(dockerAccessContext, log);
    }

    /**
     * Return a list of providers which could delive connection parameters from
     * calling external commands. For this plugin this is docker-machine, but can be overridden
     * to add other config options, too.
     *
     * @return list of providers or <code>null</code> if none are applicable
     */
    private List<DockerConnectionDetector.DockerHostProvider> getDefaultDockerHostProviders(DockerAccessContext dockerAccessContext, KitLogger log) {

        DockerMachineConfiguration config = dockerAccessContext.getMachine();
        if (dockerAccessContext.isSkipMachine()) {
            config = null;
        } else if (config == null) {
            Properties projectProps = dockerAccessContext.getProjectProperties();
            if (projectProps.containsKey(DockerMachineConfiguration.DOCKER_MACHINE_NAME_PROP)) {
                config = new DockerMachineConfiguration(
                    projectProps.getProperty(DockerMachineConfiguration.DOCKER_MACHINE_NAME_PROP),
                    projectProps.getProperty(DockerMachineConfiguration.DOCKER_MACHINE_AUTO_CREATE_PROP),
                    projectProps.getProperty(DockerMachineConfiguration.DOCKER_MACHINE_REGENERATE_CERTS_AFTER_START_PROP));
            }
        }

        List<DockerConnectionDetector.DockerHostProvider> ret = new ArrayList<>();
        ret.add(new DockerMachine(log, config));
        return ret;
    }

    // Registry for managed containers
    private void setDockerHostAddressProperty(DockerAccessContext dockerAccessContext, String dockerUrl) {
        Properties props = dockerAccessContext.getProjectProperties();
        if (props.getProperty("docker.host.address") == null) {
            final String host;
            try {
                URI uri = new URI(dockerUrl);
                if (uri.getHost() == null && (uri.getScheme().equals("unix") || uri.getScheme().equals("npipe"))) {
                    host = "localhost";
                } else {
                    host = uri.getHost();
                }
            } catch (URISyntaxException e) {
                throw new IllegalStateException("Cannot parse " + dockerUrl + " as URI: " + e.getMessage(), e);
            }
            props.setProperty("docker.host.address", host == null ? "" : host);
        }
    }

    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @EqualsAndHashCode
    public static class DockerAccessContext implements Serializable {

        private static final long serialVersionUID = -4768689117574797600L;

        public static final int DEFAULT_MAX_CONNECTIONS = 100;

        private Properties projectProperties;
        private DockerMachineConfiguration machine;
        private List<DockerConnectionDetector.DockerHostProvider> dockerHostProviders;
        private boolean skipMachine;
        private String dockerHost;
        private String certPath;
        private int maxConnections;
        private KitLogger log;

        public static DockerAccessContext getDefault(KitLogger kitLogger){
            return DockerAccessContext.builder()
                .projectProperties(System.getProperties())
                .skipMachine(false)
                .maxConnections(DEFAULT_MAX_CONNECTIONS)
                .log(kitLogger)
                .build();
        }
    }

}
