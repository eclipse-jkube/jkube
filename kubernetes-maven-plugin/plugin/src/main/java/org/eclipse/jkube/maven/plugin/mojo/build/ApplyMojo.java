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

import java.io.File;
import java.net.URL;
import java.util.Collection;
import java.util.List;

import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.KubernetesHelper;
import org.eclipse.jkube.kit.common.util.MavenUtil;
import org.eclipse.jkube.kit.common.util.OpenshiftHelper;
import org.eclipse.jkube.kit.config.service.ApplyService;
import org.eclipse.jkube.kit.enricher.api.util.KubernetesResourceUtil;
import org.eclipse.jkube.maven.plugin.mojo.ManifestProvider;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import static org.eclipse.jkube.kit.config.service.kubernetes.KubernetesClientUtil.resolveFallbackNamespace;

/**
 * Base class for goals which deploy the generated artifacts into the Kubernetes cluster
 */
@Mojo(name = "apply", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, defaultPhase = LifecyclePhase.INSTALL)
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
    protected File kubernetesManifest;

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
     * Do we want to ignore services? This is particularly useful when in recreate mode
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
     * Do we want to ignore OAuthClients which are already running?. OAuthClients are shared across namespaces,
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
        try (KubernetesClient kubernetes = jkubeServiceHub.getClient()) {
            applyService = jkubeServiceHub.getApplyService();
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
            List<HasMetadata> entities = KubernetesHelper.loadResources(manifest);

            configureApplyService(kubernetes);

            log.info("Using %s at %s in namespace %s with manifest %s ", clusterKind, masterUrl,
                applyService.getNamespace(),
                manifest);

            // Apply rest of the entities present in manifest
            applyEntities(kubernetes, manifest.getName(), entities);
            log.info("[[B]]HINT:[[B]] Use the command `%s get pods -w` to watch your pods start up", clusterAccess.isOpenShift() ? "oc" : "kubectl");
        } catch (KubernetesClientException e) {
            KubernetesResourceUtil.handleKubernetesClientException(e, this.log);
        } catch(InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }

    }

    protected void applyEntities(final KubernetesClient kubernetes, String fileName, final Collection<HasMetadata> entities) throws InterruptedException {
        KitLogger serviceLogger = createLogger("[[G]][SVC][[G]] [[s]]");
        applyService.applyEntities(fileName, entities, serviceLogger, serviceUrlWaitTimeSeconds);
    }

    protected void initServices(KubernetesClient kubernetes) {
        log.debug("No services required in ApplyMojo");
    }

    public boolean isRollingUpgradePreserveScale() {
        return false;
    }

    public MavenProject getProject() {
        return project;
    }

    /**
     * Let's disable OpenShift-only features if we are not running on OpenShift
     */
    protected void disableOpenShiftFeatures(ApplyService applyService) {
        // TODO we could check if the Templates service is running and if so we could still support templates?
        this.processTemplatesLocally = true;
        applyService.setSupportOAuthClients(false);
        applyService.setProcessTemplatesLocally(true);
    }

    private void configureApplyService(KubernetesClient kubernetes) {
        applyService.setAllowCreate(createNewResources);
        applyService.setServicesOnlyMode(servicesOnly);
        applyService.setIgnoreServiceMode(ignoreServices);
        applyService.setLogJsonDir(jsonLogDir);
        applyService.setBasedir(MavenUtil.getRootProjectFolder(getProject()));
        applyService.setIgnoreRunningOAuthClients(ignoreRunningOAuthClients);
        applyService.setProcessTemplatesLocally(processTemplatesLocally);
        applyService.setDeletePodsOnReplicationControllerUpdate(deletePodsOnReplicationControllerUpdate);
        applyService.setRollingUpgrade(rollingUpgrades);
        applyService.setRollingUpgradePreserveScale(isRollingUpgradePreserveScale());
        applyService.setRecreateMode(recreate);
        applyService.setNamespace(namespace);
        applyService.setFallbackNamespace(resolveFallbackNamespace(resources, clusterAccess));

        boolean openShift = OpenshiftHelper.isOpenShift(kubernetes);
        if (openShift) {
            log.info("[[B]]OpenShift[[B]] platform detected");
        } else {
            disableOpenShiftFeatures(applyService);
        }
    }
}
