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
package org.eclipse.jkube.kit.config.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.ReplicationControllerSpec;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apps.DaemonSet;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.ReplicaSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.batch.Job;
import io.fabric8.kubernetes.api.model.extensions.Ingress;
import io.fabric8.kubernetes.api.model.rbac.Role;
import io.fabric8.kubernetes.api.model.rbac.RoleBinding;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.fabric8.openshift.api.model.BuildConfig;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.api.model.DoneableImageStream;
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
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.FileUtil;
import org.eclipse.jkube.kit.common.util.KubernetesHelper;
import org.eclipse.jkube.kit.common.util.OpenshiftHelper;
import org.eclipse.jkube.kit.common.util.ResourceUtil;
import org.eclipse.jkube.kit.common.util.UserConfigurationCompare;
import org.eclipse.jkube.kit.config.service.kubernetes.KubernetesClientUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.eclipse.jkube.kit.common.util.KubernetesHelper.getKind;
import static org.eclipse.jkube.kit.common.util.KubernetesHelper.getName;
import static org.eclipse.jkube.kit.common.util.KubernetesHelper.getOrCreateLabels;
import static org.eclipse.jkube.kit.common.util.KubernetesHelper.getOrCreateMetadata;


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
    private boolean rollingUpgradePreserveScale = true;
    private boolean recreateMode;
    private PatchService patchService;
    // This map is to track projects created.
    private static Set<String> projectsCreated = new HashSet<>();

    public ApplyService(KubernetesClient kubernetesClient, KitLogger log) {
        this.kubernetesClient = kubernetesClient;
        this.patchService = new PatchService(kubernetesClient, log);
        this.log = log;
    }

    /**
     * Applies the given DTOs onto the Kubernetes master
     */
    public void apply(Object dto, String sourceName) throws Exception {
        if (dto instanceof List) {
            List list = (List) dto;
            for (Object element : list) {
                if (dto == element) {
                    log.warn("Found recursive nested object for " + dto + " of class: " + dto.getClass().getName());
                    continue;
                }
                apply(element, sourceName);
            }
        } else if (dto instanceof KubernetesList) {
            applyList((KubernetesList) dto, sourceName);
        } else if (dto != null) {
            applyEntity(dto, sourceName);
        }
    }

    /**
     * Applies the given DTOs onto the Kubernetes master
     */
    private void applyEntity(Object dto, String sourceName) throws Exception {
        if (dto instanceof Pod) {
            applyPod((Pod) dto, sourceName);
        } else if (dto instanceof ReplicationController) {
            applyReplicationController((ReplicationController) dto, sourceName);
        } else if (dto instanceof Service) {
            applyService((Service) dto, sourceName);
        } else if (dto instanceof Route) {
            applyRoute((Route) dto, sourceName);
        } else if (dto instanceof BuildConfig) {
            applyBuildConfig((BuildConfig) dto, sourceName);
        } else if (dto instanceof DeploymentConfig) {
            DeploymentConfig resource = (DeploymentConfig) dto;
            OpenShiftClient openShiftClient = getOpenShiftClient();
            if (openShiftClient != null) {
                applyResource(resource, sourceName, openShiftClient.deploymentConfigs());
            } else {
                log.warn("Not connected to OpenShift cluster so cannot apply entity " + dto);
            }
        } else if (dto instanceof RoleBinding) {
            applyRoleBinding((RoleBinding) dto, sourceName);
        } else if (dto instanceof Role) {
            applyResource((Role)dto, sourceName, kubernetesClient.rbac().roles());
        } else if (dto instanceof ImageStream) {
            applyImageStream((ImageStream) dto, sourceName);
        } else if (dto instanceof OAuthClient) {
            applyOAuthClient((OAuthClient) dto, sourceName);
        } else if (dto instanceof Template) {
            applyTemplate((Template) dto, sourceName);
        } else if (dto instanceof ServiceAccount) {
            applyServiceAccount((ServiceAccount) dto, sourceName);
        } else if (dto instanceof Secret) {
            applySecret((Secret) dto, sourceName);
        } else if (dto instanceof ConfigMap) {
            applyResource((ConfigMap) dto, sourceName, kubernetesClient.configMaps());
        } else if (dto instanceof DaemonSet) {
            applyResource((DaemonSet) dto, sourceName, kubernetesClient.apps().daemonSets());
        } else if (dto instanceof Deployment) {
            applyResource((Deployment) dto, sourceName, kubernetesClient.apps().deployments());
        } else if (dto instanceof ReplicaSet) {
            applyResource((ReplicaSet) dto, sourceName, kubernetesClient.apps().replicaSets());
        } else if (dto instanceof StatefulSet) {
            applyResource((StatefulSet) dto, sourceName, kubernetesClient.apps().statefulSets());
        } else if (dto instanceof Ingress) {
            applyResource((Ingress) dto, sourceName, kubernetesClient.extensions().ingresses());
        } else if (dto instanceof PersistentVolumeClaim) {
            applyPersistentVolumeClaim((PersistentVolumeClaim) dto, sourceName);
        }else if (dto instanceof CustomResourceDefinition) {
            applyCustomResourceDefinition((CustomResourceDefinition) dto, sourceName);
        } else if (dto instanceof Job) {
            applyJob((Job) dto, sourceName);
        } else if (dto instanceof HasMetadata) {
            HasMetadata entity = (HasMetadata) dto;
            try {
                log.info("Applying " + getKind(entity) + " " + getName(entity) + " from " + sourceName);
                kubernetesClient.resource(entity).inNamespace(getNamespace()).createOrReplace();
            } catch (Exception e) {
                onApplyError("Failed to create " + getKind(entity) + " from " + sourceName + ". " + e, e);
            }
        } else {
            throw new IllegalArgumentException("Unknown entity type " + dto);
        }
    }

    public void applyOAuthClient(OAuthClient entity, String sourceName) {
        OpenShiftClient openShiftClient = getOpenShiftClient();
        if (openShiftClient != null) {
            if (supportOAuthClients) {
                String id = getName(entity);
                Objects.requireNonNull(id, "No name for " + entity + " " + sourceName);
                if (isServicesOnlyMode()) {
                    log.debug("Only processing Services right now so ignoring OAuthClient: " + id);
                    return;
                }
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
                                log.info("Updated OAuthClient result: " + answer);
                            } catch (Exception e) {
                                onApplyError("Failed to update OAuthClient from " + sourceName + ". " + e + ". " + entity, e);
                            }
                        }
                    }
                } else {
                    if (!isAllowCreate()) {
                        log.warn("Creation disabled so not creating an OAuthClient from " + sourceName + " name " + getName(entity));
                    } else {
                        doCreateOAuthClient(entity, sourceName);
                    }
                }
            }
        }
    }

    protected void doCreateOAuthClient(OAuthClient entity, String sourceName) {
        OpenShiftClient openShiftClient = getOpenShiftClient();
        if (openShiftClient != null) {
            Object result = null;
            try {
                result = openShiftClient.oAuthClients().create(entity);
            } catch (Exception e) {
                onApplyError("Failed to create OAuthClient from " + sourceName + ". " + e + ". " + entity, e);
            }
        }
    }

    /**
     * Creates/updates the template and processes it returning the processed DTOs
     */
    public Object applyTemplate(Template entity, String sourceName) throws Exception {
        installTemplate(entity, sourceName);
        return processTemplate(entity, sourceName);
    }

    /**
     * Installs the template into the namespace without processing it
     */
    public void installTemplate(Template entity, String sourceName) {
        OpenShiftClient openShiftClient = getOpenShiftClient();
        if (openShiftClient == null) {
            // lets not install the template on Kubernetes!
            return;
        }
        if (!isProcessTemplatesLocally()) {
            String namespace = getNamespace();
            String id = getName(entity);
            Objects.requireNonNull(id, "No name for " + entity + " " + sourceName);
            Template old = openShiftClient.templates().inNamespace(namespace).withName(id).get();
            if (isRunning(old)) {
                if (UserConfigurationCompare.configEqual(entity, old)) {
                    log.info("Template has not changed so not doing anything");
                } else {
                    boolean recreateMode = isRecreateMode();
                    // TODO seems you can't update templates right now
                    recreateMode = true;
                    if (recreateMode) {
                        openShiftClient.templates().inNamespace(namespace).withName(id).delete();
                        doCreateTemplate(entity, namespace, sourceName);
                    } else {
                        log.info("Updating a Template from " + sourceName);
                        try {
                            Object answer = openShiftClient.templates().inNamespace(namespace).withName(id).replace(entity);
                            log.info("Updated Template: " + answer);
                        } catch (Exception e) {
                            onApplyError("Failed to update Template from " + sourceName + ". " + e + ". " + entity, e);
                        }
                    }
                }
            } else {
                if (!isAllowCreate()) {
                    log.warn("Creation disabled so not creating a Template from " + sourceName + " namespace " + namespace + " name " + getName(entity));
                } else {
                    doCreateTemplate(entity, namespace, sourceName);
                }
            }
        }
    }

    public OpenShiftClient getOpenShiftClient() {
        return OpenshiftHelper.asOpenShiftClient(kubernetesClient);
    }

    protected void doCreateTemplate(Template entity, String namespace, String sourceName) {
        OpenShiftClient openShiftClient = getOpenShiftClient();
        if (openShiftClient != null) {
            log.info("Creating a Template from " + sourceName + " namespace " + namespace + " name " + getName(entity));
            try {
                Object answer = openShiftClient.templates().inNamespace(namespace).create(entity);
                logGeneratedEntity("Created Template: ", namespace, entity, answer);
            } catch (Exception e) {
                onApplyError("Failed to Template entity from " + sourceName + ". " + e + ". " + entity, e);
            }
        }
    }

    /**
     * Creates/updates a service account and processes it returning the processed DTOs
     */
    public void applyServiceAccount(ServiceAccount serviceAccount, String sourceName) throws Exception {
        String namespace = getNamespace();
        String id = getName(serviceAccount);
        Objects.requireNonNull(id, "No name for " + serviceAccount + " " + sourceName);
        if (isServicesOnlyMode()) {
            log.debug("Only processing Services right now so ignoring ServiceAccount: " + id);
            return;
        }
        ServiceAccount old = kubernetesClient.serviceAccounts().inNamespace(namespace).withName(id).get();
        if (isRunning(old)) {
            if (UserConfigurationCompare.configEqual(serviceAccount, old)) {
                log.info("ServiceAccount has not changed so not doing anything");
            } else {
                if (isRecreateMode()) {
                    kubernetesClient.serviceAccounts().inNamespace(namespace).withName(id).delete();
                    doCreateServiceAccount(serviceAccount, namespace, sourceName);
                } else {
                    log.info("Updating a ServiceAccount from " + sourceName);
                    try {
                        Object answer = kubernetesClient.serviceAccounts().inNamespace(namespace).withName(id).replace(serviceAccount);
                        logGeneratedEntity("Updated ServiceAccount: ", namespace, serviceAccount, answer);
                    } catch (Exception e) {
                        onApplyError("Failed to update ServiceAccount from " + sourceName + ". " + e + ". " + serviceAccount, e);
                    }
                }
            }
        } else {
            if (!isAllowCreate()) {
                log.warn("Creation disabled so not creating a ServiceAccount from " + sourceName + " namespace " + namespace + " name " + getName(serviceAccount));
            } else {
                doCreateServiceAccount(serviceAccount, namespace, sourceName);
            }
        }
    }

    protected void doCreateServiceAccount(ServiceAccount serviceAccount, String namespace, String sourceName) {
        log.info("Creating a ServiceAccount from " + sourceName + " namespace " + namespace + " name " + getName
                (serviceAccount));
        try {
            Object answer;
            if (StringUtils.isNotBlank(namespace)) {
                answer = kubernetesClient.serviceAccounts().inNamespace(namespace).create(serviceAccount);
            } else {
                answer = kubernetesClient.serviceAccounts().inNamespace(getNamespace()).create(serviceAccount);
            }
            logGeneratedEntity("Created ServiceAccount: ", namespace, serviceAccount, answer);
        } catch (Exception e) {
            onApplyError("Failed to create ServiceAccount from " + sourceName + ". " + e + ". " + serviceAccount, e);
        }
    }

    public void applyPersistentVolumeClaim(PersistentVolumeClaim entity, String sourceName) throws Exception {
        // we cannot update PVCs
        boolean alwaysRecreate = true;
        String namespace = getNamespace();
        String id = getName(entity);
        Objects.requireNonNull(id, "No name for " + entity + " " + sourceName);
        if (isServicesOnlyMode()) {
            log.debug("Only processing Services right now so ignoring PersistentVolumeClaim: " + id);
            return;
        }
        PersistentVolumeClaim old = kubernetesClient.persistentVolumeClaims().inNamespace(namespace).withName(id).get();
        if (isRunning(old)) {
            if (UserConfigurationCompare.configEqual(entity, old)) {
                log.info("PersistentVolumeClaim has not changed so not doing anything");
            } else {
                if (alwaysRecreate || isRecreateMode()) {
                    if (!isRecreateMode() && isIgnoreBoundPersistentVolumeClaims() && isBound(old)) {
                        log.warn("PersistentVolumeClaim " + id + " in namespace " + namespace + " is already bound and will not be replaced with the new one from " + sourceName);
                    } else {
                        log.info("Deleting PersistentVolumeClaim from namespace " + namespace + " with name " + id);
                        kubernetesClient.persistentVolumeClaims().inNamespace(namespace).withName(id).delete();
                        log.info("Deleted PersistentVolumeClaim from namespace " + namespace + " with name " + id);

                        doCreatePersistentVolumeClaim(entity, namespace, sourceName);
                    }
                } else {
                    log.info("Updating a PersistentVolumeClaim from " + sourceName);
                    try {
                        HasMetadata answer = patchService.compareAndPatchEntity(namespace, entity, old);
                        logGeneratedEntity("Updated PersistentVolumeClaim: ", namespace, entity, answer);
                    } catch (Exception e) {
                        onApplyError("Failed to update PersistentVolumeClaim from " + sourceName + ". " + e + ". " + entity, e);
                    }
                }
            }
        } else {
            if (!isAllowCreate()) {
                log.warn("Creation disabled so not creating a PersistentVolumeClaim from " + sourceName + " namespace " + namespace + " name " + getName(entity));
            } else {
                doCreatePersistentVolumeClaim(entity, namespace, sourceName);
            }
        }
    }

    public void applyCustomResourceDefinition(CustomResourceDefinition entity, String sourceName) {
        String namespace = getNamespace();
        String id = getName(entity);
        Objects.requireNonNull(id, "No name for " + entity + " " + sourceName);
        if (isServicesOnlyMode()) {
            log.debug("Only processing Services right now so ignoring Custom Resource Definition: " + namespace + ":" + id);
            return;
        }
        CustomResourceDefinition old = kubernetesClient.customResourceDefinitions().withName(id).get();
        if (isRunning(old)) {
            if (UserConfigurationCompare.configEqual(entity, old)) {
                log.info("Custom Resource Definition has not changed so not doing anything");
            } else {
                if (isRecreateMode()) {
                    log.info("Deleting Custom Resource Definition: " + id);
                    kubernetesClient.customResourceDefinitions().withName(id).delete();
                    doCreateCustomResourceDefinition(entity, sourceName);
                } else {
                    log.info("Updating a Custom Resource Definition from " + sourceName + " name " + getName(entity));
                    try {
                        HasMetadata answer = patchService.compareAndPatchEntity(namespace, entity, old);
                        log.info("Updated Custom Resource Definition result: " + getName(answer));
                    } catch (Exception e) {
                        onApplyError("Failed to update Custom Resource Definition from " + sourceName + ". " + e + ". " + entity, e);
                    }
                }
            }
        } else {
            if (!isAllowCreate()) {
                log.warn("Creation disabled so not creating a Custom Resource Definition from " + sourceName + " name " + getName(entity));
            } else {
                doCreateCustomResourceDefinition(entity, sourceName);
            }
        }
    }

    private void doCreateCustomResourceDefinition(CustomResourceDefinition entity, String sourceName) {
        log.info("Creating a Custom Resource Definition from " + sourceName + " name " + getName(entity));
        try {
            Object answer = kubernetesClient.customResourceDefinitions().create(entity);
            log.info("Created Custom Resource Definition result: " + ((CustomResourceDefinition) answer).getMetadata().getName());
        } catch (Exception e) {
            onApplyError("Failed to create Custom Resource Definition from " + sourceName + ". " + e + ". " + entity, e);
        }
    }

    public void applyCustomResource(File customResourceFile, String namespace, CustomResourceDefinitionContext context)
        throws Exception {

        Map<String, Object> cr = KubernetesClientUtil.doReadCustomResourceFile(customResourceFile);
        Map<String, Object> objectMeta = (Map<String, Object>)cr.get("metadata");
        String name = objectMeta.get("name").toString();

        if (isRecreateMode()) {
            KubernetesClientUtil.doDeleteCustomResource(kubernetesClient, context, namespace, name);
            KubernetesClientUtil.doCreateCustomResource(kubernetesClient, context, namespace, customResourceFile);
            log.info("Created Custom Resource: " + name);
        } else {
            cr = KubernetesClientUtil.doGetCustomResource(kubernetesClient, context, namespace, name);
            if (cr == null) {
                KubernetesClientUtil.doCreateCustomResource(kubernetesClient, context, namespace, customResourceFile);
                log.info("Created Custom Resource: " + name);
            } else {
                KubernetesClientUtil.doEditCustomResource(kubernetesClient, context, namespace, name, customResourceFile);
                log.info("Updated Custom Resource: " + name);
            }
        }
    }

    public void deleteCustomResource(File customResourceFile, String namespace, CustomResourceDefinitionContext crdContext)
        throws Exception {

        Map<String, Object> customResource = KubernetesClientUtil.doReadCustomResourceFile(customResourceFile);
        Map<String, Object> objectMeta = (Map<String, Object>)customResource.get("metadata");
        String name = objectMeta.get("name").toString();
        log.info("Deleting Custom Resource " + name);
        KubernetesClientUtil.doDeleteCustomResource(kubernetesClient, crdContext, namespace, name);
    }

    protected boolean isBound(PersistentVolumeClaim claim) {
        return claim != null &&
                claim.getStatus() != null &&
                "Bound".equals(claim.getStatus().getPhase());
    }

    protected void doCreatePersistentVolumeClaim(PersistentVolumeClaim entity, String namespace, String sourceName) {
        log.info("Creating a PersistentVolumeClaim from " + sourceName + " namespace " + namespace + " name " + getName(entity));
        try {
            Object answer;
            if (StringUtils.isNotBlank(namespace)) {
                answer = kubernetesClient.persistentVolumeClaims().inNamespace(namespace).create(entity);
            } else {
                answer = kubernetesClient.persistentVolumeClaims().inNamespace(getNamespace()).create(entity);
            }
            logGeneratedEntity("Created PersistentVolumeClaim: ", namespace, entity, answer);
        } catch (Exception e) {
            onApplyError("Failed to create PersistentVolumeClaim from " + sourceName + ". " + e + ". " + entity, e);
        }
    }

    public void applySecret(Secret secret, String sourceName) throws Exception {
        String namespace = getNamespace(secret);
        String id = getName(secret);
        Objects.requireNonNull(id, "No name for " + secret + " " + sourceName);
        if (isServicesOnlyMode()) {
            log.debug("Only processing Services right now so ignoring Secrets: " + id);
            return;
        }

        Secret old = kubernetesClient.secrets().inNamespace(namespace).withName(id).get();
        // check if the secret already exists or not
        if (isRunning(old)) {
            // if the secret already exists and is the same, then do nothing
            if (UserConfigurationCompare.configEqual(secret, old)) {
                log.info("Secret has not changed so not doing anything");
                return;
            } else {
                if (isRecreateMode()) {
                    kubernetesClient.secrets().inNamespace(namespace).withName(id).delete();
                    doCreateSecret(secret, namespace, sourceName);
                } else {
                    log.info("Updating a Secret from " + sourceName);
                    try {
                        Object answer = patchService.compareAndPatchEntity(namespace, secret, old);
                        logGeneratedEntity("Updated Secret:", namespace, secret, answer);
                    } catch (Exception e) {
                        onApplyError("Failed to update secret from " + sourceName + ". " + e + ". " + secret, e);
                    }
                }
            }
        } else {
            if (!isAllowCreate()) {
                log.warn("Creation disabled so not creating a Secret from " + sourceName + " namespace " + namespace + " name " + getName(secret));
            } else {
                doCreateSecret(secret, namespace, sourceName);
            }
        }
    }


    protected void doCreateSecret(Secret secret, String namespace, String sourceName) {
        log.info("Creating a Secret from " + sourceName + " namespace " + namespace + " name " + getName(secret));
        try {
            Object answer;
            if (StringUtils.isNotBlank(namespace)) {
                answer = kubernetesClient.secrets().inNamespace(namespace).create(secret);
            } else {
                answer = kubernetesClient.secrets().inNamespace(getNamespace()).create(secret);
            }
            logGeneratedEntity("Created Secret: ", namespace, secret, answer);
        } catch (Exception e) {
            onApplyError("Failed to create Secret from " + sourceName + ". " + e + ". " + secret, e);
        }
    }

    protected void logGeneratedEntity(String message, String namespace, HasMetadata entity, Object result) {
        if (logJsonDir != null) {
            File namespaceDir = new File(logJsonDir, namespace);
            namespaceDir.mkdirs();
            String kind = getKind(entity);
            String name = getName(entity);
            if (StringUtils.isNotBlank(kind)) {
                name = kind.toLowerCase() + "-" + name;
            }
            if (StringUtils.isBlank(name)) {
                log.warn("No name for the entity " + entity);
            } else {
                String fileName = name + ".json";
                File file = new File(namespaceDir, fileName);
                if (file.exists()) {
                    int idx = 1;
                    while (true) {
                        fileName = name + "-" + idx++ + ".json";
                        file = new File(namespaceDir, fileName);
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
                        text = ResourceUtil.toJson(result);
                    } catch (JsonProcessingException e) {
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
            } catch (IOException e) {
                onApplyError("Failed to process template " + sourceName + ". " + e + ". " + entity, e);
                return null;
            }
    }


    public void applyRoute(Route entity, String sourceName) {
        OpenShiftClient openShiftClient = getOpenShiftClient();
        if (openShiftClient != null) {
            String id = getName(entity);
            Objects.requireNonNull(id, "No name for " + entity + " " + sourceName);
            String namespace = KubernetesHelper.getNamespace(entity);
            if (StringUtils.isBlank(namespace)) {
                namespace = getNamespace();
            }
            Route route = openShiftClient.routes().inNamespace(namespace).withName(id).get();
            if (route == null) {
                try {
                    log.info("Creating Route " + namespace + ":" + id + " " +
                             (entity.getSpec() != null ?
                                 "host: " + entity.getSpec().getHost() :
                                 "No Spec !"));
                    openShiftClient.routes().inNamespace(namespace).create(entity);
                } catch (Exception e) {
                    onApplyError("Failed to create Route from " + sourceName + ". " + e + ". " + entity, e);
                }
            }
        }
    }

    public void applyBuildConfig(BuildConfig entity, String sourceName) {
        OpenShiftClient openShiftClient = getOpenShiftClient();
        if (openShiftClient != null) {
            String id = getName(entity);

            Objects.requireNonNull(id, "No name for " + entity + " " + sourceName);
            String namespace = KubernetesHelper.getNamespace(entity);
            if (StringUtils.isBlank(namespace)) {
                namespace = getNamespace();
            }
            applyNamespace(namespace);
            BuildConfig old = openShiftClient.buildConfigs().inNamespace(namespace).withName(id).get();
            if (isRunning(old)) {
                if (UserConfigurationCompare.configEqual(entity, old)) {
                    log.info("BuildConfig has not changed so not doing anything");
                } else {
                    if (isRecreateMode()) {
                        log.info("Deleting BuildConfig: " + id);
                        openShiftClient.buildConfigs().inNamespace(namespace).withName(id).delete();
                        doCreateBuildConfig(entity, namespace, sourceName);
                    } else {
                        log.info("Updating BuildConfig from " + sourceName);
                        try {
                            String resourceVersion = KubernetesHelper.getResourceVersion(old);
                            ObjectMeta metadata = getOrCreateMetadata(entity);
                            metadata.setNamespace(namespace);
                            metadata.setResourceVersion(resourceVersion);
                            Object answer = patchService.compareAndPatchEntity(namespace, entity, old);
                            logGeneratedEntity("Updated BuildConfig: ", namespace, entity, answer);
                        } catch (Exception e) {
                            onApplyError("Failed to update BuildConfig from " + sourceName + ". " + e + ". " + entity, e);
                        }
                    }
                }
            } else {
                if (!isAllowCreate()) {
                    log.warn("Creation disabled so not creating BuildConfig from " + sourceName + " namespace " + namespace + " name " + getName(entity));
                } else {
                    doCreateBuildConfig(entity, namespace, sourceName);
                }
            }
        }
    }

    public void doCreateBuildConfig(BuildConfig entity, String namespace , String sourceName) {
        OpenShiftClient openShiftClient = getOpenShiftClient();
        if (openShiftClient != null) {
            try {
                openShiftClient.buildConfigs().inNamespace(namespace).create(entity);
            } catch (Exception e) {
                onApplyError("Failed to create BuildConfig from " + sourceName + ". " + e, e);
            }
        }
    }

    public void applyRoleBinding(RoleBinding entity, String sourceName) {
        String id = getName(entity);

        Objects.requireNonNull(id, "No name for " + entity + " " + sourceName);
        String namespace = KubernetesHelper.getNamespace(entity);
        if (StringUtils.isBlank(namespace)) {
            namespace = getNamespace();
        }
        applyNamespace(namespace);
        RoleBinding old = kubernetesClient.rbac().roleBindings().inNamespace(namespace).withName(id).get();
        if (isRunning(old)) {
            if (UserConfigurationCompare.configEqual(entity, old)) {
                log.info("RoleBinding has not changed so not doing anything");
            } else {
                if (isRecreateMode()) {
                    log.info("Deleting RoleBinding: " + id);
                    kubernetesClient.rbac().roleBindings().inNamespace(namespace).withName(id).delete();
                    doCreateRoleBinding(entity, namespace, sourceName);
                } else {
                    log.info("Updating RoleBinding from " + sourceName);
                    try {
                        String resourceVersion = KubernetesHelper.getResourceVersion(old);
                        ObjectMeta metadata = getOrCreateMetadata(entity);
                        metadata.setNamespace(namespace);
                        metadata.setResourceVersion(resourceVersion);
                        Object answer = kubernetesClient.rbac().roleBindings().inNamespace(namespace).withName(id).replace(entity);
                        logGeneratedEntity("Updated RoleBinding: ", namespace, entity, answer);
                    } catch (Exception e) {
                        onApplyError("Failed to update RoleBinding from " + sourceName + ". " + e + ". " + entity, e);
                    }
                }
            }
        } else {
            if (!isAllowCreate()) {
                log.warn("Creation disabled so not creating RoleBinding from " + sourceName + " namespace " + namespace + " name " + getName(entity));
            } else {
                doCreateRoleBinding(entity, namespace, sourceName);
            }
        }
    }

    public void doCreateRoleBinding(RoleBinding entity, String namespace , String sourceName) {
        try {
            log.info("Creating RoleBinding from " + sourceName + " namespace " + namespace + " name " + getName(entity));
            kubernetesClient.rbac().roleBindings().inNamespace(namespace).create(entity);
        } catch (Exception e) {
            onApplyError("Failed to create RoleBinding from " + sourceName + ". " + e, e);
        }
    }

    public void applyImageStream(ImageStream entity, String sourceName) {
        OpenShiftClient openShiftClient = getOpenShiftClient();
        if (openShiftClient != null) {
            String kind = getKind(entity);
            String name = getName(entity);
            String namespace = getNamespace();
            try {
                Resource<ImageStream, DoneableImageStream> resource = openShiftClient.imageStreams().inNamespace(namespace).withName(name);
                ImageStream old = resource.get();
                if (old == null) {
                    log.info("Creating " + kind + " " + name + " from " + sourceName);
                    resource.create(entity);
                } else {
                    log.info("Updating " + kind + " " + name + " from " + sourceName);
                    copyAllImageStreamTags(entity, old);
                    entity = patchService.compareAndPatchEntity(namespace, entity, old);
                    openShiftClient.resource(entity).inNamespace(namespace).createOrReplace();
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


    public void applyList(KubernetesList list, String sourceName) throws Exception {
        List<HasMetadata> entities = list.getItems();
        if (entities != null) {
            for (Object entity : entities) {
                applyEntity(entity, sourceName);
            }
        }
    }

    public void applyService(Service service, String sourceName) throws Exception {
        String namespace = getNamespace();
        String id = getName(service);
        Objects.requireNonNull(id, "No name for " + service + " " + sourceName);
        if (isIgnoreServiceMode()) {
            log.debug("Ignoring Service: " + namespace + ":" + id);
            return;
        }
        Service old = kubernetesClient.services().inNamespace(namespace).withName(id).get();
        if (isRunning(old)) {
            if (UserConfigurationCompare.configEqual(service, old)) {
                log.info("Service has not changed so not doing anything");
            } else {
                if (isRecreateMode()) {
                    log.info("Deleting Service: " + id);
                    kubernetesClient.services().inNamespace(namespace).withName(id).delete();
                    doCreateService(service, namespace, sourceName);
                } else {
                    log.info("Updating a Service from " + sourceName);
                    try {
                        Object answer = patchService.compareAndPatchEntity(namespace, service, old);
                        logGeneratedEntity("Updated Service: ", namespace, service, answer);
                    } catch (Exception e) {
                        onApplyError("Failed to update Service from " + sourceName + ". " + e + ". " + service, e);
                    }
                }
            }
        } else {
            if (!isAllowCreate()) {
                log.warn("Creation disabled so not creating a Service from " + sourceName + " namespace " + namespace + " name " + getName(service));
            } else {
                doCreateService(service, namespace, sourceName);
            }
        }
    }

    public <T extends HasMetadata,L,D> void applyResource(T resource, String sourceName, MixedOperation<T, L, D, ? extends Resource<T, D>> resources) throws Exception {
        String namespace = getNamespace();
        String id = getName(resource);
        String kind = getKind(resource);
        Objects.requireNonNull(id, "No name for " + resource + " " + sourceName);
        if (isServicesOnlyMode()) {
            log.debug("Ignoring " + kind + ": " + namespace + ":" + id);
            return;
        }
        T old = resources.inNamespace(namespace).withName(id).get();
        if (isRunning(old)) {
            if (UserConfigurationCompare.configEqual(resource, old)) {
                log.info(kind + " has not changed so not doing anything");
            } else {
                if (isRecreateMode()) {
                    log.info("Deleting " + kind + ": " + id);
                    resources.inNamespace(namespace).withName(id).delete();
                    doCreateResource(resource, namespace, sourceName, resources);
                } else {
                    log.info("Updating " + kind + " from " + sourceName);
                    try {
                        Object answer = resources.inNamespace(namespace).withName(id).replace(resource);
                        logGeneratedEntity("Updated " + kind + ": ", namespace, resource, answer);
                    } catch (Exception e) {
                        onApplyError("Failed to update " + kind + " from " + sourceName + ". " + e + ". " + resource, e);
                    }
                }
            }
        } else {
            if (!isAllowCreate()) {
                log.warn("Creation disabled so not creating a " + kind + " from " + sourceName + " namespace " + namespace + " name " + getName(resource));
            } else {
                doCreateResource(resource, namespace, sourceName, resources);
            }
        }
    }

    protected <T extends HasMetadata,L,D> void doCreateResource(T resource, String namespace , String sourceName, MixedOperation<T, L, D, ? extends Resource<T, D>> resources) throws Exception {
        String kind = getKind(resource);
        log.info("Creating a " + kind + " from " + sourceName + " namespace " + namespace + " name " + getName(resource));
        try {
            Object answer;
            if (StringUtils.isNotBlank(namespace)) {
                answer = resources.inNamespace(namespace).create(resource);
            } else {
                answer = resources.inNamespace(getNamespace()).create(resource);
            }
            logGeneratedEntity("Created " + kind + ": ", namespace, resource, answer);
        } catch (Exception e) {
            onApplyError("Failed to create " + kind + " from " + sourceName + ". " + e + ". " + resource, e);
        }
    }

    protected void doCreateService(Service service, String namespace, String sourceName) {
        log.info("Creating a Service from " + sourceName + " namespace " + namespace + " name " + getName(service));
        try {
            Object answer;
            if (StringUtils.isNotBlank(namespace)) {
                answer = kubernetesClient.services().inNamespace(namespace).create(service);
            } else {
                answer = kubernetesClient.services().inNamespace(getNamespace()).create(service);
            }
            logGeneratedEntity("Created Service: ", namespace, service, answer);
        } catch (Exception e) {
            onApplyError("Failed to create Service from " + sourceName + ". " + e + ". " + service, e);
        }
    }

    public boolean checkNamespace(String namespaceName) {
        if (StringUtils.isBlank(namespaceName)) {
            return false;
        }
        OpenShiftClient openshiftClient = getOpenShiftClient();
        if (openshiftClient != null) {
            // It is preferable to iterate on the list of projects as regular user with the 'basic-role' bound
            // are not granted permission get operation on non-existing project resource that returns 403
            // instead of 404. Only more privileged roles like 'view' or 'cluster-reader' are granted this permission.
            List<Project> projects = openshiftClient.projects().list().getItems();
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

    public boolean deleteNamespace(String namespaceName) {
        if (!checkNamespace(namespaceName)) {
            return false;
        }
        OpenShiftClient openshiftClient = getOpenShiftClient();
        if (openshiftClient != null) {
            return openshiftClient.projects().withName(namespaceName).delete();
        } else {
            return kubernetesClient.namespaces().withName(namespaceName).delete();
        }
    }

    public void applyNamespace(String namespaceName) {
        applyNamespace(namespaceName, null);

    }
    public void applyNamespace(String namespaceName, Map<String,String> labels) {
        if (StringUtils.isBlank(namespaceName)) {
            return;
        }
        OpenShiftClient openshiftClient = getOpenShiftClient();
        if (openshiftClient != null) {
            ProjectRequest entity = new ProjectRequest();
            ObjectMeta metadata = getOrCreateMetadata(entity);
            metadata.setName(namespaceName);
            String namespace = kubernetesClient.getNamespace();
            if (StringUtils.isNotBlank(namespace)) {
                Map<String, String> entityLabels = getOrCreateLabels(entity);
                if (labels != null) {
                    entityLabels.putAll(labels);
                } else {
                    // lets associate this new namespace with the project that it was created from
                    entityLabels.put("project", namespace);
                }
            }
            applyProjectRequest(entity);
        }
        else {
            Namespace entity = new Namespace();
            ObjectMeta metadata = getOrCreateMetadata(entity);
            metadata.setName(namespaceName);
            String namespace = kubernetesClient.getNamespace();
            if (StringUtils.isNotBlank(namespace)) {
                Map<String, String> entityLabels = getOrCreateLabels(entity);
                if (labels != null) {
                    entityLabels.putAll(labels);
                } else {
                    // lets associate this new namespace with the project that it was created from
                    entityLabels.put("project", namespace);
                }
            }
            applyNamespace(entity);
        }
    }

    /**
     * Returns true if the namespace is created
     */
    public boolean applyNamespace(Namespace entity) {
        String namespace = getOrCreateMetadata(entity).getName();
        log.info("Using namespace: " + namespace);
        String name = getName(entity);
        Objects.requireNonNull(name, "No name for " + entity );
        Namespace old = kubernetesClient.namespaces().withName(name).get();
        if (!isRunning(old)) {
            try {
                Object answer = kubernetesClient.namespaces().create(entity);
                logGeneratedEntity("Created namespace: ", namespace, entity, answer);
                return true;
            } catch (Exception e) {
                onApplyError("Failed to create namespace: " + name + " due " + e.getMessage(), e);
            }
        }
        return false;
    }

    /**
     * Creates and return a project in openshift
     * @param project
     * @return
     */
    public boolean applyProject(Project project) {
        return applyProjectRequest(new ProjectRequestBuilder()
                .withDisplayName(project.getMetadata().getName())
                .withMetadata(project.getMetadata()).build());
    }

    /**
     * Returns true if the ProjectRequest is created
     */
    public boolean applyProjectRequest(ProjectRequest entity) {
        // Check whether project creation attempted before
        if (projectsCreated.contains(getName(entity))) {
            return false;
        }
        String namespace = getOrCreateMetadata(entity).getName();
        log.info("Using project: " + namespace);
        String name = getName(entity);
        Objects.requireNonNull(name, "No name for " + entity);
        OpenShiftClient openshiftClient = getOpenShiftClient();
        if (openshiftClient == null) {
            log.warn("Cannot check for Project " + namespace + " as not running against OpenShift!");
            return false;
        }
        boolean exists = checkNamespace(name);
        // We may want to be more fine-grained on the phase of the project
        if (!exists) {
            try {
                Object answer = openshiftClient.projectrequests().create(entity);
                // Add project to created projects
                projectsCreated.add(name);
                logGeneratedEntity("Created ProjectRequest: ", namespace, entity, answer);
                return true;
            } catch (Exception e) {
                onApplyError("Failed to create ProjectRequest: " + name + " due " + e.getMessage(), e);
            }
        }
        return false;
    }

    public void applyReplicationController(ReplicationController replicationController, String sourceName) throws Exception {
        String namespace = getNamespace();
        String id = getName(replicationController);
        Objects.requireNonNull(id, "No name for " + replicationController + " " + sourceName);
        if (isServicesOnlyMode()) {
            log.debug("Only processing Services right now so ignoring ReplicationController: " + namespace + ":" + id);
            return;
        }
        ReplicationController old = kubernetesClient.replicationControllers().inNamespace(namespace).withName(id).get();
        if (isRunning(old)) {
            if (UserConfigurationCompare.configEqual(replicationController, old)) {
                log.info("ReplicationController has not changed so not doing anything");
            } else {
                ReplicationControllerSpec newSpec = replicationController.getSpec();
                ReplicationControllerSpec oldSpec = old.getSpec();
                if (rollingUpgrade) {
                    log.info("Rolling upgrade of the ReplicationController: " + namespace + "/" + id);
                    // lets preserve the number of replicas currently running in the environment we are about to upgrade
                    if (rollingUpgradePreserveScale && newSpec != null && oldSpec != null) {
                        Integer replicas = oldSpec.getReplicas();
                        if (replicas != null) {
                            newSpec.setReplicas(replicas);
                        }
                    }
                    log.info("rollingUpgradePreserveScale " + rollingUpgradePreserveScale + " new replicas is " + (newSpec != null ? newSpec.getReplicas() : "<null>"));
                    kubernetesClient.replicationControllers().inNamespace(namespace).withName(id).rolling().replace(replicationController);
                } else if (isRecreateMode()) {
                    log.info("Deleting ReplicationController: " + id);
                    kubernetesClient.replicationControllers().inNamespace(namespace).withName(id).delete();
                    doCreateReplicationController(replicationController, namespace, sourceName);
                } else {
                    log.info("Updating ReplicationController from " + sourceName + " namespace " + namespace + " name " + getName(replicationController));
                    try {
                        Object answer = patchService.compareAndPatchEntity(namespace, replicationController, old);
                        logGeneratedEntity("Updated replicationController: ", namespace, replicationController, answer);

                        if (deletePodsOnReplicationControllerUpdate) {
                            kubernetesClient.pods().inNamespace(namespace).withLabels(newSpec.getSelector()).delete();
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
                log.warn("Creation disabled so not creating a ReplicationController from " + sourceName + " namespace " + namespace + " name " + getName(replicationController));
            } else {
                doCreateReplicationController(replicationController, namespace, sourceName);
            }
        }
    }

    protected void doCreateReplicationController(ReplicationController replicationController, String namespace, String sourceName) {
        log.info("Creating a ReplicationController from " + sourceName + " namespace " + namespace + " name " + getName(replicationController));
        try {
            Object answer;
            if (StringUtils.isNotBlank(namespace)) {
                answer = kubernetesClient.replicationControllers().inNamespace(namespace).create(replicationController);
            } else {
                answer =  kubernetesClient.replicationControllers().inNamespace(getNamespace()).create(replicationController);
            }
            logGeneratedEntity("Created ReplicationController: ", namespace, replicationController, answer);
        } catch (Exception e) {
            onApplyError("Failed to create ReplicationController from " + sourceName + ". " + e + ". " + replicationController, e);
        }
    }

    public void applyPod(Pod pod, String sourceName) throws Exception {
        String namespace = getNamespace();
        String id = getName(pod);
        Objects.requireNonNull(id, "No name for " + pod + " " + sourceName);
        if (isServicesOnlyMode()) {
            log.debug("Only processing Services right now so ignoring Pod: " + namespace + ":" + id);
            return;
        }
        Pod old = kubernetesClient.pods().inNamespace(namespace).withName(id).get();
        if (isRunning(old)) {
            if (UserConfigurationCompare.configEqual(pod, old)) {
                log.info("Pod has not changed so not doing anything");
            } else {
                if (isRecreateMode()) {
                    log.info("Deleting Pod: " + id);
                    kubernetesClient.pods().inNamespace(namespace).withName(id).delete();
                    doCreatePod(pod, namespace, sourceName);
                } else {
                    log.info("Updating a Pod from " + sourceName + " namespace " + namespace + " name " + getName(pod));
                    try {
                        Object answer = patchService.compareAndPatchEntity(namespace, pod, old);
                        log.info("Updated Pod result: " + answer);
                    } catch (Exception e) {
                        onApplyError("Failed to update Pod from " + sourceName + ". " + e + ". " + pod, e);
                    }
                }
            }
        } else {
            if (!isAllowCreate()) {
                log.warn("Creation disabled so not creating a pod from " + sourceName + " namespace " + namespace + " name " + getName(pod));
            } else {
                doCreatePod(pod, namespace, sourceName);
            }
        }
    }

    protected void doCreatePod(Pod pod, String namespace, String sourceName) {
        log.info("Creating a Pod from " + sourceName + " namespace " + namespace + " name " + getName(pod));
        try {
            Object answer;
            if (StringUtils.isNotBlank(namespace)) {
                answer = kubernetesClient.pods().inNamespace(namespace).create(pod);
            } else {
                answer = kubernetesClient.pods().inNamespace(getNamespace()).create(pod);
            }
            log.info("Created Pod result: " + answer);
        } catch (Exception e) {
            onApplyError("Failed to create Pod from " + sourceName + ". " + e + ". " + pod, e);
        }
    }

    protected void applyJob(Job job, String sourceName) {
        String namespace = getNamespace();
        String id = getName(job);
        Objects.requireNonNull(id, "No name for " + job + " " + sourceName);
        if (isServicesOnlyMode()) {
            log.debug("Only processing Services right now so ignoring Job: " + namespace + ":" + id);
            return;
        }
        // Not using createOrReplace() here (https://github.com/fabric8io/kubernetes-client/issues/1586)
        try {
            doCreateJob(job, namespace, sourceName);
        } catch (KubernetesClientException exception) {
            if(exception.getStatus().getCode().equals(HttpURLConnection.HTTP_CONFLICT)) {
                Job old = kubernetesClient.batch().jobs().inNamespace(namespace).withName(id).get();
                Job updatedJob = patchService.compareAndPatchEntity(namespace, job, old);
                log.info("Updated Job: " + old.getMetadata().getName());
                return;
            }
            onApplyError("Failed to apply Job from " + job.getMetadata().getName(), exception);
        }
    }

    public void doCreateJob(Job job, String namespace, String sourceName) throws KubernetesClientException {
        if (StringUtils.isNotBlank(namespace)) {
            kubernetesClient.batch().jobs().inNamespace(namespace).create(job);
        } else {
            kubernetesClient.batch().jobs().inNamespace(getNamespace()).create(job);
        }
        log.info("Creating a Job from " + sourceName + " namespace " + namespace + " name " + getName(job));
    }

    public String getNamespace() {
        return namespace;
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
}
