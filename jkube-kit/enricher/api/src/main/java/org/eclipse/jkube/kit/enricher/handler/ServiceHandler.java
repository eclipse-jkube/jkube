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
package org.eclipse.jkube.kit.enricher.handler;

import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServiceFluent;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import org.eclipse.jkube.kit.config.resource.ServiceConfig;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author roland
 */
public class ServiceHandler {

    public List<Service> getServices(List<ServiceConfig> services) {

        ArrayList<Service> ret = new ArrayList<>();

        for (ServiceConfig service : services) {

            ServiceBuilder serviceBuilder = new ServiceBuilder()
                .withNewMetadata()
                  .withName(service.getName())
                  .withAnnotations(getAnnotations(service))
                  .withLabels(getLabels(service))
                .endMetadata();

            ServiceFluent<?>.SpecNested<ServiceBuilder> serviceSpecBuilder = serviceBuilder.withNewSpec();

            List<ServicePort> servicePorts = new ArrayList<>();

            for (ServiceConfig.Port port : Optional.ofNullable(service.getPorts()).orElse(Collections.emptyList())) {
                ServicePort servicePort = new ServicePortBuilder()
                    .withName(port.getName())
                    .withProtocol(port.getProtocol() != null ? port.getProtocol().name() : "TCP")
                    .withTargetPort(new IntOrString(port.getTargetPort()))
                    .withPort(port.getPort())
                    .withNodePort(port.getNodePort())
                    .build();
                servicePorts.add(servicePort);
            }

            if (!servicePorts.isEmpty()) {
                serviceSpecBuilder.withPorts(servicePorts);
            }

            if (service.isHeadless()) {
                serviceSpecBuilder.withClusterIP("None");
            }

            if (StringUtils.isNotBlank(service.getType())) {
                serviceSpecBuilder.withType(service.getType());
            }
            serviceSpecBuilder.endSpec();

            if (service.isHeadless() || !servicePorts.isEmpty()) {
                ret.add(serviceBuilder.build());
            }
        }
        return ret;
    }

    private Map<String, String> getAnnotations(ServiceConfig service) {
        return new HashMap<>();
    }

    private Map<String, String> getLabels(ServiceConfig service) {
        Map<String, String> labels = new HashMap<>();
        if (service.isExpose()) {
            labels.putIfAbsent("expose","true");
        }
        return labels;
    }
}
