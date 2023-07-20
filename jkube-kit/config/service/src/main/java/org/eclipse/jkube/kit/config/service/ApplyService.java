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
package org.eclipse.jkube.kit.config.service;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.HasMetadataComparator;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.dsl.base.PatchContext;
import io.fabric8.kubernetes.client.dsl.base.PatchType;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.FileUtil;
import org.eclipse.jkube.kit.common.util.KubernetesHelper;
import org.eclipse.jkube.kit.common.util.OpenshiftHelper;
import org.eclipse.jkube.kit.common.util.Serialization;
import org.eclipse.jkube.kit.common.util.UserConfigurationCompare;
import org.eclipse.jkube.kit.config.service.ingresscontroller.IngressControllerDetectorManager;
import org.eclipse.jkube.kit.config.service.kubernetes.KubernetesClientUtil;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.ReplicationControllerSpec;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.extensions.Ingress;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.openshift.api.model.BuildConfig;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.api.model.ImageStream;
import io.fabric8.openshift.api.model.ImageStreamSpec;
import io.fabric8.openshift.api.model.OAuthClient;
import io.fabric8.openshift.api.model.Project;
import io.fabric8.openshift.api.model.ProjectRequest;
import io.fabric8.openshift.api.model.ProjectRequestBuilder;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.TagReference;
import io.fabric8.openshift.api.model.Template;
import io.fabric8.openshift.client.OpenShiftClient;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.eclipse.jkube.kit.common.util.KubernetesHelper.getKind;
import static org.eclipse.jkube.kit.common.util.KubernetesHelper.getName;
import static org.eclipse.jkube.kit.common.util.KubernetesHelper.getOrCreateLabels;
import static org.eclipse.jkube.kit.common.util.KubernetesHelper.getOrCreateMetadata;
import static org.eclipse.jkube.kit.config.service.kubernetes.KubernetesClientUtil.applicableNamespace;

/**
 * Applies DTOs to the current Kubernetes master
 */
public class ApplyService {

    private final KubernetesClient kubernetesClient;
    private final KitLogger log;

    private boolean allowCreate = true;
    private boolean servicesOnlyMode;
    private boolean ignoreServiceMode;
    private boolean ignoreRunningOAuthClients = true;
    private boolean ignoreBoundPersistentVolumeClaims = true;
    private boolean rollingUpgrade;
    private boolean processTemplatesLocally;
    private File logJsonDir;
    private File basedir;
    private boolean supportOAuthClients;
    private boolean deletePodsOnReplicationControllerUpdate = true;
    private String namespace = KubernetesHelper.getDefaultNamespace();
    private String fallbackNamespace;
    private boolean rollingUpgradePreserveScale = true;
    private boolean recreateMode;
    private final PatchService patchService;
    private final IngressControllerDetectorManager ingressControllerDetectorManager;
    // This map is to track projects created.
    private static final Set<String> projectsCreated = new HashSet<>();

    public ApplyService(JKubeServiceHub serviceHub) {
        this.kubernetesClient = serviceHub.getClient();
        this.log = serviceHub.getLog();
        this.patchService = new PatchService(kubernetesClient);
        this.ingressControllerDetectorManager = new IngressControllerDetectorManager(serviceHub);
    }

    /**
     * Applies the given DTOs onto the Kubernetes master
     */
    public void apply(Object dto, String fileName) {
        if (dto instanceof List) {
            List<Object> list = (List<Object>) dto;
            for (Object element : list) {
                if (dto == element) {
                    log.warn("Found recursive nested object for %s of class: %s", dto, dto.getClass().getName());
                    continue;
                }
                apply(element, fileName);
            }
        } else if (dto instanceof KubernetesList) {
            applyList((KubernetesList) dto, fileName);
        } else if (dto != null) {
            applyEntity(dto, fileName);
        }
    }

    public boolean isAlreadyApplied(HasMetadata resource) {
        return kubernetesClient.resource(resource)
            .inNamespace(applicableNamespace(resource, namespace, fallbackNamespace)).get() != null;
    }

    /**
     * Applies the given DTOs onto the Kubernetes master
     */
    private void applyEntity(Object dto, String sourceName) {
        if (dto instanceof ReplicationController) {
            applyReplicationController((ReplicationController) dto, sourceName);
        } else if (dto instanceof Route) {
            applyRoute((Route) dto, sourceName);
        } else if (dto instanceof BuildConfig) {
            applyBuildConfig((BuildConfig) dto, sourceName);
        } else if (dto instanceof DeploymentConfig) {
            DeploymentConfig resource = (DeploymentConfig) dto;
            if (OpenshiftHelper.isOpenShift(kubernetesClient)) {
                applyResource(resource, sourceName);
            } else {
                log.warn("Not connected to OpenShift cluster so cannot apply entity %s", dto);
            }
        } else if (dto instanceof ImageStream) {
            applyImageStream((ImageStream) dto, sourceName);
        } else if (dto instanceof OAuthClient) {
            applyOAuthClient((OAuthClient) dto, sourceName);
        } else if (dto instanceof Template) {
            applyTemplate((Template) dto, sourceName);
        } else if (dto instanceof Ingress) {
            applyResource((Ingress) dto, sourceName);
            ingressControllerDetectorManager.detect();
        } else if (dto instanceof io.fabric8.kubernetes.api.model.networking.v1.Ingress) {
            applyResource((io.fabric8.kubernetes.api.model.networking.v1.Ingress) dto, sourceName);
            ingressControllerDetectorManager.detect();
        } else if (dto instanceof PersistentVolumeClaim) {
            applyPersistentVolumeClaim((PersistentVolumeClaim) dto, sourceName);
        } else if (dto instanceof Job) {
            applyJob((Job) dto, sourceName);
        } else if (dto instanceof Namespace) {
            applyNamespace((Namespace) dto);
        } else if (dto instanceof Project) {
            applyProject((Project) dto);
        } else if (dto instanceof GenericKubernetesResource) {
            applyGenericKubernetesResource((GenericKubernetesResource) dto, sourceName);
        } else if (dto instanceof HasMetadata) {
            applyResource((HasMetadata) dto, sourceName);
        } else {
            throw new IllegalArgumentException("Unknown entity type " + dto);
        }
    }

    public void applyGenericKubernetesResource(GenericKubernetesResource genericKubernetesResource, String sourceName) {
        String name = genericKubernetesResource.getMetadata().getName();
        String applyNamespace = applicableNamespace(genericKubernetesResource, namespace, fallbackNamespace);
        String apiGroupWithKind = KubernetesHelper.getFullyQualifiedApiGroupWithKind(genericKubernetesResource);
        Objects.requireNonNull(name, "No name for " + genericKubernetesResource + " " + sourceName);

        if (isRecreateMode()) {
            log.info("Attempting to delete Custom Resource: %s %s/%s", apiGroupWithKind, namespace, name);
            KubernetesClientUtil.doDeleteAndWait(kubernetesClient, genericKubernetesResource, applyNamespace, 10L);
        }
        final GenericKubernetesResource existentCR = KubernetesClientUtil.doGetCustomResource(kubernetesClient, genericKubernetesResource, applyNamespace);
        if (existentCR != null && isBlank(existentCR.getMetadata().getDeletionTimestamp())) {
            log.info("Replacing Custom Resource: %s %s/%s",
                apiGroupWithKind, applyNamespace, name);
            genericKubernetesResource.getMetadata().setResourceVersion(existentCR.getMetadata().getResourceVersion());
        }
        kubernetesClient.genericKubernetesResources(genericKubernetesResource.getApiVersion(), genericKubernetesResource.getKind()).inNamespace(applyNamespace).withName(name)
            .createOrReplace(genericKubernetesResource);
        log.info("Created Custom Resource: %s %s/%s", apiGroupWithKind, applyNamespace, name);
    }

    public void applyOAuthClient(OAuthClient entity, String sourceName) {
        if (OpenshiftHelper.isOpenShift(kubernetesClient) && supportOAuthClients) {
            String id = getName(entity);
            Objects.requireNonNull(id, "No name for " + entity + " " + sourceName);
            if (isServicesOnlyMode()) {
                log.debug("Only processing Services right now so ignoring OAuthClient: %s", id);
                return;
            }
            final OpenShiftClient openShiftClient = asOpenShiftClient();
            OAuthClient old = openShiftClient.oAuthClients().withName(id).get();
            if (isRunning(old)) {
                if (isIgnoreRunningOAuthClients()) {
                    log.info("Not updating the OAuthClient which are shared across namespaces as its already running");
                    return;
                }
                if (UserConfigurationCompare.configEqual(entity, old)) {
                    log.info("OAuthClient has not changed so not doing anything");
                } else {
                    if (isRecreateMode()) {
                        openShiftClient.oAuthClients().withName(id).delete();
                        doCreateOAuthClient(entity, sourceName);
                    } else {
                        try {
                            Object answer = openShiftClient.oAuthClients().withName(id).replace(entity);
                            log.info("Updated OAuthClient result: %s", answer);
                        } catch (Exception e) {
                            onApplyError("Failed to update OAuthClient from " + sourceName + ". " + e + ". " + entity, e);
                        }
                    }
                }
            } else {
                if (!isAllowCreate()) {
                    log.warn("Creation disabled so not creating an OAuthClient from %s name %s", sourceName, getName(entity));
                } else {
                    doCreateOAuthClient(entity, sourceName);
                }
            }
        }
    }

    protected void doCreateOAuthClient(OAuthClient entity, String sourceName) {
        if (OpenshiftHelper.isOpenShift(kubernetesClient)) {
            try {
                asOpenShiftClient().oAuthClients().resource(entity).create();
            } catch (Exception e) {
                onApplyError("Failed to create OAuthClient from " + sourceName + ". " + e + ". " + entity, e);
            }
        }
    }

    /**
     * Creates/updates the template and processes it returning the processed DTOs
     */
    public Object applyTemplate(Template entity, String sourceName) {
        installTemplate(entity, sourceName);
        return processTemplate(entity, sourceName);
    }

    /**
     * Installs the template into the namespace without processing it
     */
    public void installTemplate(Template entity, String sourceName) {
        if (!OpenshiftHelper.isOpenShift(kubernetesClient)) {
            // lets not install the template on Kubernetes!
            return;
        }
        if (!isProcessTemplatesLocally()) {
            final OpenShiftClient openShiftClient = asOpenShiftClient();
            String currentNamespace = applicableNamespace(entity, namespace, fallbackNamespace);
            String id = getName(entity);
            Objects.requireNonNull(id, "No name for " + entity + " " + sourceName);
            Template old = openShiftClient.templates().inNamespace(currentNamespace).withName(id).get();
            if (isRunning(old)) {
                if (UserConfigurationCompare.configEqual(entity, old)) {
                    log.info("Template has not changed so not doing anything");
                } else {
                    boolean recreateMode = isRecreateMode();
                    // TODO seems you can't update templates right now
                    recreateMode = true;
                    if (recreateMode) {
                        openShiftClient.templates().inNamespace(currentNamespace).withName(id).delete();
                        doCreateTemplate(entity, currentNamespace, sourceName);
                    } else {
                        log.info("Updating a Template from %s", sourceName);
                        try {
                            Object answer = openShiftClient.templates().inNamespace(currentNamespace).withName(id).replace(entity);
                            log.info("Updated Template: " + answer);
                        } catch (Exception e) {
                            onApplyError("Failed to update Template from " + sourceName + ". " + e + ". " + entity, e);
                        }
                    }
                }
            } else {
                if (!isAllowCreate()) {
                    log.warn("Creation disabled so not creating a Template from %s namespace %s name %s", sourceName, currentNamespace, getName(entity));
                } else {
                    doCreateTemplate(entity, currentNamespace, sourceName);
                }
            }
        }
    }

    private OpenShiftClient asOpenShiftClient() {
        return OpenshiftHelper.asOpenShiftClient(kubernetesClient);
    }

    protected void doCreateTemplate(Template entity, String namespace, String sourceName) {
        if (OpenshiftHelper.isOpenShift(kubernetesClient)) {
            log.info("Creating a Template from " + sourceName + " namespace " + namespace + " name " + getName(entity));
            try {
                final Template answer = asOpenShiftClient().templates().inNamespace(namespace).create(entity);
                logGeneratedEntity("Created Template: ", namespace, entity, answer);
            } catch (Exception e) {
                onApplyError("Failed to Template entity from " + sourceName + ". " + e + ". " + entity, e);
            }
        }
    }

    public void applyPersistentVolumeClaim(PersistentVolumeClaim entity, String sourceName) {
        // we cannot update PVCs
        boolean alwaysRecreate = true;
        String currentNamespace = applicableNamespace(entity, namespace, fallbackNamespace);
        String id = getName(entity);
        Objects.requireNonNull(id, "No name for " + entity + " " + sourceName);
        if (isServicesOnlyMode()) {
            log.debug("Only processing Services right now so ignoring PersistentVolumeClaim: " + id);
            return;
        }
        PersistentVolumeClaim old = kubernetesClient.persistentVolumeClaims().inNamespace(currentNamespace).withName(id).get();
        if (isRunning(old)) {
            if (UserConfigurationCompare.configEqual(entity, old)) {
                log.info("PersistentVolumeClaim has not changed so not doing anything");
            } else {
                if (alwaysRecreate || isRecreateMode()) {
                    if (!isRecreateMode() && isIgnoreBoundPersistentVolumeClaims() && isBound(old)) {
                        log.warn("PersistentVolumeClaim " + id + " in namespace " + currentNamespace + " is already bound and will not be replaced with the new one from " + sourceName);
                    } else {
                        log.info("Deleting PersistentVolumeClaim from namespace " + currentNamespace + " with name " + id);
                        kubernetesClient.persistentVolumeClaims().inNamespace(currentNamespace).withName(id).delete();
                        log.info("Deleted PersistentVolumeClaim from namespace " + currentNamespace + " with name " + id);

                        doCreate(entity, currentNamespace, sourceName);
                    }
                } else {
                    doPatchEntity(old, entity, currentNamespace, sourceName);
                }
            }
        } else {
            if (!isAllowCreate()) {
                log.warn("Creation disabled so not creating a %s from %s in namespace %s with name %s",
                  "PersistentVolumeClaim", sourceName, currentNamespace, id);
            } else {
                doCreate(entity, currentNamespace, sourceName);
            }
        }
    }

    protected boolean isBound(PersistentVolumeClaim claim) {
        return claim != null &&
                claim.getStatus() != null &&
                "Bound".equals(claim.getStatus().getPhase());
    }

    private void logGeneratedEntity(String message, String namespace, HasMetadata entity, Object result) {
        if (logJsonDir != null) {
            final File directory = StringUtils.isBlank(namespace) ? logJsonDir : new File(logJsonDir, namespace);
            directory.mkdirs();
            String kind = getKind(entity);
            String name = getName(entity);
            if (isNotBlank(kind)) {
                name = kind.toLowerCase() + "-" + name;
            }
            if (StringUtils.isBlank(name)) {
                log.warn("No name for the entity " + entity);
            } else {
                String fileName = name + ".json";
                File file = new File(directory, fileName);
                if (file.exists()) {
                    int idx = 1;
                    while (true) {
                        fileName = name + "-" + idx++ + ".json";
                        file = new File(directory, fileName);
                        if (!file.exists()) {
                            break;
                        }
                    }
                }
                String text;
                if (result instanceof String) {
                    text = result.toString();
                } else {
                    try {
                        text = Serialization.asJson(result);
                    } catch (Exception e) {
                        log.warn("Cannot convert " + result + " to JSON: " + e, e);
                        if (result != null) {
                            text = result.toString();
                        } else {
                            text = "null";
                        }
                    }
                }
                try {
                    FileUtils.writeStringToFile(file, text, Charset.defaultCharset());
                    Object fileLocation = file;
                    if (basedir != null) {
                        String path = FileUtil.getRelativePath(basedir, file).getPath();
                        if (path != null) {
                            fileLocation = FileUtil.stripPrefix(path, "/");
                        }
                    }
                    log.info(message + fileLocation);
                } catch (IOException e) {
                    log.warn("Failed to write to file " + file + ". " + e, e);
                }
                return;
            }
        }
        log.info(message + result);
    }

    public Object processTemplate(Template entity, String sourceName) {
            try {
                return OpenshiftHelper.processTemplatesLocally(entity, false);
            } catch (Exception e) {
                onApplyError("Failed to process template " + sourceName + ". " + e + ". " + entity, e);
                return null;
            }
    }

    public void applyRoute(Route entity, String sourceName) {
        if (OpenshiftHelper.isOpenShift(kubernetesClient)) {
            String id = getName(entity);
            Objects.requireNonNull(id, "No name for " + entity + " " + sourceName);
            String currentNamespace = applicableNamespace(entity, namespace, fallbackNamespace);
            if (isServicesOnlyMode()) {
                log.debug("Ignoring Route: " + currentNamespace + ":" + id);
                return;
            }
            final OpenShiftClient openShiftClient = asOpenShiftClient();
            Route route = openShiftClient.routes().inNamespace(currentNamespace).withName(id).get();
            if (isRunning(route)) {
                if (UserConfigurationCompare.configEqual(entity, route)) {
                    log.info("Route has not changed so not doing anything");
                } else {
                    if (isRecreateMode()) {
                        log.info("Deleting Route: " + id);
                        openShiftClient.routes().inNamespace(currentNamespace).withName(id).delete();
                        doCreateRoute(entity, currentNamespace, sourceName);
                    } else {
                        doPatchEntity(route, entity, currentNamespace, sourceName);
                    }
                }
            } else {
                if (!isAllowCreate()) {
                    log.warn("Creation disabled so not creating a Route from " + sourceName + " namespace " + currentNamespace + " name " + id);
                } else {
                    doCreateRoute(entity, currentNamespace, sourceName);
                }
            }
        }
    }

    private void doCreateRoute(Route entity, String namespace, String sourceName) {
        if (OpenshiftHelper.isOpenShift(kubernetesClient)) {
            String id = getName(entity);
            try {
                log.info("Creating Route " + namespace + ":" + id + " " +
                    (entity.getSpec() != null ?
                        "host: " + entity.getSpec().getHost() :
                        "No Spec !"));
                asOpenShiftClient().routes().inNamespace(namespace).resource(entity).create();
            } catch (Exception e) {
                onApplyError("Failed to create Route from " + sourceName + ". " + e + ". " + entity, e);
            }
        }
    }

    public void applyBuildConfig(BuildConfig entity, String sourceName) {
        if (OpenshiftHelper.isOpenShift(kubernetesClient)) {
            String id = getName(entity);

            Objects.requireNonNull(id, "No name for " + entity + " " + sourceName);
            String currentNamespace = applicableNamespace(entity, namespace, fallbackNamespace);
            applyNamespace(currentNamespace);
            final OpenShiftClient openShiftClient = asOpenShiftClient();
            BuildConfig old = openShiftClient.buildConfigs().inNamespace(currentNamespace).withName(id).get();
            if (isRunning(old)) {
                if (UserConfigurationCompare.configEqual(entity, old)) {
                    log.info("BuildConfig has not changed so not doing anything");
                } else {
                    if (isRecreateMode()) {
                        log.info("Deleting BuildConfig: " + id);
                        openShiftClient.buildConfigs().inNamespace(currentNamespace).withName(id).delete();
                        doCreateBuildConfig(entity, currentNamespace, sourceName);
                    } else {
                        doPatchEntity(old, entity, currentNamespace, sourceName);
                    }
                }
            } else {
                if (!isAllowCreate()) {
                    log.warn("Creation disabled so not creating BuildConfig from " + sourceName + " namespace " + currentNamespace + " name " + getName(entity));
                } else {
                    doCreateBuildConfig(entity, currentNamespace, sourceName);
                }
            }
        }
    }

    public void doCreateBuildConfig(BuildConfig entity, String namespace , String sourceName) {
        if (OpenshiftHelper.isOpenShift(kubernetesClient)) {
            try {
                asOpenShiftClient().buildConfigs().inNamespace(namespace).resource(entity).create();
            } catch (Exception e) {
                onApplyError("Failed to create BuildConfig from " + sourceName + ". " + e, e);
            }
        }
    }

    public void applyImageStream(ImageStream entity, String sourceName) {
        if (OpenshiftHelper.isOpenShift(kubernetesClient)) {
            String kind = getKind(entity);
            String name = getName(entity);
            String currentNamespace = applicableNamespace(entity, namespace, fallbackNamespace);
            try {
                final OpenShiftClient openShiftClient = asOpenShiftClient();
                Resource<ImageStream> resource = openShiftClient.imageStreams().inNamespace(currentNamespace).withName(name);
                ImageStream old = resource.get();
                if (old == null) {
                    log.info("Creating " + kind + " " + name + " from " + sourceName);
                    resource.create(entity);
                } else {
                    log.info("Updating " + kind + " " + name + " from " + sourceName);
                    copyAllImageStreamTags(entity, old);
                    entity = patchService.compareAndPatchEntity(currentNamespace, entity, old);
                    openShiftClient.resource(entity).inNamespace(currentNamespace).createOrReplace();
                }
            } catch (Exception e) {
                onApplyError("Failed to create " + kind + " from " + sourceName + ". " + e, e);
            }
        }
    }

    protected void copyAllImageStreamTags(ImageStream from, ImageStream to) {
        ImageStreamSpec toSpec = to.getSpec();
        if (toSpec == null) {
            toSpec = new ImageStreamSpec();
            to.setSpec(toSpec);
        }
        List<TagReference> toTags = toSpec.getTags();
        if (toTags == null) {
            toTags = new ArrayList<>();
            toSpec.setTags(toTags);
        }

        ImageStreamSpec fromSpec = from.getSpec();
        if (fromSpec != null) {
            List<TagReference> fromTags = fromSpec.getTags();
            if (fromTags != null) {
                // lets remove all the tags with these names first
                for (TagReference tag : fromTags) {
                    removeTagByName(toTags, tag.getName());
                }

                // now lets add them all in case 2 tags have the same name
                for (TagReference tag : fromTags) {
                    toTags.add(tag);
                }
            }
        }
    }

    /**
     * Removes all the tags with the given name
     * @return the number of tags removed
     */
    private int removeTagByName(List<TagReference> tags, String tagName) {
        List<TagReference> removeTags = new ArrayList<>();
        for (TagReference tag : tags) {
            if (Objects.equals(tagName, tag.getName())) {
                removeTags.add(tag);
            }
        }
        tags.removeAll(removeTags);
        return removeTags.size();
    }


    public void applyList(KubernetesList list, String sourceName) {
        List<HasMetadata> entities = list.getItems();
        if (entities != null) {
            for (Object entity : entities) {
                applyEntity(entity, sourceName);
            }
        }
    }

    public <T extends HasMetadata> void applyResource(T resource, String sourceName) {
        String currentNamespace = applicableNamespace(resource, namespace, fallbackNamespace);
        String id = getName(resource);
        String kind = getKind(resource);
        Objects.requireNonNull(id, "No name for " + resource + " " + sourceName);
        if (isServicesOnlyMode() && !(resource instanceof Service)) {
            log.debug("Ignoring " + kind + ": " + currentNamespace + ":" + id);
            return;
        }
        T old = kubernetesClient.resource(resource).inNamespace(currentNamespace).get();
        if (isRunning(old)) {
            if (UserConfigurationCompare.configEqual(resource, old)) {
                log.info(kind + " has not changed so not doing anything");
            } else {
                if (isRecreateMode()) {
                    log.info("Deleting " + kind + ": " + id);
                    kubernetesClient.resource(resource).inNamespace(currentNamespace).delete();
                    doCreate(resource, currentNamespace, sourceName);
                } else {
                    log.info("Updating " + kind + " from " + sourceName);
                    try {
                        T updatedResource = kubernetesClient
                          .resource(resource)
                          .inNamespace(currentNamespace)
                          .patch();
                        logGeneratedEntity("Updated " + kind + ": ", currentNamespace, resource, updatedResource);
                    } catch (Exception e) {
                        onApplyError("Failed to update " + kind + " from " + sourceName + ". " + e + ". " + resource, e);
                    }
                }
            }
        } else {
            if (!isAllowCreate() && resource instanceof Namespaced) {
                log.warn("Creation disabled so not creating a %s from %s in namespace %s with name %s",
                  kind, sourceName, currentNamespace, id);
            } else if (!isAllowCreate()) {
                log.warn("Creation disabled so not creating a %s from %s with name %s",
                  kind, sourceName, id);
            } else {
                doCreate(resource, currentNamespace, sourceName);
            }
        }
    }

    private <T extends HasMetadata> void doPatchEntity(T oldEntity, T newEntity, String namespace, String sourceName) {
        String kind = newEntity.getKind();
        log.info("Updating %s from %s", kind, sourceName);
        try {
            Object answer = patchService.compareAndPatchEntity(namespace, newEntity, oldEntity);
            logGeneratedEntity("Updated " + kind + ": ", namespace, newEntity, answer);
        } catch (Exception e) {
            onApplyError("Failed to update " + kind + " from " + sourceName + ". " + e + ". " + newEntity, e);
        }
    }

    public boolean checkNamespace(String namespaceName) {
        if (StringUtils.isBlank(namespaceName)) {
            return false;
        }
        if (OpenshiftHelper.isOpenShift(kubernetesClient)) {
            // It is preferable to iterate on the list of projects as regular user with the 'basic-role' bound
            // are not granted permission get operation on non-existing project resource that returns 403
            // instead of 404. Only more privileged roles like 'view' or 'cluster-reader' are granted this permission.
            List<Project> projects = asOpenShiftClient().projects().list().getItems();
            for (Project project : projects) {
                if (namespaceName.equals(project.getMetadata().getName())) {
                    return true;
                }
            }
            return false;
        }
        else {
            return kubernetesClient.namespaces().withName(namespaceName).get() != null;
        }
    }

    public void applyNamespace(String namespaceName) {
        applyNamespace(namespaceName, null);

    }
    public void applyNamespace(String namespaceName, Map<String,String> labels) {
        if (StringUtils.isBlank(namespaceName)) {
            return;
        }
        if (OpenshiftHelper.isOpenShift(kubernetesClient)) {
            ProjectRequest entity = new ProjectRequest();
            ObjectMeta metadata = getOrCreateMetadata(entity);
            metadata.setName(namespaceName);
            String kubernetesClientNamespace = asOpenShiftClient().getNamespace();
            if (isNotBlank(kubernetesClientNamespace)) {
                Map<String, String> entityLabels = getOrCreateLabels(entity);
                if (labels != null) {
                    entityLabels.putAll(labels);
                } else {
                    // lets associate this new namespace with the project that it was created from
                    entityLabels.put("project", kubernetesClientNamespace);
                }
            }
            applyProjectRequest(entity);
        }
        else {
            Namespace entity = new Namespace();
            ObjectMeta metadata = getOrCreateMetadata(entity);
            metadata.setName(namespaceName);
            String kubernetesClientNamespace = kubernetesClient.getNamespace();
            if (isNotBlank(kubernetesClientNamespace)) {
                Map<String, String> entityLabels = getOrCreateLabels(entity);
                if (labels != null) {
                    entityLabels.putAll(labels);
                } else {
                    // lets associate this new namespace with the project that it was created from
                    entityLabels.put("project", kubernetesClientNamespace);
                }
            }
            applyNamespace(entity);
        }
    }

    /**
     * Returns true if the namespace is created
     */
    public boolean applyNamespace(Namespace entity) {
        String currentNamespace = getOrCreateMetadata(entity).getName();
        log.info("Creating currentNamespace: " + currentNamespace);
        String name = getName(entity);
        Objects.requireNonNull(name, "No name for " + entity );
        Namespace old = kubernetesClient.namespaces().withName(name).get();
        if (!isRunning(old)) {
            try {
                Object answer = kubernetesClient.namespaces().create(entity);
                logGeneratedEntity("Created Namespace: ", currentNamespace, entity, answer);
                return true;
            } catch (Exception e) {
                onApplyError("Failed to create Namespace: " + name + " due " + e.getMessage(), e);
            }
        }
        return false;
    }

    /**
     * Creates an OpenShift Project
     */
    public void applyProject(Project project) {
        applyProjectRequest(new ProjectRequestBuilder()
            .withDisplayName(project.getMetadata().getName())
            .withMetadata(project.getMetadata()).build());
    }

    /**
     * Returns true if the ProjectRequest is created
     */
    public void applyProjectRequest(ProjectRequest entity) {
        final String projectName = getOrCreateMetadata(entity).getName();
        Objects.requireNonNull(projectName, "No name for " + entity);
        // Check whether project creation attempted before
        if (projectsCreated.contains(projectName)) {
            return;
        }
        log.info("Creating project: " + projectName);
        if (!OpenshiftHelper.isOpenShift(kubernetesClient)) {
            log.warn("Cannot check for Project " + projectName + " as not running against OpenShift!");
            return;
        }
        boolean exists = checkNamespace(projectName);
        // We may want to be more fine-grained on the phase of the project
        if (!exists) {
            try {
                Object answer = asOpenShiftClient().projectrequests().create(entity);
                // Add project to created projects
                projectsCreated.add(projectName);
                logGeneratedEntity("Created ProjectRequest: ", projectName, entity, answer);
            } catch (Exception e) {
                onApplyError("Failed to create ProjectRequest: " + projectName + " due " + e.getMessage(), e);
            }
        }
    }

    private void doCreate(HasMetadata resource, String namespace, String fileName) {
        final String kind = getKind(resource);
        try {
            final Object answer;
            if (resource instanceof Namespaced) {
                log.info("Creating a %s in %s namespace with name %s from %s",
                  kind, namespace, getName(resource), fileName);
                answer = kubernetesClient.resource(resource).inNamespace(namespace).create();
            } else {
                log.info("Creating a %s with name %s from %s", kind, getName(resource), fileName);
                answer = kubernetesClient.resource(resource).create();
            }
            logGeneratedEntity("Created " + kind + ": ", namespace, resource, answer);
        } catch (Exception e) {
            onApplyError("Failed to create " + kind + " from " + fileName + ". " + e + ". " + resource, e);
        }
    }

    public void applyReplicationController(ReplicationController replicationController, String sourceName) {
        String currentNamespace = applicableNamespace(replicationController, namespace, fallbackNamespace);
        String id = getName(replicationController);
        Objects.requireNonNull(id, "No name for " + replicationController + " " + sourceName);
        if (isServicesOnlyMode()) {
            log.debug("Only processing Services right now so ignoring ReplicationController: " + currentNamespace + ":" + id);
            return;
        }
        ReplicationController old = kubernetesClient.replicationControllers().inNamespace(currentNamespace).withName(id).get();
        if (isRunning(old)) {
            if (UserConfigurationCompare.configEqual(replicationController, old)) {
                log.info("ReplicationController has not changed so not doing anything");
            } else {
                ReplicationControllerSpec newSpec = replicationController.getSpec();
                ReplicationControllerSpec oldSpec = old.getSpec();
                if (rollingUpgrade) {
                    log.info("Rolling upgrade of the ReplicationController: " + currentNamespace + "/" + id);
                    // lets preserve the number of replicas currently running in the environment we are about to upgrade
                    if (rollingUpgradePreserveScale && newSpec != null && oldSpec != null) {
                        Integer replicas = oldSpec.getReplicas();
                        if (replicas != null) {
                            newSpec.setReplicas(replicas);
                        }
                    }
                    log.info("rollingUpgradePreserveScale " + rollingUpgradePreserveScale + " new replicas is " + (newSpec != null ? newSpec.getReplicas() : "<null>"));
                    kubernetesClient.replicationControllers()
                        .inNamespace(currentNamespace).withName(id)
                        .patch(PatchContext.of(PatchType.SERVER_SIDE_APPLY), replicationController);
                } else if (isRecreateMode()) {
                    log.info("Deleting ReplicationController: " + id);
                    kubernetesClient.replicationControllers().inNamespace(currentNamespace).withName(id).delete();
                    doCreate(replicationController, currentNamespace, sourceName);
                } else {
                    log.info("Updating ReplicationController from " + sourceName + " namespace " + currentNamespace + " name " + getName(replicationController));
                    try {
                        Object answer = patchService.compareAndPatchEntity(currentNamespace, replicationController, old);
                        logGeneratedEntity("Updated replicationController: ", currentNamespace, replicationController, answer);

                        if (deletePodsOnReplicationControllerUpdate) {
                            kubernetesClient.pods().inNamespace(currentNamespace).withLabels(newSpec.getSelector()).delete();
                            log.info("Deleting any pods for the replication controller to ensure they use the new configuration");
                        } else {
                            log.info("Warning not deleted any pods so they could well be running with the old configuration!");
                        }
                    } catch (Exception e) {
                        onApplyError("Failed to update ReplicationController from " + sourceName + ". " + e + ". " + replicationController, e);
                    }
                }
            }
        } else {
            if (!isAllowCreate()) {
                log.warn("Creation disabled so not creating a %s from %s in namespace %s with name %s",
                  "ReplicationController", sourceName, currentNamespace, id);
            } else {
                doCreate(replicationController, currentNamespace, sourceName);
            }
        }
    }

    protected void applyJob(Job job, String sourceName) {
        String currentNamespace = applicableNamespace(job, namespace, fallbackNamespace);
        String id = getName(job);
        Objects.requireNonNull(id, "No name for " + job + " " + sourceName);
        if (isServicesOnlyMode()) {
            log.debug("Only processing Services right now so ignoring Job: " + currentNamespace + ":" + id);
            return;
        }
        // Not using createOrReplace() here (https://github.com/fabric8io/kubernetes-client/issues/1586)
        try {
            log.info("Creating a Job from " + sourceName + " namespace " + namespace + " name " + getName(job));
            kubernetesClient.batch().v1().jobs().inNamespace(namespace).resource(job).create();
        } catch (KubernetesClientException exception) {
            if(exception.getStatus().getCode().equals(HttpURLConnection.HTTP_CONFLICT)) {
                Job old = kubernetesClient.batch().v1().jobs().inNamespace(currentNamespace).withName(id).get();
                Job updatedJob = patchService.compareAndPatchEntity(currentNamespace, job, old);
                log.info("Updated Job: " + updatedJob.getMetadata().getName());
                return;
            }
            onApplyError("Failed to apply Job from " + job.getMetadata().getName(), exception);
        }
    }

    public String getNamespace() {
        return namespace;
    }

    public String getFallbackNamespace() {
        return fallbackNamespace;
    }

    /**
     * Returns the namespace defined in the entity or the configured namespace
     */
    protected String getNamespace(HasMetadata entity) {
        String answer = KubernetesHelper.getNamespace(entity);
        if (StringUtils.isBlank(answer)) {
            answer = getNamespace();
        }
        // lest make sure the namespace exists
        applyNamespace(answer);
        return answer;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public void setFallbackNamespace(String namespace) {
        this.fallbackNamespace = namespace;
    }

    public boolean isProcessTemplatesLocally() {
        return processTemplatesLocally;
    }

    public void setProcessTemplatesLocally(boolean processTemplatesLocally) {
        this.processTemplatesLocally = processTemplatesLocally;
    }

    public void setDeletePodsOnReplicationControllerUpdate(boolean deletePodsOnReplicationControllerUpdate) {
        this.deletePodsOnReplicationControllerUpdate = deletePodsOnReplicationControllerUpdate;
    }

    /**
     * Lets you configure the directory where JSON logging files should go
     */
    public void setLogJsonDir(File logJsonDir) {
        this.logJsonDir = logJsonDir;
    }

    public File getBasedir() {
        return basedir;
    }

    public void setBasedir(File basedir) {
        this.basedir = basedir;
    }

    protected boolean isRunning(HasMetadata entity) {
        return entity != null;
    }


    /**
     * Logs an error applying some JSON to Kubernetes and optionally throws an exception
     */
    protected void onApplyError(String message, Exception e) {
        log.error(message, e);
        throw new RuntimeException(message, e);
    }

    /**
     * Returns true if this controller allows new resources to be created in the given namespace
     */
    public boolean isAllowCreate() {
        return allowCreate;
    }

    public void setAllowCreate(boolean allowCreate) {
        this.allowCreate = allowCreate;
    }

    /**
     * If enabled then updates are performed by deleting the resource first then creating it
     */
    public boolean isRecreateMode() {
        return recreateMode;
    }

    public void setRecreateMode(boolean recreateMode) {
        this.recreateMode = recreateMode;
    }

    public void setServicesOnlyMode(boolean servicesOnlyMode) {
        this.servicesOnlyMode = servicesOnlyMode;
    }

    /**
     * If enabled then only services are created/updated to allow services to be created/updated across
     * a number of apps before any pods/replication controllers are updated
     */
    public boolean isServicesOnlyMode() {
        return servicesOnlyMode;
    }

    /**
     * If enabled then all services are ignored to avoid them being recreated. This is useful if you want to
     * recreate ReplicationControllers and Pods but leave Services as they are to avoid the clusterIP addresses
     * changing
     */
    public boolean isIgnoreServiceMode() {
        return ignoreServiceMode;
    }

    public void setIgnoreServiceMode(boolean ignoreServiceMode) {
        this.ignoreServiceMode = ignoreServiceMode;
    }

    public boolean isIgnoreRunningOAuthClients() {
        return ignoreRunningOAuthClients;
    }

    public void setIgnoreRunningOAuthClients(boolean ignoreRunningOAuthClients) {
        this.ignoreRunningOAuthClients = ignoreRunningOAuthClients;
    }

    /**
     * If enabled, persistent volume claims are not replaced (deleted and recreated) if already bound
     */
    public boolean isIgnoreBoundPersistentVolumeClaims() {
        return ignoreBoundPersistentVolumeClaims;
    }

    /**
     * Do not replace (delete and recreate) persistent volume claims if already bound
     */
    public void setIgnoreBoundPersistentVolumeClaims(boolean ignoreBoundPersistentVolumeClaims) {
        this.ignoreBoundPersistentVolumeClaims = ignoreBoundPersistentVolumeClaims;
    }

    public void setSupportOAuthClients(boolean supportOAuthClients) {
        this.supportOAuthClients = supportOAuthClients;
    }

    public void setRollingUpgrade(boolean rollingUpgrade) {
        this.rollingUpgrade = rollingUpgrade;
    }

    public void setRollingUpgradePreserveScale(boolean rollingUpgradePreserveScale) {
        this.rollingUpgradePreserveScale = rollingUpgradePreserveScale;
    }

    public void applyEntities(String fileName, Collection<HasMetadata> entities) {
        getK8sListWithNamespaceFirst(entities).forEach(entity -> applyEntity(entity, fileName));
    }

    public static List<HasMetadata> getK8sListWithNamespaceFirst(Collection<HasMetadata> k8sList) {
        return k8sList.stream().sorted(new HasMetadataComparator()).sorted((k1, k2) -> {
            if (isNamespaceOrProject(k1)) {
                return -1;
            } else if (isNamespaceOrProject(k2)) {
                return 1;
            }
            return 0;
        }).collect(Collectors.toList());
    }

    private static boolean isNamespaceOrProject(HasMetadata h) {
        return h instanceof Namespace || h instanceof Project;
    }
}
