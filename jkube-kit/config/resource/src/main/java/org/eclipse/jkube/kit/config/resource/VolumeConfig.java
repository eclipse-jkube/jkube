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
package org.eclipse.jkube.kit.config.resource;

import java.util.List;

import io.fabric8.kubernetes.api.model.DownwardAPIVolumeFile;
import io.fabric8.kubernetes.api.model.KeyToPath;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Singular;

/**
 * Configuration for a single volume
 *
 * @author roland
 */
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@EqualsAndHashCode()
public class VolumeConfig {

  private String type;
  private String name;
  /**
   * List of mount paths of this volume.
   */
  @Singular
  private List<String> mounts;
  private String path;
  private String medium;
  private String repository;
  private String revision;
  private String secretName;
  private String server;
  private Boolean readOnly;
  private String pdName;
  private String fsType;
  private Integer partition;
  private String endpoints;
  private String claimRef;
  private String volumeId;
  private String diskName;
  private String diskUri;
  private String kind;
  private String cachingMode;
  private String hostPathType;
  private String shareName;
  private String user;
  private String secretFile;
  private String secretRef;
  private Integer lun;
  @Singular
  private List<String> targetWwns;
  private String datasetName;
  @Singular
  private List<String> portals;
  private String targetPortal;
  private String registry;
  private String volume;
  private String group;
  private String iqn;
  @Singular
  private List<String> monitors;
  private String pool;
  private String keyring;
  private String image;
  private String gateway;
  private String system;
  private String protectionDomain;
  private String storagePool;
  private String volumeName;
  private String configMapName;
  @Singular
  private List<KeyToPath> configMapItems;
  @Singular
  private List<DownwardAPIVolumeFile> items;

}
