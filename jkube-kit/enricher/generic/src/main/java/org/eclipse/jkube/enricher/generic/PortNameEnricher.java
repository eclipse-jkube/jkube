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
package org.eclipse.jkube.enricher.generic;

import io.fabric8.kubernetes.api.builder.TypedVisitor;
import io.fabric8.kubernetes.api.model.ContainerPortBuilder;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.enricher.api.BaseEnricher;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static io.fabric8.ianaservicehelper.Helper.serviceNames;

/**
 * Use a given set of well known ports and, if not found, create container ports with names with
 * names of IANA registered services, if not already present.
 */
public class PortNameEnricher extends BaseEnricher {

    private static final Map<Integer, String> DEFAULT_PORT_MAPPING = new HashMap<>();
    static {
        DEFAULT_PORT_MAPPING.put(8080, "http");
        DEFAULT_PORT_MAPPING.put(8443, "https");
        DEFAULT_PORT_MAPPING.put(8778, "jolokia");
        DEFAULT_PORT_MAPPING.put(9779, "prometheus");
    }

    public PortNameEnricher(JKubeEnricherContext buildContext) {
        super(buildContext, "jkube-portname");
    }

    @Override
    public void create(PlatformMode platformMode, KubernetesListBuilder builder) {
        builder.accept(new TypedVisitor<ContainerPortBuilder>() {
            @Override
            public void visit(ContainerPortBuilder portBuilder) {
                Integer port = portBuilder.getContainerPort();

                // If port is given but no name, then try to detect the name
                if (port != null && StringUtils.isBlank(portBuilder.getName())) {
                    addPortName(portBuilder, port);
                }
            }
        });
    }

    private void addPortName(ContainerPortBuilder builder, Integer port) {
        String protocol = getProtocol(builder);
        try {
            String serviceName = getDefaultServiceName(port);
            if (serviceName == null) {
                serviceName = extractIANAServiceName(port, protocol);
            }
            if (StringUtils.isNotBlank(serviceName)) {
                builder.withName(serviceName);
            }
        } catch (IOException e) {
            log.error("Internal: Failed to find IANA service names for port %d/%s : %s",
                      port, protocol, e.getMessage());
        }
    }

    private String getProtocol(ContainerPortBuilder builder) {
        return Optional.ofNullable(builder.getProtocol())
                       .filter(StringUtils::isNotBlank)
                       .map(String::toLowerCase)
                       .orElse("tcp");
    }

    private String extractIANAServiceName(Integer port, String protocol) throws IOException {
        Set<String> sn = serviceNames(port, protocol);
        if (sn == null || sn.isEmpty()) {
            return null;
        }

        String serviceName = sn.iterator().next();
        log.verbose("Adding IANA port name %s for port %d", serviceName, port);
        return serviceName;
    }

    private String getDefaultServiceName(Integer port) {
        String serviceName = DEFAULT_PORT_MAPPING.get(port);
        if (StringUtils.isBlank(serviceName)) {
            return null;
        }
        log.verbose("Adding default port name %s for port %d", serviceName, port);
        return serviceName;
    }
}
