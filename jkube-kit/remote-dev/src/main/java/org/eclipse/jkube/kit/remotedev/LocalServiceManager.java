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
package org.eclipse.jkube.kit.remotedev;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.Serialization;
import org.eclipse.jkube.kit.common.KitLogger;

import java.util.Collections;
import java.util.List;


class LocalServiceManager {

  private static final String PREVIOUS_SERVICE_ANNOTATION = "jkube/previous-service";

  private final KitLogger logger;
  private final KubernetesClient kubernetesClient;
  private final List<LocalService> localServices;

  public LocalServiceManager(RemoteDevelopmentContext context) {
    logger = context.getLogger();
    kubernetesClient = context.getKubernetesClient();
    localServices = context.getRemoteDevelopmentConfig().getLocalServices();
  }

  public void createOrReplaceServices() {
    logger.debug("Creating or replacing Kubernetes services for exposed ports from local environment");
    for (LocalService localService : localServices) {
      final Service existingService = kubernetesClient.services().withName(localService.getServiceName()).get();
      final Service newService;
      if (existingService == null) {
        newService = localService.toKubernetesService();
      } else {
        final String previousServiceAnnotation;
        if (existingService.getMetadata().getAnnotations().get(PREVIOUS_SERVICE_ANNOTATION) != null) {
          previousServiceAnnotation = existingService.getMetadata().getAnnotations().get(PREVIOUS_SERVICE_ANNOTATION);
        } else {
          final Service cleanExistingService = new ServiceBuilder(existingService)
            .withStatus(null)
            .editSpec()
            .withClusterIP(null)
            .withClusterIPs(Collections.emptyList())
            .withExternalIPs(Collections.emptyList())
            .endSpec()
            .build();
          previousServiceAnnotation = Serialization.asJson(cleanExistingService);
        }
        newService = new ServiceBuilder(localService.toKubernetesService())
          .editOrNewMetadata()
          .addToAnnotations(PREVIOUS_SERVICE_ANNOTATION, previousServiceAnnotation)
          .endMetadata()
          .build();
      }
      kubernetesClient.services().resource(newService).createOrReplace();
    }
  }

  public void tearDownServices() {
    logger.debug("Tearing down Kubernetes services for exposed ports from local environment");
    for (LocalService localService : localServices) {
      final Service service = kubernetesClient.services().withName(localService.getServiceName()).get();
      if (service != null && service.getMetadata().getAnnotations().get(PREVIOUS_SERVICE_ANNOTATION) != null) {
        final Service previousService = Serialization.unmarshal(
          service.getMetadata().getAnnotations().get(PREVIOUS_SERVICE_ANNOTATION), Service.class);
        kubernetesClient.services().resource(previousService).createOrReplace();
      } else if (service != null) {
        kubernetesClient.services().withName(localService.getServiceName()).delete();
      }
    }
  }
}
