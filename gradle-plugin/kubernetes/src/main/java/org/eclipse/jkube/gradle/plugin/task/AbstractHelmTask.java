package org.eclipse.jkube.gradle.plugin.task;

import org.eclipse.jkube.gradle.plugin.KubernetesExtension;
import org.eclipse.jkube.kit.resource.helm.HelmConfig;

import javax.inject.Inject;

import java.io.IOException;

import static org.eclipse.jkube.kit.resource.helm.HelmServiceUtil.initHelmConfig;

public abstract class AbstractHelmTask extends AbstractJKubeTask {


  @Inject
  protected AbstractHelmTask(Class<? extends KubernetesExtension> extensionClass) {
    super(extensionClass);
  }

  @Override
  public void run() {
    if(kubernetesExtension.getSkipOrDefault()) {
      return;
    }

    try {
      HelmConfig helmConfig = initHelmConfig(
              kubernetesExtension.getDefaultHelmType(),
              kubernetesExtension.javaProject,
              kubernetesExtension.getKubernetesTemplateOrDefault(),
              kubernetesExtension.helm
      ).build();

      executeTask(helmConfig);

    } catch (IOException e) {
      kitLogger.error("Error initializing Helm configuration", e);
      throw new IllegalStateException(e.getMessage(), e);
    }
  }

  protected abstract void executeTask(HelmConfig helmConfig);
}
