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
package io.jshift.enricher.generic;

import io.fabric8.kubernetes.api.builder.TypedVisitor;
import io.fabric8.kubernetes.api.model.ContainerPortBuilder;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.jshift.kit.config.resource.PlatformMode;
import io.jshift.maven.enricher.api.BaseEnricher;
import io.jshift.maven.enricher.api.MavenEnricherContext;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Collections;
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

    private static final Map<Integer, String> DEFAULT_PORT_MAPPING =
        Collections.unmodifiableMap(
            new HashMap<Integer, String>() {{
                put(8080, "http");
                put(8443, "https");
                put(8778, "jolokia");
                put(9779, "prometheus");
            }});

    public PortNameEnricher(MavenEnricherContext buildContext) {
        super(buildContext, "jshift-portname");
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
