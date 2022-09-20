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


class LocalServiceManager {

  private static final String PREVIOUS_SERVICE_ANNOTATION = "previous-service";

  private final KubernetesClient kubernetesClient;
  private final RemoteDevelopmentConfig remoteDevelopmentConfig;

  public LocalServiceManager(RemoteDevelopmentConfig remoteDevelopmentConfig, KubernetesClient kubernetesClient) {
    this.remoteDevelopmentConfig = remoteDevelopmentConfig;
    this.kubernetesClient = kubernetesClient;
  }

  public void createOrReplaceServices() {
    for (LocalService localService : remoteDevelopmentConfig.getLocalServices()) {
      final Service existingService = kubernetesClient.services().withName(localService.getServiceName()).get();
      final Service newService;
      if (existingService == null) {
        newService = localService.toKubernetesService();
      } else {
        newService = new ServiceBuilder(localService.toKubernetesService())
          .editOrNewMetadata()
          .addToAnnotations(PREVIOUS_SERVICE_ANNOTATION, Serialization.asJson(existingService))
          .endMetadata()
          .build();
      }
      kubernetesClient.services().resource(newService).createOrReplace();
    }
  }

  public void tearDownServices() {
    for (LocalService localService : remoteDevelopmentConfig.getLocalServices()) {
      final Service service = kubernetesClient.services().withName(localService.getServiceName()).get();
      if (service.getMetadata().getAnnotations().get(PREVIOUS_SERVICE_ANNOTATION) != null) {
        final Service previousService = Serialization.unmarshal(
          service.getMetadata().getAnnotations().get(PREVIOUS_SERVICE_ANNOTATION), Service.class);
        kubernetesClient.services().resource(previousService).createOrReplace();
      } else {
        kubernetesClient.services().resource(service).delete();
      }
    }
  }
}
