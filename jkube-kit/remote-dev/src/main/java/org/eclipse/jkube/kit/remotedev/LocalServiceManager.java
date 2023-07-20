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
package org.eclipse.jkube.kit.remotedev;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.Serialization;

import java.util.Collections;

import static org.eclipse.jkube.kit.remotedev.RemoteDevelopmentService.LABEL_INSTANCE;


class LocalServiceManager {

  private static final String PREVIOUS_SERVICE_ANNOTATION = "jkube/previous-service";

  private final KitLogger logger;
  private final KubernetesClient kubernetesClient;
  private final RemoteDevelopmentContext context;

  public LocalServiceManager(RemoteDevelopmentContext context) {
    this.context = context;
    logger = context.getLogger();
    kubernetesClient = context.getKubernetesClient();
  }

  public void createOrReplaceServices() {
    logger.debug("Creating or replacing Kubernetes services for exposed ports from local environment");
    for (LocalService localService : context.getRemoteDevelopmentConfig().getLocalServices()) {
      final Service existingService = kubernetesClient.services().withName(localService.getServiceName()).get();
      final ServiceBuilder newServiceBuilder;
      if (existingService == null) {
        newServiceBuilder = new ServiceBuilder(localService.toKubernetesService());
      } else {
        final String previousServiceAnnotation;
        if (existingService.getMetadata().getAnnotations().get(PREVIOUS_SERVICE_ANNOTATION) != null) {
          previousServiceAnnotation = existingService.getMetadata().getAnnotations().get(PREVIOUS_SERVICE_ANNOTATION);
        } else {
          final Service sanitizedExistingService = new ServiceBuilder(existingService)
            .withStatus(null)
            .editSpec()
            .withClusterIP(null)
            .withClusterIPs(Collections.emptyList())
            .withExternalIPs(Collections.emptyList())
            .endSpec()
            .build();
          previousServiceAnnotation = Serialization.asJson(sanitizedExistingService);
        }
        newServiceBuilder = new ServiceBuilder(localService.toKubernetesService())
          .editOrNewMetadata()
          .addToAnnotations(PREVIOUS_SERVICE_ANNOTATION, previousServiceAnnotation)
          .endMetadata();
        // Prefer existing service ports
        if (existingService.getSpec().getPorts() != null && existingService.getSpec().getPorts().size() == 1) {
          newServiceBuilder.editOrNewSpec()
            .withPorts(existingService.getSpec().getPorts())
            .endSpec();
        }
      }
      final Service newService = newServiceBuilder
        .editSpec()
        .addToSelector(LABEL_INSTANCE, context.getSessionID().toString())
        .endSpec()
        .build();
      kubernetesClient.services().resource(newService).createOrReplace();
      context.getManagedServices().put(localService, newService);
    }
  }

  public void tearDownServices() {
    logger.debug("Tearing down Kubernetes services for exposed ports from local environment");
    for (Service managedService : context.getManagedServices().values()) {
      final Service service = kubernetesClient.services().resource(managedService).get();
      if (service != null && service.getMetadata().getAnnotations().get(PREVIOUS_SERVICE_ANNOTATION) != null) {
        final Service previousService = Serialization.unmarshal(
          service.getMetadata().getAnnotations().get(PREVIOUS_SERVICE_ANNOTATION), Service.class);
        kubernetesClient.services().resource(previousService).createOrReplace();
      } else if (service != null) {
        kubernetesClient.services().resource(service).delete();
      }
    }
  }
}
