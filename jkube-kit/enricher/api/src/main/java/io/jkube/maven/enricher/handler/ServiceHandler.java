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
package io.jkube.maven.enricher.handler;

import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServiceFluent;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.jkube.kit.common.util.MapUtil;
import io.jkube.kit.config.resource.ServiceConfig;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author roland
 * @since 08/04/16
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

            ServiceFluent.SpecNested<ServiceBuilder> serviceSpecBuilder = serviceBuilder.withNewSpec();

            List<ServicePort> servicePorts = new ArrayList<>();

            for (ServiceConfig.Port port : service.getPorts()) {
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
            MapUtil.putIfAbsent(labels, "expose", "true");
        }
        return labels;
    }
}
