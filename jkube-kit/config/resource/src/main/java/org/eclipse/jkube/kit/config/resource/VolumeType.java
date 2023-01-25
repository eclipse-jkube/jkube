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


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.fabric8.kubernetes.api.model.DownwardAPIVolumeFile;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;

public enum VolumeType {

    HOST_PATH("hostPath") {
        @Override
        public Volume fromConfig(VolumeConfig config) {
            return new VolumeBuilder()
                    .withName(config.getName())
                    .withNewHostPath(config.getPath(), config.getHostPathType())
                    .build();
        }
    }, EMPTY_DIR("emptyDir") {
        @Override
        public Volume fromConfig(VolumeConfig config) {
            return new VolumeBuilder()
                    .withName(config.getName())
                    .withNewEmptyDir().withMedium(config.getMedium()).endEmptyDir()
                    .build();

        }
    }, GIT_REPO("gitRepo") {
        public Volume fromConfig(VolumeConfig config) {
            String repository = config.getRepository();
            String revision = config.getRevision();
            return new VolumeBuilder()
                    .withName(config.getName())
                    .withNewGitRepo().withRepository(repository).withRevision(revision).endGitRepo()
                    .build();
        }
    }, SECRET("secret") {
        public Volume fromConfig(VolumeConfig config) {
            String secretName = config.getSecretName();
            return new VolumeBuilder()
                    .withName(config.getName())
                    .withNewSecret().withSecretName(secretName).endSecret()
                    .build();
        }
    }, CONFIGMAP("configMap") {
        public Volume fromConfig(VolumeConfig config) {

            return new VolumeBuilder()
                    .withName(config.getName())
                    .withNewConfigMap()
                    .withName(config.getConfigMapName())
                    .withItems(config.getConfigMapItems())
                    .endConfigMap()
                    .build();
        }
    }, NFS_PATH("nfsPath") {
        public Volume fromConfig(VolumeConfig config) {
            String path = config.getPath();
            String server = config.getServer();
            Boolean readOnly = config.getReadOnly();
            return new VolumeBuilder()
                    .withName(config.getName())
                    .withNewNfs(path, readOnly, server)
                    .build();
        }
    }, CGE_DISK("gcePdName") {
        public Volume fromConfig(VolumeConfig config) {

            String pdName = config.getPdName();
            String fsType = config.getFsType();
            Integer partition = config.getPartition();
            Boolean readOnly = config.getReadOnly();

            return new VolumeBuilder()
                    .withName(config.getName())
                    .withNewGcePersistentDisk(fsType, partition, pdName, readOnly)
                    .build();
        }

    }, GLUSTER_FS_PATH("glusterFsPath") {
        public Volume fromConfig(VolumeConfig config) {
            String path = config.getPath();
            String endpoints = config.getEndpoints();
            Boolean readOnly = config.getReadOnly();

            return new VolumeBuilder()
                    .withName(config.getName())
                    .withNewGlusterfs(path, endpoints, readOnly)
                    .build();
        }

    }, PERSISTENT_VOLUME_CLAIM("persistentVolumeClaim") {
        public Volume fromConfig(VolumeConfig config) {
            String claimRef = config.getClaimRef();
            Boolean readOnly = config.getReadOnly();

            return new VolumeBuilder()
                    .withName(config.getName())
                    .withNewPersistentVolumeClaim(claimRef, readOnly)
                    .build();
        }

    }, AWS_ELASTIC_BLOCK_STORE("awsElasticBlockStore") {
        public Volume fromConfig(VolumeConfig config) {
            String volumeId = config.getVolumeId();
            String fsType = config.getFsType();
            Boolean readOnly = config.getReadOnly();

            return new VolumeBuilder()
                    .withName(config.getName())
                    .withNewAwsElasticBlockStore()
                    .withFsType(fsType)
                    .withReadOnly(readOnly)
                    .withVolumeID(volumeId)
                    .endAwsElasticBlockStore()
                    .build();
        }
    }, AZURE_DISK("azureDisk") {
        public Volume fromConfig(VolumeConfig config) {
            String diskName = config.getDiskName();
            String diskURI = config.getDiskUri();
            String fsType = config.getFsType();
            String kind = config.getKind();
            String cachingMode = config.getCachingMode();
            Boolean readOnly = config.getReadOnly();

            return new VolumeBuilder()
                    .withName(config.getName())
                    .withNewAzureDisk()
                    .withDiskName(diskName)
                    .withDiskURI(diskURI)
                    .withFsType(fsType)
                    .withReadOnly(readOnly)
                    .withCachingMode(cachingMode)
                    .withKind(kind)
                    .endAzureDisk()
                    .build();
        }
    }, AZURE_FILE("azureFile") {
        public Volume fromConfig(VolumeConfig config) {
            Boolean readOnly = config.getReadOnly();
            String secretName = config.getSecretName();
            String shareName = config.getShareName();

            return new VolumeBuilder()
                    .withName(config.getName())
                    .withNewAzureFile()
                    .withReadOnly(readOnly)
                    .withSecretName(secretName)
                    .withShareName(shareName)
                    .endAzureFile()
                    .build();
        }
    }, CEPHFS("cephfs") {
        public Volume fromConfig(VolumeConfig config) {
            String path = config.getPath();
            Boolean readOnly = config.getReadOnly();
            String user = config.getUser();
            String secretFile = config.getSecretFile();
            String secretRef = config.getSecretRef();

            return new VolumeBuilder()
                    .withName(config.getName())
                    .withNewCephfs()
                    .withPath(path)
                    .withMonitors()
                    .withUser(user)
                    .withReadOnly(readOnly)
                    .withSecretFile(secretFile)
                    .withNewSecretRef().withName(secretRef).endSecretRef()
                    .endCephfs()
                    .build();
        }
    }, FIBRE_CHANNEL("fc") {
        public Volume fromConfig(VolumeConfig config) {
            Boolean readOnly = config.getReadOnly();
            String fsType = config.getFsType();
            Integer lun = config.getLun();
            List<String> targetWWNs = config.getTargetWwns();

            return new VolumeBuilder()
                    .withName(config.getName())
                    .withNewFc()
                    .withWwids(targetWWNs)
                    .withReadOnly(readOnly)
                    .withFsType(fsType)
                    .withLun(lun)
                    .endFc()
                    .build();
        }
    }, FLOCKER("flocker") {
        public Volume fromConfig(VolumeConfig config) {
            String datasetName = config.getDatasetName();

            return new VolumeBuilder()
                    .withName(config.getName())
                    .withNewFlocker()
                    .withDatasetName(datasetName)
                    .endFlocker()
                    .build();
        }
    }, ISCSI("iscsi") {
        public Volume fromConfig(VolumeConfig config) {
            List<String> portals = config.getPortals();
            String targetPortal = config.getTargetPortal();
            String iqn = config.getIqn();
            Integer lun = config.getLun();
            String fsType = config.getFsType();
            Boolean readOnly = config.getReadOnly();

            return new VolumeBuilder()
                    .withName(config.getName())
                    .withNewIscsi()
                    .withTargetPortal(targetPortal)
                    .withPortals(portals)
                    .withIqn(iqn)
                    .withLun(lun)
                    .withFsType(fsType)
                    .withReadOnly(readOnly)
                    .endIscsi()
                    .build();
        }
    }, PORTWORXVOLUME("portworxVolume") {
        public Volume fromConfig(VolumeConfig config) {
            String fsType = config.getFsType();
            String volumeId = config.getVolumeId();
            Boolean readOnly = config.getReadOnly();

            return new VolumeBuilder()
                    .withName(config.getName())
                    .withNewPortworxVolume()
                    .withFsType(fsType)
                    .withVolumeID(volumeId)
                    .withReadOnly(readOnly)
                    .endPortworxVolume()
                    .build();
        }
    }, QUOBYTE("quobyte") {
        public Volume fromConfig(VolumeConfig config) {
            String registry = config.getRegistry();
            String volume = config.getVolume();
            Boolean readOnly = config.getReadOnly();
            String user = config.getUser();
            String group = config.getGroup();

            return new VolumeBuilder()
                    .withName(config.getName())
                    .withNewQuobyte()
                    .withRegistry(registry)
                    .withVolume(volume)
                    .withReadOnly(readOnly)
                    .withUser(user)
                    .withGroup(group)
                    .endQuobyte()
                    .build();
        }
    }, RADOS_BLOCK_DEVICE("rbd") {
        public Volume fromConfig(VolumeConfig config) {
            List<String> monitors = config.getMonitors();
            String pool = config.getPool();
            String fsType = config.getFsType();
            Boolean readOnly = config.getReadOnly();
            String user = config.getUser();
            String keyring = config.getKeyring();
            String image = config.getImage();

            return new VolumeBuilder()
                    .withName(config.getName())
                    .withNewRbd()
                    .withMonitors(monitors)
                    .withPool(pool)
                    .withFsType(fsType)
                    .withReadOnly(readOnly)
                    .withUser(user)
                    .withKeyring(keyring)
                    .withImage(image)
                    .endRbd()
                    .build();
        }
    }, SCALE_IO("scaleIO") {
        public Volume fromConfig(VolumeConfig config) {
            String gateway = config.getGateway();
            String system = config.getSystem();
            String protectionDomain = config.getProtectionDomain();
            String storagePool = config.getStoragePool();
            String volumeName = config.getVolumeName();
            String secretRef = config.getSecretRef();
            String fsType = config.getFsType();
            Boolean readOnly = config.getReadOnly();

            return new VolumeBuilder()
                    .withName(config.getName())
                    .withNewScaleIO()
                    .withFsType(fsType)
                    .withGateway(gateway)
                    .withReadOnly(readOnly)
                    .withNewSecretRef().withName(secretRef).endSecretRef()
                    .withVolumeName(volumeName)
                    .withStoragePool(storagePool)
                    .withProtectionDomain(protectionDomain)
                    .withSystem(system)
                    .endScaleIO()
                    .build();
        }
    }, STORAGE_OS("storageOS") {
        public Volume fromConfig(VolumeConfig config) {
            String volumeName = config.getVolumeName();
            String fsType = config.getFsType();
            Boolean readOnly = config.getReadOnly();

            return new VolumeBuilder()
                    .withName(config.getName())
                    .withNewStorageos()
                    .withFsType(fsType)
                    .withVolumeName(volumeName)
                    .withReadOnly(readOnly)
                    .endStorageos()
                    .build();
        }
    }, VSPHERE_VOLUME("vsphereVolume") {
        public Volume fromConfig(VolumeConfig config) {
            String path = config.getPath();
            String fsType = config.getFsType();

            return new VolumeBuilder()
                    .withName(config.getName())
                    .withNewVsphereVolume()
                    .withFsType(fsType)
                    .withVolumePath(path)
                    .endVsphereVolume()
                    .build();
        }
    }, DOWNWARD_API("downwardAPI") {
        public Volume fromConfig(VolumeConfig config) {
            List<DownwardAPIVolumeFile> items = config.getItems();
            return new VolumeBuilder()
                    .withName(config.getName())
                    .withNewDownwardAPI()
                    .withItems(items)
                    .endDownwardAPI()
                    .build();
        }
    };

    private final String type;
    public abstract Volume fromConfig(VolumeConfig config);
    VolumeType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    private static final Map<String, VolumeType> VOLUME_TYPES = new HashMap<>();
    static {
        for (VolumeType volumeType : VolumeType.values()) {
            VOLUME_TYPES.put(volumeType.getType(), volumeType);
        }
    }

    public static VolumeType typeFor(String type) {
        return VOLUME_TYPES.get(type);
    }
}

