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
package org.eclipse.jkube.enricher.generic;

import io.fabric8.kubernetes.api.builder.TypedVisitor;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimBuilder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jkube.kit.common.Configs;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.enricher.api.BaseEnricher;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;

public class PersistentVolumeClaimStorageClassEnricher extends BaseEnricher {

  public static final String ENRICHER_NAME = "jkube-persistentvolumeclaim-storageclass";
  static final String VOLUME_STORAGE_CLASS_ANNOTATION = "volume.beta.kubernetes.io/storage-class";

  @AllArgsConstructor
  enum Config implements Configs.Config {
    DEFAULT_STORAGE_CLASS("defaultStorageClass", null),
    USE_ANNOTATION("useStorageClassAnnotation", "false");

    @Getter
    protected String key;
    @Getter
    protected String defaultValue;
  }

  public PersistentVolumeClaimStorageClassEnricher(JKubeEnricherContext buildContext) {
    super(buildContext, ENRICHER_NAME);
  }

  @Override
  public void enrich(PlatformMode platformMode, KubernetesListBuilder builder) {
    builder.accept(new TypedVisitor<PersistentVolumeClaimBuilder>() {
      @Override
      public void visit(PersistentVolumeClaimBuilder pvcBuilder) {
        // lets ensure we have a default storage class so that PVs will get dynamically created OOTB
        if (pvcBuilder.buildMetadata() == null) {
          pvcBuilder.withNewMetadata().endMetadata();
        }
        String storageClass = getStorageClass();
        if (StringUtils.isNotBlank(storageClass)) {
          if (shouldUseAnnotation()) {
            pvcBuilder.editMetadata().addToAnnotations(VOLUME_STORAGE_CLASS_ANNOTATION, storageClass).endMetadata();
          } else {
            pvcBuilder.editSpec().withStorageClassName(storageClass).endSpec();
          }
        }
      }
    });
  }

  private boolean shouldUseAnnotation() {
    if (Boolean.TRUE.equals(Boolean.parseBoolean(getConfig(Config.USE_ANNOTATION)))) {
      return true;
    }
    VolumePermissionEnricher volumePermissionEnricher = new VolumePermissionEnricher((JKubeEnricherContext) getContext());
    return Boolean.TRUE.equals(volumePermissionEnricher.shouldUseAnnotation());
  }

  private String getStorageClass() {
    String storageClassConfig = getConfig(Config.DEFAULT_STORAGE_CLASS);
    if (StringUtils.isNotBlank(storageClassConfig)) {
      return storageClassConfig;
    }
    VolumePermissionEnricher volumePermissionEnricher = new VolumePermissionEnricher((JKubeEnricherContext) getContext());
    return volumePermissionEnricher.getDefaultStorageClass();
  }
}
