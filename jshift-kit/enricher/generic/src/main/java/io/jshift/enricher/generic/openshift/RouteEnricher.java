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

package io.jshift.enricher.generic.openshift;

import io.fabric8.kubernetes.api.builder.TypedVisitor;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.ServiceSpec;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.RouteBuilder;
import io.fabric8.openshift.api.model.RoutePort;
import io.jshift.kit.config.resource.JshiftAnnotations;
import io.jshift.kit.config.resource.PlatformMode;
import io.jshift.maven.enricher.api.BaseEnricher;
import io.jshift.maven.enricher.api.MavenEnricherContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Enricher which generates a Route for each exposed Service
 */
public class RouteEnricher extends BaseEnricher {

    public RouteEnricher(MavenEnricherContext buildContext) {
        super(buildContext, "jshift-openshift-route");
    }

    @Override
    public void create(PlatformMode platformMode, final KubernetesListBuilder listBuilder) {
        if(platformMode == PlatformMode.openshift) {
            final List<Route> routes = new ArrayList<>();
            listBuilder.accept(new TypedVisitor<ServiceBuilder>() {

                @Override
                public void visit(ServiceBuilder serviceBuilder) {
                    addRoute(listBuilder, serviceBuilder, routes);
                }
            });

            if (!routes.isEmpty()) {
                Route[] routeArray = new Route[routes.size()];
                routes.toArray(routeArray);
                listBuilder.addToRouteItems(routeArray);
            }
        }
    }

    private void addRoute(KubernetesListBuilder listBuilder, ServiceBuilder serviceBuilder, List<Route> routes) {
        ObjectMeta metadata = serviceBuilder.getMetadata();
        if (metadata != null && isExposedService(serviceBuilder)) {
            String name = metadata.getName();
            if (!hasRoute(listBuilder, name)) {
                RoutePort routePort = createRoutePort(serviceBuilder);
                if (routePort != null) {
                    // TODO one day lets support multiple ports on a Route when the model supports it
                    routes.add(new RouteBuilder().
                            withMetadata(serviceBuilder.getMetadata()).
                            withNewSpec().
                            withPort(routePort).
                            withNewTo().withKind("Service").withName(name).endTo().
                            endSpec().
                            build());
                }
            }
        }
    }

    private RoutePort createRoutePort(ServiceBuilder serviceBuilder) {
        RoutePort routePort = null;
        ServiceSpec spec = serviceBuilder.getSpec();
        if (spec != null) {
            List<ServicePort> ports = spec.getPorts();
            if (ports != null && ports.size() > 0) {
                ServicePort servicePort = ports.get(0);
                if (servicePort != null) {
                    IntOrString targetPort = servicePort.getTargetPort();
                    if (targetPort != null) {
                        routePort = new RoutePort();
                        routePort.setTargetPort(targetPort);
                    }
                }
            }
        }
        return routePort;
    }

    /**
     * Returns true if we already have a route created for the given name
     */
    private boolean hasRoute(final KubernetesListBuilder listBuilder, final String name) {
        final AtomicBoolean answer = new AtomicBoolean(false);
        listBuilder.accept(new TypedVisitor<RouteBuilder>() {

            @Override
            public void visit(RouteBuilder builder) {
                ObjectMeta metadata = builder.getMetadata();
                if (metadata != null && name.equals(metadata.getName())) {
                    answer.set(true);
                }
            }
        });
        return answer.get();
    }

    protected boolean isExposedService(ServiceBuilder serviceBuilder) {
        Service service = serviceBuilder.build();
        return isExposedService(service);
    }

    protected boolean isExposedService(Service service) {
        ObjectMeta metadata = service.getMetadata();
        if (metadata != null) {
            Map<String, String> labels = metadata.getLabels();
            if (labels != null) {
                if ("true".equals(labels.get("expose")) || "true".equals(labels.get(JshiftAnnotations.SERVICE_EXPOSE_URL.value()))) {
                    return true;
                }
            }
        } else {
            log.info("No Metadata for service! " + service);
        }
        return false;
    }
}
