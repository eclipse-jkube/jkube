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
package org.eclipse.jkube.enricher.generic.openshift;

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
import org.eclipse.jkube.kit.config.resource.JKubeAnnotations;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.enricher.api.BaseEnricher;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Enricher which generates a Route for each exposed Service
 */
public class RouteEnricher extends BaseEnricher {
    private Boolean generateRoute;

    public RouteEnricher(JKubeEnricherContext buildContext) {
        super(buildContext, "jkube-openshift-route");
        this.generateRoute = getValueFromConfig(GENERATE_ROUTE, true);
    }

    @Override
    public void create(PlatformMode platformMode, final KubernetesListBuilder listBuilder) {
        if(platformMode == PlatformMode.openshift && generateRoute.equals(Boolean.TRUE)) {
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
                listBuilder.addToItems(routeArray);
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
                if ("true".equals(labels.get("expose")) || "true".equals(labels.get(JKubeAnnotations.SERVICE_EXPOSE_URL.value()))) {
                    return true;
                }
            }
        } else {
            log.info("No Metadata for service! " + service);
        }
        return false;
    }
}
