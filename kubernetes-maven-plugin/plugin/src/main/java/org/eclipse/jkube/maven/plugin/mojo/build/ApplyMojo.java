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


import com.fasterxml.jackson.core.JsonProcessingException;
import io.fabric8.kubernetes.api.model.DoneableService;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.ServiceSpec;
import io.fabric8.kubernetes.api.model.extensions.HTTPIngressPath;
import io.fabric8.kubernetes.api.model.extensions.HTTPIngressPathBuilder;
import io.fabric8.kubernetes.api.model.extensions.HTTPIngressRuleValue;
import io.fabric8.kubernetes.api.model.extensions.Ingress;
import io.fabric8.kubernetes.api.model.extensions.IngressBackend;
import io.fabric8.kubernetes.api.model.extensions.IngressBuilder;
import io.fabric8.kubernetes.api.model.extensions.IngressList;
import io.fabric8.kubernetes.api.model.extensions.IngressRule;
import io.fabric8.kubernetes.api.model.extensions.IngressSpec;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.fabric8.openshift.api.model.Project;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.RouteList;
import io.fabric8.openshift.api.model.RouteSpec;
import io.fabric8.openshift.api.model.RouteTargetReference;
import io.fabric8.openshift.api.model.RouteTargetReferenceBuilder;
import io.fabric8.openshift.client.OpenShiftClient;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.FileUtil;
import org.eclipse.jkube.kit.common.util.KubernetesHelper;
import org.eclipse.jkube.kit.common.util.OpenshiftHelper;
import org.eclipse.jkube.kit.common.util.ResourceUtil;
import org.eclipse.jkube.kit.config.access.ClusterAccess;
import org.eclipse.jkube.kit.config.resource.JKubeAnnotations;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.config.service.ApplyService;
import org.eclipse.jkube.kit.config.service.kubernetes.KubernetesClientUtil;
import org.eclipse.jkube.kit.enricher.api.util.KubernetesResourceUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Base class for goals which deploy the generated artifacts into the Kubernetes cluster
 */
@Mojo(name = "apply", requiresDependencyResolution = ResolutionScope.COMPILE, defaultPhase = LifecyclePhase.INSTALL)
public class ApplyMojo extends AbstractJKubeMojo {

    public static final String DEFAULT_KUBERNETES_MANIFEST = "${basedir}/target/classes/META-INF/jkube/kubernetes.yml";
    public static final String DEFAULT_OPENSHIFT_MANIFEST = "${basedir}/target/classes/META-INF/jkube/openshift.yml";

    /**
     * The domain added to the service ID when creating OpenShift routes
     */
    @Parameter(property = "jkube.domain")
    protected String routeDomain;

    /**
     * Should we fail the build if an apply fails?
     */
    @Parameter(property = "jkube.deploy.failOnError", defaultValue = "true")
    protected boolean failOnError;

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
     * The generated openshift YAML file
     */
    @Parameter(property = "jkube.openshiftManifest", defaultValue = DEFAULT_OPENSHIFT_MANIFEST)
    private File openshiftManifest;

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
     * Should we create external Ingress/Routes for any LoadBalancer Services which don't already have them.
     * <p>
     * We now do not do this by default and defer this to the
     * <a href="https://github.com/jkubeio/exposecontroller/">exposecontroller</a> to decide
     * if Ingress or Router is being used or whether we should use LoadBalancer or NodePorts for single node clusters
     */
    @Parameter(property = "jkube.deploy.createExternalUrls", defaultValue = "false")
    private boolean createExternalUrls;

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
     * The S2I binary builder BuildConfig name suffix appended to the image name to avoid
     * clashing with the underlying BuildConfig for the Jenkins pipeline
     */
    @Parameter(property = "jkube.s2i.buildNameSuffix", defaultValue = "-s2i")
    protected String s2iBuildNameSuffix;

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

    private ClusterAccess clusterAccess;
    protected ApplyService applyService;

    public void executeInternal() throws MojoExecutionException {
        if (skipApply) {
            return;
        }

        clusterAccess = new ClusterAccess(getClusterConfiguration());

        try {
            KubernetesClient kubernetes = clusterAccess.createDefaultClient(log);
            applyService = new ApplyService(kubernetes, log);
            initServices(kubernetes, log);

            URL masterUrl = kubernetes.getMasterUrl();
            File manifest;
            if (OpenshiftHelper.isOpenShift(kubernetes)) {
                manifest = openshiftManifest;
            } else {
                manifest = kubernetesManifest;
            }
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
                getLog().info("OpenShift platform detected");
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

            if (createExternalUrls) {
                if (applyService.getOpenShiftClient() != null) {
                    createRoutes(entities);
                } else {
                    createIngress(kubernetes, entities);
                }
            }
            applyEntities(kubernetes, namespace, manifest.getName(), entities);
            log.info("[[B]]HINT:[[B]] Use the command `%s get pods -w` to watch your pods start up", clusterAccess.isOpenShiftImageStream(log) ? "oc" : "kubectl");

        } catch (KubernetesClientException e) {
            KubernetesResourceUtil.handleKubernetesClientException(e, this.log);
        } catch (MojoExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    protected void initServices(KubernetesClient kubernetes, KitLogger log) {

    }

    private Route createRouteForService(String routeDomainPostfix, String namespace, Service service) {
        Route route = null;
        String id = KubernetesHelper.getName(service);
        if (StringUtils.isNotBlank(id) && hasExactlyOneService(service, id)) {
            route = new Route();
            ObjectMeta routeMeta = KubernetesHelper.getOrCreateMetadata(route);
            routeMeta.setName(id);
            routeMeta.setNamespace(namespace);

            RouteSpec routeSpec = new RouteSpec();
            RouteTargetReference objectRef = new RouteTargetReferenceBuilder().withName(id).build();
            //objectRef.setNamespace(namespace);
            routeSpec.setTo(objectRef);
            if (StringUtils.isNotBlank(routeDomainPostfix)) {
                routeSpec.setHost(prepareHostForRoute(routeDomainPostfix, id));
            } else {
                routeSpec.setHost("");
            }
            route.setSpec(routeSpec);
            String json;
            try {
                json = ResourceUtil.toJson(route);
            } catch (JsonProcessingException e) {
                json = e.getMessage() + ". object: " + route;
            }
            log.debug("Created route: " + json);
        }
        return route;
    }

    private String prepareHostForRoute(String routeDomainPostfix, String name) {
        String ret = FileUtil.stripPostfix(name,"-service");
        ret = FileUtil.stripPostfix(ret,".");
        ret += ".";
        ret += FileUtil.stripPrefix(routeDomainPostfix, ".");
        return ret;
    }


    private Ingress createIngressForService(String routeDomainPostfix, String namespace, Service service) {
        Ingress ingress = null;
        String serviceName = KubernetesHelper.getName(service);
        ServiceSpec serviceSpec = service.getSpec();
        if (serviceSpec != null && StringUtils.isNotBlank(serviceName) && shouldCreateExternalURLForService(service, serviceName)) {
            String ingressId = serviceName;
            String host = "";
            if (StringUtils.isNotBlank(routeDomainPostfix)) {
                host = serviceName + "." + namespace + "." + FileUtil.stripPrefix(routeDomainPostfix, ".");
            }
            List<HTTPIngressPath> paths = new ArrayList<>();
            List<ServicePort> ports = serviceSpec.getPorts();
            if (ports != null) {
                for (ServicePort port : ports) {
                    Integer portNumber = port.getPort();
                    if (portNumber != null) {
                        HTTPIngressPath path =
                            new HTTPIngressPathBuilder()
                                .withNewBackend()
                                  .withServiceName(serviceName)
                                  .withServicePort(KubernetesHelper.createIntOrString(portNumber))
                                .endBackend()
                                .build();
                        paths.add(path);
                    }
                }
            }
            if (paths.isEmpty()) {
                return ingress;
            }
            ingress = new IngressBuilder().
                    withNewMetadata().withName(ingressId).withNamespace(namespace).endMetadata().
                    withNewSpec().
                    addNewRule().
                    withHost(host).
                    withNewHttp().
                    withPaths(paths).
                    endHttp().
                    endRule().
                    endSpec().build();

            String json;
            try {
                json = ResourceUtil.toJson(ingress);
            } catch (JsonProcessingException e) {
                json = e.getMessage() + ". object: " + ingress;
            }
            log.debug("Created ingress: " + json);
        }
        return ingress;
    }


    /**
     * Should we try to create an external URL for the given service?
     * <p/>
     * By default lets ignore the kubernetes services and any service which does not expose ports 80 and 443
     *
     * @return true if we should create an OpenShift Route for this service.
     */
    private boolean shouldCreateExternalURLForService(Service service, String id) {
        if ("kubernetes".equals(id) || "kubernetes-ro".equals(id)) {
            return false;
        }
        Set<Integer> ports = getPorts(service);
        log.debug("Service " + id + " has ports: " + ports);
        if (ports.size() == 1) {
            String type = null;
            ServiceSpec spec = service.getSpec();
            if (spec != null) {
                type = spec.getType();
                if (Objects.equals(type, "LoadBalancer")) {
                    return true;
                }
            }
            log.info("Not generating route for service " + id + " type is not LoadBalancer: " + type);
        } else {
            log.info("Not generating route for service " + id + " as only single port services are supported. Has ports: " + ports);
        }
        return false;
    }

    private boolean hasExactlyOneService(Service service, String id) {
        Set<Integer> ports = getPorts(service);
        if (ports.size() != 1) {
            log.info("Not generating route for service " + id + " as only single port services are supported. Has ports: " +
                     ports);
            return false;
        } else {
            return true;
        }
    }

    private Set<Integer> getPorts(Service service) {
        Set<Integer> answer = new HashSet<>();
        if (service != null) {
            ServiceSpec spec = getOrCreateSpec(service);
            for (ServicePort port : spec.getPorts()) {
                answer.add(port.getPort());
            }
        }
        return answer;
    }

    public static ServiceSpec getOrCreateSpec(Service entity) {
        ServiceSpec spec = entity.getSpec();
        if (spec == null) {
            spec = new ServiceSpec();
            entity.setSpec(spec);
        }
        return spec;
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

        KitLogger serviceLogger = createExternalProcessLogger("[[G]][SVC][[G]] ");
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
                    serviceLogger.info("" + name + ": " + url);
                }
            }
        }
        processCustomEntities(kubernetes, namespace, resources != null ? resources.getCustomResourceDefinitions() : null, false);
    }

    protected String getExternalServiceURL(Service service) {
        return KubernetesHelper.getOrCreateAnnotations(service).get(JKubeAnnotations.SERVICE_EXPOSE_URL.value());
    }

    protected boolean isExposeService(Service service) {
        String expose = KubernetesHelper.getLabels(service).get("expose");
        return expose != null && expose.toLowerCase().equals("true");
    }

    public boolean isRollingUpgrades() {
        return rollingUpgrades;
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


    protected void createRoutes(Collection<HasMetadata> collection) {
        String routeDomainPostfix = this.routeDomain;
        Log log = getLog();
        String namespace = clusterAccess.getNamespace();
        // lets get the routes first to see if we should bother
        try {
            OpenShiftClient openshiftClient = applyService.getOpenShiftClient();
            if (openshiftClient == null) {
                return;
            }
            RouteList routes = openshiftClient.routes().inNamespace(namespace).list();
            if (routes != null) {
                routes.getItems();
            }
        } catch (Exception e) {
            log.warn("Cannot load OpenShift Routes; maybe not connected to an OpenShift platform? " + e, e);
            return;
        }
        List<Route> routes = new ArrayList<>();
        for (Object object : collection) {
            if (object instanceof Service) {
                Service service = (Service) object;
                Route route = createRouteForService(routeDomainPostfix, namespace, service);
                if (route != null) {
                    routes.add(route);
                }
            }
        }
        collection.addAll(routes);
    }

    protected void createIngress(KubernetesClient kubernetesClient, Collection<HasMetadata> collection) {
        String routeDomainPostfix = this.routeDomain;
        Log log = getLog();
        String namespace = clusterAccess.getNamespace();
        List<Ingress> ingressList = null;
        // lets get the routes first to see if we should bother
        try {
            IngressList ingresses = kubernetesClient.extensions().ingresses().inNamespace(namespace).list();
            if (ingresses != null) {
                ingressList = ingresses.getItems();
            }
        } catch (Exception e) {
            log.warn("Cannot load Ingress instances. Must be an older version of Kubernetes? Error: " + e, e);
            return;
        }
        List<Ingress> ingresses = new ArrayList<>();
        for (Object object : collection) {
            if (object instanceof Service) {
                Service service = (Service) object;
                if (!serviceHasIngressRule(ingressList, service)) {
                    Ingress ingress = createIngressForService(routeDomainPostfix, namespace, service);
                    if (ingress != null) {
                        ingresses.add(ingress);
                        log.info("Created ingress for " + namespace + ":" + KubernetesHelper.getName(service));
                    } else {
                        log.debug("No ingress required for " + namespace + ":" + KubernetesHelper.getName(service));
                    }
                } else {
                    log.info("Already has ingress for service " + namespace + ":" + KubernetesHelper.getName(service));
                }
            }
        }
        collection.addAll(ingresses);

    }

    protected void processCustomEntities(KubernetesClient client, String namespace, List<String> customResourceDefinitions, boolean isDelete) throws Exception {
        if(customResourceDefinitions == null)
            return;

        List<CustomResourceDefinitionContext> crdContexts = KubernetesClientUtil.getCustomResourceDefinitionContext(client ,customResourceDefinitions);
        Map<File, String> fileToCrdMap = getCustomResourcesFileToNamemap();

        for(CustomResourceDefinitionContext customResourceDefinitionContext : crdContexts) {
            for(Map.Entry<File, String> entry : fileToCrdMap.entrySet()) {
                if(entry.getValue().equals(customResourceDefinitionContext.getGroup())) {
                    if(isDelete) {
                        applyService.deleteCustomResource(entry.getKey(), namespace, customResourceDefinitionContext);
                    } else {
                        applyService.applyCustomResource(entry.getKey(), namespace, customResourceDefinitionContext);
                    }
                }
            }
        }
    }

    protected Map<File, String> getCustomResourcesFileToNamemap() throws IOException {
        Map<File, String> fileToCrdGroupMap = new HashMap<>();
        File resourceDirFinal = ResourceUtil.getFinalResourceDir(resourceDir, environment);
        File[] resourceFiles = KubernetesResourceUtil.listResourceFragments(resourceDirFinal, resources != null ? resources.getRemotes() : null, log);

        for (File file : resourceFiles) {
            if (file.getName().endsWith("cr.yml") || file.getName().endsWith("cr.yaml")) {
                Map<String, Object> customResource = KubernetesClientUtil.doReadCustomResourceFile(file);
                String apiVersion = customResource.get("apiVersion").toString();
                if (apiVersion.contains("/")) {
                    fileToCrdGroupMap.put(file, apiVersion.split("/")[0]);
                }
            }
        }
        return fileToCrdGroupMap;
    }

    /**
     * Returns true if there is an existing ingress rule for the given service
     */
    private boolean serviceHasIngressRule(List<Ingress> ingresses, Service service) {
        String serviceName = KubernetesHelper.getName(service);
        for (Ingress ingress : ingresses) {
            IngressSpec spec = ingress.getSpec();
            if (spec == null) {
                break;
            }
            List<IngressRule> rules = spec.getRules();
            if (rules == null) {
                break;
            }
            for (IngressRule rule : rules) {
                HTTPIngressRuleValue http = rule.getHttp();
                if (http == null) {
                    break;
                }
                List<HTTPIngressPath> paths = http.getPaths();
                if (paths == null) {
                    break;
                }
                for (HTTPIngressPath path : paths) {
                    IngressBackend backend = path.getBackend();
                    if (backend == null) {
                        break;
                    }
                    if (Objects.equals(serviceName, backend.getServiceName())) {
                        return true;
                    }
                }
            }
        }
        return false;
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

    /**
     * Returns the root project folder
     */
    protected MavenProject getRootProject() {
        MavenProject project = getProject();
        while (project != null) {
            MavenProject parent = project.getParent();
            if (parent == null) {
                break;
            }
            project = parent;
        }
        return project;
    }

}
