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
package org.eclipse.jkube.gradle.plugin.task;

import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import org.eclipse.jkube.gradle.plugin.KubernetesExtension;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.KubernetesHelper;
import org.eclipse.jkube.kit.common.util.OpenshiftHelper;
import org.eclipse.jkube.kit.config.service.ApplyService;
import org.eclipse.jkube.kit.enricher.api.util.KubernetesResourceUtil;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.List;

import static org.eclipse.jkube.kit.config.service.kubernetes.KubernetesClientUtil.resolveFallbackNamespace;

@SuppressWarnings("CdiInjectionPointsInspection")
public class KubernetesApplyTask extends AbstractJKubeTask {
  private ApplyService applyService;

  @Inject
  public KubernetesApplyTask(Class<? extends KubernetesExtension> extensionClass) {
    super(extensionClass);
    setDescription("Deploys the generated cluster resource configuration manifests into Kubernetes.");
  }

  @Override
  public void run() {
    try (KubernetesClient kubernetes = jKubeServiceHub.getClient()) {
      applyService = jKubeServiceHub.getApplyService();

      final File manifest = getManifest(kubernetes);
      URL masterUrl = kubernetes.getMasterUrl();
      KubernetesResourceUtil.validateKubernetesMasterUrl(masterUrl);
      List<HasMetadata> entities = KubernetesHelper.loadResources(manifest);

      configureApplyService();

      kitLogger.info("Using %s at %s in namespace %s with manifest %s ", OpenshiftHelper.isOpenShift(kubernetes) ? "OpenShift" : "Kubernetes", masterUrl, applyService.getNamespace(), manifest);

      // Apply rest of the entities present in manifest
      applyEntities(manifest.getName(), entities);
      kitLogger.info("[[B]]HINT:[[B]] Use the command `%s get pods -w` to watch your pods start up",
          clusterAccess.isOpenShift() ? "oc" : "kubectl");
    } catch (KubernetesClientException e) {
      KubernetesResourceUtil.handleKubernetesClientException(e, kitLogger);
    } catch (IOException ioException) {
      kitLogger.error("Error in loading Kubernetes Manifests ", ioException);
      throw new IllegalStateException(ioException);
    }
  }

  @Override
  protected boolean shouldSkip() {
    return super.shouldSkip() || kubernetesExtension.getSkipApplyOrDefault();
  }

  private void applyEntities(String fileName, final Collection<HasMetadata> entities) {
    KitLogger serviceLogger = createLogger("[[G]][SVC][[G]] [[s]]");
    applyService.applyEntities(fileName, entities, serviceLogger, kubernetesExtension.getServiceUrlWaitTimeSecondsOrDefault());
  }

  protected void configureApplyService() {
    applyService.setAllowCreate(kubernetesExtension.getCreateNewResourcesOrDefault());
    applyService.setServicesOnlyMode(kubernetesExtension.getServicesOnlyOrDefault());
    applyService.setIgnoreServiceMode(kubernetesExtension.getIgnoreServicesOrDefault());
    applyService.setLogJsonDir(kubernetesExtension.getJsonLogDirOrDefault());
    applyService.setBasedir(kubernetesExtension.javaProject.getBaseDirectory());
    applyService.setSupportOAuthClients(kubernetesExtension.isSupportOAuthClients());
    applyService.setIgnoreRunningOAuthClients(kubernetesExtension.getIgnoreRunningOAuthClientsOrDefault());
    applyService.setProcessTemplatesLocally(kubernetesExtension.getProcessTemplatesLocallyOrDefault());
    applyService.setDeletePodsOnReplicationControllerUpdate(kubernetesExtension.getDeletePodsOnReplicationControllerUpdateOrDefault());
    applyService.setRollingUpgrade(kubernetesExtension.getRollingUpgradesOrDefault());
    applyService.setRollingUpgradePreserveScale(kubernetesExtension.getRollingUpgradePreserveScaleOrDefault());
    applyService.setRecreateMode(kubernetesExtension.getRecreateOrDefault());
    applyService.setNamespace(kubernetesExtension.getNamespaceOrNull());
    applyService.setFallbackNamespace(resolveFallbackNamespace(kubernetesExtension.resources, clusterAccess));
  }
}
