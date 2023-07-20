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
package org.eclipse.jkube.kit.enricher.api;

import io.fabric8.kubernetes.api.builder.Visitor;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Implementing Enrichers should generate Kubernetes Resources/Objects that Expose a Kubernetes Service.
 */
public interface ServiceExposer extends Enricher {

  /**
   * This label was originally consumed by the the deprecated
   * <a href="https://github.com/jenkins-x/exposecontroller">Expose controller</a>.
   * <p>
   * It is now used to prevent a Service from being exposed.
   */
  String EXPOSE_LABEL = "expose";

  final class Util {
    private static final Set<Integer> WEB_PORTS = new HashSet<>(Arrays.asList(80, 443, 8443, 8080, 9080, 9090, 9443));

    private Util() {}

    private static String exposeLabel(ServiceBuilder serviceBuilder) {
      return serviceBuilder.editOrNewMetadata().getLabels().getOrDefault(EXPOSE_LABEL, "")
        .toLowerCase(Locale.ROOT);
    }
  }

  /**
   * Returns a Set of ports exposed by the Service.
   * @param serviceBuilder the Service to get the ports from.
   * @return the Set of ports exposed by the Service.
   */
  @SuppressWarnings({"Convert2Lambda", "java:S1604"}) // Visitors can't be converted to lambdas
  default Set<Integer> getPorts(ServiceBuilder serviceBuilder) {
    final Set<Integer> ret = new HashSet<>();
    if (serviceBuilder != null) {
      serviceBuilder.accept(new Visitor<ServicePortBuilder>() {
        @Override
        public void visit(ServicePortBuilder element) {
          if (element.getPort() != null) {
            ret.add(element.getPort());
          }
        }
      });
    }
    return ret;
  }

  /**
   * Returns true if the Service exposes a 'web' port.
   * @param serviceBuilder the ServiceBuilder to check the ports from.
   * @return true if the Service exposes a 'web' port, false otherwise.
   */
  default boolean hasWebPorts(ServiceBuilder serviceBuilder) {
    for (int port : getPorts(serviceBuilder)) {
      if (Util.WEB_PORTS.contains(port)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Services with the following properties will be automatically exposed:
   * <ul>
   *   <li>Exposure of a single port</li>
   *   <li>Are not labeled expose: false</li>
   *   <li>Not part of the Kubernetes System (?? legacy from IngressEnricher)</li>
   * </ul>
   *
   * @param serviceBuilder the ServiceBuilder to check the ports from.
   * @return true if the Service exposes a single port, false otherwise.
   */
  default boolean canExposeService(ServiceBuilder serviceBuilder) {
    final String serviceName = serviceBuilder.editOrNewMetadata().getName();
    if (Util.exposeLabel(serviceBuilder).equals("false")) {
      getContext().getLog().debug(
        "Service %s can't be exposed, it has en expose: false annotation", serviceName);
      return false;
    }
    // Not sure why this check is performed (moved from IngressEnricher)
    if (Objects.equals(serviceName, "kubernetes") || Objects.equals(serviceName, "kubernetes-ro")) {
      getContext().getLog().debug(
        "Service %s can't be exposed, Kubernetes System services cannot be exposed automatically", serviceName);
      return false;
    }
    final Set<Integer> ports = getPorts(serviceBuilder);
    if (ports.size() != 1) {
      getContext().getLog().info(
        "Service %s can't be exposed, only single port services are supported. Has ports: %s",
        serviceName, ports);
      return false;
    }
    return true;
  }

  /**
   * Returns true if the Service has a <code>metadata.label</code> with the key 'expose' and the value 'true'.
   * <p>
   * n.b. This label should be provided by a fragment or the jkube.enricher.jkube-service.expose configuration.
   * Using the MetadataEnricher will add the label after the Service has been processed by the ServiceExposer(s).
   *
   * @param serviceBuilder the Service to check the label from.
   * @return true if the Service has an expose label, false otherwise.
   */
  default boolean isExposedWithLabel(ServiceBuilder serviceBuilder) {
    return Util.exposeLabel(serviceBuilder).equals("true");
  }
}
