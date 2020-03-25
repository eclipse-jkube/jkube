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
package org.eclipse.jkube.kit.config.resource;

import java.util.List;

import io.fabric8.kubernetes.api.model.DownwardAPIVolumeFile;
import io.fabric8.kubernetes.api.model.KeyToPath;

/**
 * Configuration for a single volume
 *
 * @author roland
 * @since 22/03/16
 */
public class VolumeConfig {

    private String type;

    private String name;

    // List of mount paths of this volume
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

    private List<String> targetWwns;

    private String datasetName;

    private List<String> portals;

    private String targetPortal;

    private String registry;

    private String volume;

    private String group;

    private String iqn;

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

    private List<KeyToPath> configMapItems;

    private List<DownwardAPIVolumeFile> items;

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public String getMedium() {
        return medium;
    }

    public String getRepository() {
        return repository;
    }

    public String getRevision() {
        return revision;
    }

    public String getSecretName() {
        return secretName;
    }

    public String getServer() {
        return server;
    }

    public Boolean getReadOnly() {
        return readOnly;
    }

    public String getPdName() {
        return pdName;
    }

    public String getFsType() {
        return fsType;
    }

    public Integer getPartition() {
        return partition;
    }

    public String getEndpoints() {
        return endpoints;
    }

    public String getClaimRef() {
        return claimRef;
    }

    public List<String> getMounts() {
        return mounts;
    }

    public String getVolumeId() { return volumeId; }

    public String getDiskName() { return diskName; }

    public String getDiskUri() { return diskUri; }

    public String getKind() { return kind; }

    public String getCachingMode() { return cachingMode; }

    public String getShareName() { return shareName; }

    public String getUser() { return user; }

    public String getGroup() { return group; }

    public String getSecretFile() { return secretFile; }

    public String getSecretRef() { return secretRef; }

    public Integer getLun() { return lun; }

    public List<String> getTargetWwns() { return targetWwns; }

    public String getDatasetName() { return datasetName; }

    public List<String> getPortals() { return portals; }

    public String getTargetPortal() { return targetPortal; }

    public String getIqn() { return iqn; }

    public String getVolume() { return volume; }

    public String getRegistry() { return registry; }

    public List<String> getMonitors() { return monitors; }

    public String getPool() { return pool; }

    public String getKeyring() { return keyring; }

    public String getImage() { return image; }

    public String getGateway() { return gateway; }

    public String getSystem() { return system; }

    public String getProtectionDomain() { return protectionDomain; }

    public String getStoragePool() { return storagePool; }

    public String getVolumeName() { return volumeName; }

    public List<DownwardAPIVolumeFile> getItems() { return items; }

    public String getHostPathType() { return hostPathType; }

    public String getConfigMapName() { return configMapName; }

    public List<KeyToPath> getConfigMapItems() { return configMapItems; }

    public void setConfigMapName(String configMapName) {
        this.configMapName = configMapName;
    }


    public static class Builder {
        private VolumeConfig volumeConfig = new VolumeConfig();

        public VolumeConfig.Builder name(String name) {
            volumeConfig.name = name;
            return this;
        }

        public VolumeConfig.Builder mounts(List<String> mounts) {
            volumeConfig.mounts = mounts;
            return this;
        }

        public VolumeConfig.Builder type(String type) {
            volumeConfig.type = type;
            return this;
        }

        public VolumeConfig.Builder path(String path) {
            volumeConfig.path = path;
            return this;
        }

        public VolumeConfig build() {
            return volumeConfig;
        }
    }

    // TODO: Change to rich configuration as described in http://blog.sonatype.com/2011/03/configuring-plugin-goals-in-maven-3/

}
