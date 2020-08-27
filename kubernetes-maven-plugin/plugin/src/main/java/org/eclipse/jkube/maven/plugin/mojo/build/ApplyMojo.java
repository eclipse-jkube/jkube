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
package org.eclipse.jkube.maven.plugin.mojo.build;

import io.fabric8.kubernetes.api.model.DoneableService;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.fabric8.openshift.api.model.Project;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.KubernetesHelper;
import org.eclipse.jkube.kit.common.util.OpenshiftHelper;
import org.eclipse.jkube.kit.common.util.ResourceUtil;
import org.eclipse.jkube.kit.config.resource.JKubeAnnotations;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.config.service.ApplyService;
import org.eclipse.jkube.kit.config.service.kubernetes.KubernetesClientUtil;
import org.eclipse.jkube.kit.enricher.api.util.KubernetesResourceUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.eclipse.jkube.maven.plugin.mojo.ManifestProvider;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.eclipse.jkube.kit.common.util.KubernetesHelper.getCustomResourcesFileToNameMap;

/**
 * Base class for goals which deploy the generated artifacts into the Kubernetes cluster
 */
@Mojo(name = "apply", requiresDependencyResolution = ResolutionScope.COMPILE, defaultPhase = LifecyclePhase.INSTALL)
public class ApplyMojo extends AbstractJKubeMojo implements ManifestProvider {

    public static final String DEFAULT_KUBERNETES_MANIFEST = "${basedir}/target/classes/META-INF/jkube/kubernetes.yml";
    public static final String DEFAULT_OPENSHIFT_MANIFEST = "${basedir}/target/classes/META-INF/jkube/openshift.yml";

    /**
     * Should we update resources by deleting them first and then creating them again?
     */
    @Parameter(property = "jkube.recreate", defaultValue = "false")
    protected boolean recreate;

    /**
     * The generated kubernetes YAML file
     */
    @Parameter(property = "jkube.kubernetesManifest", defaultValue = DEFAULT_KUBERNETES_MANIFEST)
    private File kubernetesManifest;

    /**
     * Should we create new kubernetes resources?
     */
    @Parameter(property = "jkube.deploy.create", defaultValue = "true")
    private boolean createNewResources;

    /**
     * Should we use rolling upgrades to apply changes?
     */
    @Parameter(property = "jkube.rolling", defaultValue = "false")
    private boolean rollingUpgrades;

    /**
     * Should we fail if there is no kubernetes json
     */
    @Parameter(property = "jkube.deploy.failOnNoKubernetesJson", defaultValue = "false")
    private boolean failOnNoKubernetesJson;

    /**
     * In services only mode we only process services so that those can be recursively created/updated first
     * before creating/updating any pods and replication controllers
     */
    @Parameter(property = "jkube.deploy.servicesOnly", defaultValue = "false")
    private boolean servicesOnly;

    /**
     * Do we want to ignore services. This is particularly useful when in recreate mode
     * to let you easily recreate all the ReplicationControllers and Pods but leave any service
     * definitions alone to avoid changing the portalIP addresses and breaking existing pods using
     * the service.
     */
    @Parameter(property = "jkube.deploy.ignoreServices", defaultValue = "false")
    private boolean ignoreServices;

    /**
     * Process templates locally in Java so that we can apply OpenShift templates on any Kubernetes environment
     */
    @Parameter(property = "jkube.deploy.processTemplatesLocally", defaultValue = "false")
    private boolean processTemplatesLocally;

    /**
     * Should we delete all the pods if we update a Replication Controller
     */
    @Parameter(property = "jkube.deploy.deletePods", defaultValue = "true")
    private boolean deletePodsOnReplicationControllerUpdate;

    /**
     * Do we want to ignore OAuthClients which are already running?. OAuthClients are shared across namespaces
     * so we should not try to update or create/delete global oauth clients
     */
    @Parameter(property = "jkube.deploy.ignoreRunningOAuthClients", defaultValue = "true")
    private boolean ignoreRunningOAuthClients;

    /**
     * The folder we should store any temporary json files or results
     */
    @Parameter(property = "jkube.deploy.jsonLogDir", defaultValue = "${basedir}/target/jkube/applyJson")
    private File jsonLogDir;

    /**
     * How many seconds to wait for a URL to be generated for a service
     */
    @Parameter(property = "jkube.serviceUrl.waitSeconds", defaultValue = "5")
    protected long serviceUrlWaitTimeSeconds;

    @Parameter
    protected ResourceConfig resources;

    /**
     * Folder where to find project specific files
     */
    @Parameter(property = "jkube.resourceDir", defaultValue = "${basedir}/src/main/jkube")
    private File resourceDir;

    /**
     * Environment name where resources are placed. For example, if you set this property to dev and resourceDir is the default one, jkube will look at src/main/jkube/dev
     * Same applies for resourceDirOpenShiftOverride property.
     */
    @Parameter(property = "jkube.environment")
    private String environment;

    @Parameter(property = "jkube.skip.apply", defaultValue = "false")
    protected boolean skipApply;

    protected ApplyService applyService;

    @Override
    protected boolean canExecute() {
        return super.canExecute() && !skipApply;
    }

    @Override
    public File getKubernetesManifest() {
        return kubernetesManifest;
    }

    @Override
    public void executeInternal() throws MojoExecutionException {
        try {
            KubernetesClient kubernetes = clusterAccess.createDefaultClient();
            applyService = new ApplyService(kubernetes, log);
            initServices(kubernetes);

            URL masterUrl = kubernetes.getMasterUrl();
            final File manifest = getManifest(kubernetes);
            if (!manifest.exists() || !manifest.isFile()) {
                if (failOnNoKubernetesJson) {
                    throw new MojoFailureException("No such generated manifest file: " + manifest);
                } else {
                    log.warn("No such generated manifest file %s for this project so ignoring", manifest);
                    return;
                }
            }

            String clusterKind = "Kubernetes";
            if (OpenshiftHelper.isOpenShift(kubernetes)) {
                clusterKind = "OpenShift";
            }
            KubernetesResourceUtil.validateKubernetesMasterUrl(masterUrl);
            log.info("Using %s at %s in namespace %s with manifest %s ", clusterKind, masterUrl, clusterAccess.getNamespace(), manifest);

            applyService.setAllowCreate(createNewResources);
            applyService.setServicesOnlyMode(servicesOnly);
            applyService.setIgnoreServiceMode(ignoreServices);
            applyService.setLogJsonDir(jsonLogDir);
            applyService.setBasedir(getRootProjectFolder());
            applyService.setIgnoreRunningOAuthClients(ignoreRunningOAuthClients);
            applyService.setProcessTemplatesLocally(processTemplatesLocally);
            applyService.setDeletePodsOnReplicationControllerUpdate(deletePodsOnReplicationControllerUpdate);
            applyService.setRollingUpgrade(rollingUpgrades);
            applyService.setRollingUpgradePreserveScale(isRollingUpgradePreserveScale());

            boolean openShift = OpenshiftHelper.isOpenShift(kubernetes);
            if (openShift) {
                log.info("[[B]]OpenShift[[B]] platform detected");
            } else {
                disableOpenShiftFeatures(applyService);
            }

            Set<HasMetadata> entities = KubernetesResourceUtil.loadResources(manifest);

            String namespace = clusterAccess.getNamespace();
            boolean namespaceEntityExist = false;

            for (HasMetadata entity: entities) {
                if (entity instanceof Namespace) {
                    Namespace ns = (Namespace)entity;
                    namespace = ns.getMetadata().getName();
                    applyService.applyNamespace((ns));
                    namespaceEntityExist = true;
                    entities.remove(entity);
                    break;
                }
                if (entity instanceof Project) {
                    Project project = (Project)entity;
                    namespace = project.getMetadata().getName();
                    applyService.applyProject(project);
                    namespaceEntityExist = true;
                    entities.remove(entity);
                    break;
                }
            }

            if (!namespaceEntityExist) {
                applyService.applyNamespace(namespace);
            }

            applyService.setNamespace(namespace);

            applyEntities(kubernetes, namespace, manifest.getName(), entities);
            log.info("[[B]]HINT:[[B]] Use the command `%s get pods -w` to watch your pods start up", clusterAccess.isOpenShift() ? "oc" : "kubectl");
        } catch (KubernetesClientException e) {
            KubernetesResourceUtil.handleKubernetesClientException(e, this.log);
        } catch (MojoExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    protected void initServices(KubernetesClient kubernetes) {
        log.debug("No services required in ApplyMojo");
    }

    protected void applyEntities(KubernetesClient kubernetes, String namespace, String fileName, Set<HasMetadata> entities) throws Exception {
        // Apply all items
        for (HasMetadata entity : entities) {
            if (entity instanceof Pod) {
                Pod pod = (Pod) entity;
                applyService.applyPod(pod, fileName);
            } else if (entity instanceof Service) {
                Service service = (Service) entity;
                applyService.applyService(service, fileName);
            } else if (entity instanceof ReplicationController) {
                ReplicationController replicationController = (ReplicationController) entity;
                applyService.applyReplicationController(replicationController, fileName);
            } else if (entity != null) {
                applyService.apply(entity, fileName);
            }
        }

        KitLogger serviceLogger = createLogger("[[G]][SVC][[G]] [[s]]");
        long serviceUrlWaitTimeSeconds = this.serviceUrlWaitTimeSeconds;
        for (HasMetadata entity : entities) {
            if (entity instanceof Service) {
                Service service = (Service) entity;
                String name = KubernetesHelper.getName(service);
                Resource<Service, DoneableService> serviceResource = kubernetes.services().inNamespace(namespace).withName(name);
                String url = null;
                // lets wait a little while until there is a service URL in case the exposecontroller is running slow
                for (int i = 0; i < serviceUrlWaitTimeSeconds; i++) {
                    if (i > 0) {
                        Thread.sleep(1000);
                    }
                    Service s = serviceResource.get();
                    if (s != null) {
                        url = getExternalServiceURL(s);
                        if (StringUtils.isNotBlank(url)) {
                            break;
                        }
                    }
                    if (!isExposeService(service)) {
                        break;
                    }
                }

                // lets not wait for other services
                serviceUrlWaitTimeSeconds = 1;
                if (StringUtils.isNotBlank(url) && url.startsWith("http")) {
                    serviceLogger.info("%s: %s", name, url);
                }
            }
        }
        processCustomEntities(kubernetes, namespace, resources != null ? resources.getCustomResourceDefinitions() : null);
    }

    protected String getExternalServiceURL(Service service) {
        return KubernetesHelper.getOrCreateAnnotations(service).get(JKubeAnnotations.SERVICE_EXPOSE_URL.value());
    }

    protected boolean isExposeService(Service service) {
        String expose = KubernetesHelper.getLabels(service).get("expose");
        return expose != null && expose.toLowerCase().equals("true");
    }

    public boolean isRollingUpgradePreserveScale() {
        return false;
    }

    public MavenProject getProject() {
        return project;
    }

    /**
     * Lets disable OpenShift-only features if we are not running on OpenShift
     */
    protected void disableOpenShiftFeatures(ApplyService applyService) {
        // TODO we could check if the Templates service is running and if so we could still support templates?
        this.processTemplatesLocally = true;
        applyService.setSupportOAuthClients(false);
        applyService.setProcessTemplatesLocally(true);
    }

    protected void processCustomEntities(KubernetesClient client, String namespace, List<String> customResourceDefinitions) throws Exception {
        if(customResourceDefinitions == null)
            return;

        List<CustomResourceDefinitionContext> crdContexts = KubernetesClientUtil.getCustomResourceDefinitionContext(client ,customResourceDefinitions);
        File resourceDirFinal = ResourceUtil.getFinalResourceDir(resourceDir, environment);
        Map<File, String> fileToCrdMap = getCustomResourcesFileToNameMap(resourceDirFinal, resources != null ? resources.getRemotes() : null, getKitLogger());

        for(CustomResourceDefinitionContext customResourceDefinitionContext : crdContexts) {
            for(Map.Entry<File, String> entry : fileToCrdMap.entrySet()) {
                if(entry.getValue().equals(customResourceDefinitionContext.getGroup())) {
                    applyService.applyCustomResource(entry.getKey(), namespace, customResourceDefinitionContext);
                }
            }
        }
    }

    /**
     * Returns the root project folder
     */
    protected File getRootProjectFolder() {
        File answer = null;
        MavenProject project = getProject();
        while (project != null) {
            File basedir = project.getBasedir();
            if (basedir != null) {
                answer = basedir;
            }
            project = project.getParent();
        }
        return answer;
    }

}
