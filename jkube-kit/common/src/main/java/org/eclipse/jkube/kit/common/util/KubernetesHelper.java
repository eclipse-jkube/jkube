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
package org.eclipse.jkube.kit.common.util;


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.fabric8.kubernetes.api.model.HTTPHeader;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import io.fabric8.kubernetes.client.utils.ApiVersionUtil;
import org.eclipse.jkube.kit.common.KitLogger;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerPort;
import io.fabric8.kubernetes.api.model.ContainerPortBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.HasMetadataComparator;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.fabric8.kubernetes.api.model.LabelSelectorRequirement;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodCondition;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodStatus;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.ReplicationControllerSpec;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.DaemonSet;
import io.fabric8.kubernetes.api.model.apps.DaemonSetSpec;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpec;
import io.fabric8.kubernetes.api.model.apps.ReplicaSet;
import io.fabric8.kubernetes.api.model.apps.ReplicaSetSpec;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetSpec;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobSpec;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.FilterWatchListDeletable;
import io.fabric8.kubernetes.client.dsl.LogWatch;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.PodResource;
import io.fabric8.openshift.api.model.Build;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.api.model.DeploymentConfigSpec;
import org.apache.commons.lang3.StringUtils;


/**
 * @author roland
 * @since 23.05.17
 */
public class KubernetesHelper {
    protected static final String DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ssX";
    private static final String FILENAME_PATTERN_REGEX = "^(?<name>.*?)(-(?<type>[^-]+))?\\.(?<ext>yaml|yml|json)$";
    private static final String PROFILES_PATTERN_REGEX = "^profiles?\\.ya?ml$";
    public static final Pattern FILENAME_PATTERN = Pattern.compile(FILENAME_PATTERN_REGEX, Pattern.CASE_INSENSITIVE);
    public static final Pattern PROFILES_PATTERN = Pattern.compile(PROFILES_PATTERN_REGEX, Pattern.CASE_INSENSITIVE);
    protected static final String[] POD_CONTROLLER_KINDS =
            { "ReplicationController", "ReplicaSet", "Deployment", "DeploymentConfig", "StatefulSet", "DaemonSet", "Job" };

    private KubernetesHelper() {}

    /**
     * Validates that the given value is valid according to the kubernetes ID parsing rules, throwing an exception if not.
     *
     * @param currentValue current value
     * @param description description
     * @return valid value according to kubernetes ID parsing rules
     */
    public static String validateKubernetesId(String currentValue, String description) {
        if (StringUtils.isBlank(currentValue)) {
            throw new IllegalArgumentException("No " + description + " is specified!");
        }
        int size = currentValue.length();
        for (int i = 0; i < size; i++) {
            char ch = currentValue.charAt(i);
            if (Character.isUpperCase(ch)) {
                throw new IllegalArgumentException("Invalid upper case letter '" + ch + "' at index " + i + " for " + description + " value: " + currentValue);
            }
        }
        return currentValue;
    }

    public static Map<String, String> getOrCreateAnnotations(HasMetadata entity) {
        ObjectMeta metadata = getOrCreateMetadata(entity);
        Map<String, String> answer = metadata.getAnnotations();
        if (answer == null) {
            // use linked so the annotations can be in the FIFO order
            answer = new LinkedHashMap<>();
            metadata.setAnnotations(answer);
        }
        return answer;
    }

    public static ObjectMeta getOrCreateMetadata(HasMetadata entity) {
        ObjectMeta metadata = entity.getMetadata();
        if (metadata == null) {
            metadata = new ObjectMeta();
            entity.setMetadata(metadata);
        }
        return metadata;
    }

    public static Map<String, String> getOrCreateLabels(HasMetadata entity) {
        ObjectMeta metadata = getOrCreateMetadata(entity);
        Map<String, String> answer = metadata.getLabels();
        if (answer == null) {
            // use linked so the annotations can be in the FIFO order
            answer = new LinkedHashMap<>();
            metadata.setLabels(answer);
        }
        return answer;
    }

    /**
     * Returns the resource version for the entity or null if it does not have one
     *
     * @param entity entity as HasMetadata object
     * @return resource version as string value
     */
    public static String getResourceVersion(HasMetadata entity) {
        if (entity != null) {
            ObjectMeta metadata = entity.getMetadata();
            if (metadata != null) {
                String resourceVersion = metadata.getResourceVersion();
                if (StringUtils.isNotBlank(resourceVersion)) {
                    return resourceVersion;
                }
            }
        }
        return null;
    }

    public static Map<String, String> getLabels(HasMetadata entity) {
        if (entity != null) {
            return getLabels(entity.getMetadata());
        }
        return Collections.emptyMap();
    }

    /**
     * Returns the labels of the given metadata object or an empty map if the metadata or labels are null
     *
     * @param metadata metadata object ObjectMeta
     * @return labels in form of a hashmap
     */
    public static Map<String, String> getLabels(ObjectMeta metadata) {
        if (metadata != null) {
            Map<String, String> labels = metadata.getLabels();
            if (labels != null) {
                return labels;
            }
        }
        return Collections.emptyMap();
    }

    public static String getName(HasMetadata entity) {
        if (entity != null) {
            return getName(entity.getMetadata());
        } else {
            return null;
        }
    }

    public static String getName(ObjectMeta entity) {
        if (entity != null) {
            for (String name : new String[]{
                    entity.getName(),
                    getAdditionalPropertyText(entity.getAdditionalProperties(), "id"),
                    entity.getUid()
            }) {
                if (StringUtils.isNotBlank(name)) {
                    return name;
                }
            }
        }
        return null;
    }

    public static String getNamespace(ObjectMeta entity) {
        if (entity != null) {
            return entity.getNamespace();
        } else {
            return null;
        }
    }

    public static String getNamespace(HasMetadata entity) {
        if (entity != null) {
            return getNamespace(entity.getMetadata());
        } else {
            return null;
        }
    }

    /**
     * Returns the kind of the entity
     *
     * @param entity entity as HasMetadata
     * @return kind of resource
     */
    public static String getKind(HasMetadata entity) {
        if (entity != null) {
            // TODO use reflection to find the kind?
            if (entity instanceof KubernetesList) {
                return "List";
            } else {
                return entity.getClass().getSimpleName();
            }
        } else {
            return null;
        }
    }

    /**
     * Returns true if the pod is running
     *
     * @param pod Pod object
     * @return boolean value indicating it's status
     */
    public static boolean isPodRunning(Pod pod) {
        return isInPodPhase(pod, "run");
    }

    public static boolean isPodWaiting(Pod pod) {
        return isInPodPhase(pod, "wait");
    }

    /**
     * Returns true if the pod is running and ready
     *
     * @param pod Pod object
     * @return boolean value indicating it's status
     */
    public static boolean isPodReady(Pod pod) {
        if (!isPodRunning(pod)) {
            return false;
        }

        PodStatus podStatus = pod.getStatus();
        if (podStatus == null) {
            return true;
        }

        List<PodCondition> conditions = podStatus.getConditions();
        if (conditions == null || conditions.isEmpty()) {
            return true;
        }

        // Check "ready" condition
        for (PodCondition condition : conditions) {
            if ("ready".equalsIgnoreCase(condition.getType())) {
                return Boolean.parseBoolean(condition.getStatus());
            }
        }

        return true;
    }

    private static boolean isInPodPhase(Pod pod, String phase) {
        return getPodPhase(pod).toLowerCase().startsWith(phase);
    }

    public static String getPodPhase(Pod pod) {
        if (pod != null) {
            PodStatus podStatus = pod.getStatus();
            if (podStatus != null) {
                String actualPhase = podStatus.getPhase();
                return actualPhase != null ? actualPhase : "";
            }
        }
        return "";
    }


    public static List<Container> getContainers(Pod pod) {
        if (pod != null) {
            PodSpec podSpec = pod.getSpec();
            return getContainers(podSpec);

        }
        return Collections.emptyList();
    }

    public static List<Container> getContainers(PodSpec podSpec) {
        if (podSpec != null) {
            return podSpec.getContainers();
        }
        return Collections.emptyList();
    }

    private static String getAdditionalPropertyText(Map<String, Object> additionalProperties, String name) {
        if (additionalProperties != null) {
            Object value = additionalProperties.get(name);
            if (value != null) {
                return value.toString();
            }
        }
        return null;
    }


    public static String getDefaultNamespace() {
        String ns = new ConfigBuilder().build().getNamespace();
        return ns != null ? ns : "default";
    }

    public static void handleKubernetesClientException(KubernetesClientException e, KitLogger logger) {
        Throwable cause = e.getCause();
        if (cause instanceof UnknownHostException) {
            logger.error( "Could not connect to kubernetes cluster!");
            logger.error("Have you started a local cluster via `mvn jkube:cluster-start` or connected to a remote cluster via `kubectl`?");
            logger.info("For more help see: http://jkube.io/guide/getStarted/");
            logger.error( "Connection error: %s", cause);

            String message = "Could not connect to kubernetes cluster. Have you started a cluster via `minikube start` or connected to a remote cluster via `kubectl`? Error: " + cause;
            throw new IllegalStateException(message, e);
        } else {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    public static List<HasMetadata> loadResources(File manifest) throws IOException {
        return ResourceUtil.deserializeKubernetesListOrTemplate(manifest).stream()
            .distinct()
            .sorted(new HasMetadataComparator())
            .collect(Collectors.toList());
    }

    public static String getBuildStatusPhase(Build build) {
        if (build != null && build.getStatus() != null) {
            return build.getStatus().getPhase();
        }
        return null;
    }

    public static void printLogsAsync(LogWatch logWatcher, final String failureMessage, final CountDownLatch terminateLatch, final KitLogger log) {
        final InputStream in = logWatcher.getOutput();
        Thread thread = new Thread() {
            @Override
            public void run() {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
                    while (true) {
                        String line = reader.readLine();
                        if (line == null) {
                            return;
                        }
                        if (terminateLatch.getCount() <= 0L) {
                            return;
                        }
                        log.info("[[s]]%s", line);
                    }
                } catch (IOException e) {
                    // Check again the latch which could be already count down to zero in between
                    // so that an IO exception occurs on read
                    if (terminateLatch.getCount() > 0L) {
                        log.error("%s : %s", failureMessage, e);
                    }
                }
            }
        };
        thread.start();
    }

    public static String getBuildStatusReason(Build build) {
        if (build != null && build.getStatus() != null) {
            String reason = build.getStatus().getReason();
            String phase = build.getStatus().getPhase();
            if (StringUtils.isNotBlank(phase)) {
                if (StringUtils.isNotBlank(reason)) {
                    return phase + ": " + reason;
                } else {
                    return phase;
                }
            } else {
                return StringUtils.defaultIfEmpty(reason, "");
            }
        }
        return "";
    }

    public static FilterWatchListDeletable<Pod, PodList, PodResource> withSelector(NonNamespaceOperation<Pod, PodList, PodResource> pods, LabelSelector selector, KitLogger log) {
        FilterWatchListDeletable<Pod, PodList, PodResource> answer = pods;
        Map<String, String> matchLabels = selector.getMatchLabels();
        if (matchLabels != null && !matchLabels.isEmpty()) {
            answer = answer.withLabels(matchLabels);
        }
        List<LabelSelectorRequirement> matchExpressions = selector.getMatchExpressions();
        if (matchExpressions != null) {
            for (LabelSelectorRequirement expression : matchExpressions) {
                String key = expression.getKey();
                List<String> values = expression.getValues();
                if (StringUtils.isBlank(key)) {
                    log.warn("Ignoring empty key in selector expression %s", expression);
                    continue;
                }
                if (values == null || values.isEmpty()) {
                    log.warn("Ignoring empty values in selector expression %s", expression);
                    continue;
                }
                String[] valuesArray = values.toArray(new String[values.size()]);
                String operator = expression.getOperator();
                switch (operator) {
                    case "In":
                        answer = answer.withLabelIn(key, valuesArray);
                        break;
                    case "NotIn":
                        answer = answer.withLabelNotIn(key, valuesArray);
                        break;
                    default:
                        log.warn("Ignoring unknown operator %s in selector expression %s", operator, expression);
                }
            }
        }
        return answer;
    }

    public static LabelSelector extractPodLabelSelector(Collection<HasMetadata> entities) {
        LabelSelector chosenSelector = null;
        for (HasMetadata entity : entities) {
            LabelSelector selector = extractPodLabelSelector(entity);
            if (selector != null) {
                if (chosenSelector != null && !chosenSelector.equals(selector)) {
                    throw new IllegalArgumentException("Multiple selectors found for the given entities: " + chosenSelector + " - " + selector);
                }
                chosenSelector = selector;
            }
        }
        return chosenSelector;
    }

    public static LabelSelector extractPodLabelSelector(HasMetadata entity) {
        LabelSelector selector = null;
        if (entity instanceof Deployment) {
            Deployment resource = (Deployment) entity;
            DeploymentSpec spec = resource.getSpec();
            if (spec != null) {
                selector = spec.getSelector();
            }
        } else if (entity instanceof ReplicaSet) {
            ReplicaSet resource = (ReplicaSet) entity;
            ReplicaSetSpec spec = resource.getSpec();
            if (spec != null) {
                selector = spec.getSelector();
            }
        } else if (entity instanceof DeploymentConfig) {
            DeploymentConfig resource = (DeploymentConfig) entity;
            DeploymentConfigSpec spec = resource.getSpec();
            if (spec != null) {
                selector = toLabelSelector(spec.getSelector());
            }
        } else if (entity instanceof ReplicationController) {
            ReplicationController resource = (ReplicationController) entity;
            ReplicationControllerSpec spec = resource.getSpec();
            if (spec != null) {
                selector = toLabelSelector(spec.getSelector());
            }
        } else if (entity instanceof DaemonSet) {
            DaemonSet resource = (DaemonSet) entity;
            DaemonSetSpec spec = resource.getSpec();
            if (spec != null) {
                selector = spec.getSelector();
            }
        } else if (entity instanceof StatefulSet) {
            StatefulSet resource = (StatefulSet) entity;
            StatefulSetSpec spec = resource.getSpec();
            if (spec != null) {
                selector = spec.getSelector();
            }
        } else if (entity instanceof Job) {
            selector = toLabelSelector((Job) entity);
        }
        return selector;
    }

    private static LabelSelector toLabelSelector(Job job) {
      LabelSelector ret = null;
      final JobSpec spec = job.getSpec();
      if (spec != null) {
        if (spec.getSelector() != null) {
          ret = spec.getSelector();
        } else if (spec.getTemplate() != null) {
          ret = toLabelSelector(spec.getTemplate().getMetadata());
        }
      }
      return ret;
    }

    private static LabelSelector toLabelSelector(ObjectMeta metadata) {
      if (metadata != null && metadata.getLabels() != null && !metadata.getLabels().isEmpty()) {
          return toLabelSelector(metadata.getLabels());
      }
      return null;
    }

    private static LabelSelector toLabelSelector(Map<String, String> matchLabels) {
        if (matchLabels != null && !matchLabels.isEmpty()) {
            return new LabelSelectorBuilder().withMatchLabels(matchLabels).build();
        }
        return null;
    }

    public static boolean isNewerResource(HasMetadata newer, HasMetadata older) {
        Date t1 = getCreationTimestamp(newer);
        Date t2 = getCreationTimestamp(older);
        return t1 != null && (t2 == null || t1.compareTo(t2) > 0);
    }

    public static Date getCreationTimestamp(HasMetadata hasMetadata) {
        ObjectMeta metadata = hasMetadata.getMetadata();
        if (metadata != null) {
            return parseTimestamp(metadata.getCreationTimestamp());
        }
        return null;
    }

    private static Date parseTimestamp(String text) {
        if (StringUtils.isBlank(text)) {
            return null;
        }
        return parseDate(text);
    }

    public static Date parseDate(String text) {
        try {
            return new SimpleDateFormat(DATE_TIME_FORMAT).parse(text);
        } catch (ParseException e) {
            return null;
        }
    }

    public static Pod getNewestPod(Collection<Pod> pods) {
        if (pods == null || pods.isEmpty()) {
            return null;
        }
        List<Pod> sortedPods = new ArrayList<>(pods);
        sortedPods.sort((p1, p2) -> {
            Date t1 = getCreationTimestamp(p1);
            Date t2 = getCreationTimestamp(p2);
            if (t1 != null) {
                if (t2 == null) {
                    return 1;
                } else {
                    return t1.compareTo(t2);
                }
            } else if (t2 == null) {
                return 0;
            }
            return -1;
        });
        return sortedPods.get(sortedPods.size() - 1);
    }

    /**
     * Convert a map of env vars to a list of K8s EnvVar objects.
     * @param envVars the name-value map containing env vars
     * @return list of converted env vars
     */
    public static List<EnvVar> convertToEnvVarList(Map<String, String> envVars) {
        List<EnvVar> envList = new LinkedList<>();
        for (Map.Entry<String, String> entry : Optional.ofNullable(envVars).orElse(Collections.emptyMap()).entrySet()) {
            String name = entry.getKey();
            String value = entry.getValue();

            if (name != null) {
                EnvVar env = new EnvVarBuilder().withName(name).withValue(value).build();
                envList.add(env);
            }
        }
        return envList;
    }

    public static boolean setEnvVar(List<EnvVar> envVarList, String name, String value) {
        for (EnvVar envVar : envVarList) {
            String envVarName = envVar.getName();
            if (Objects.equals(name, envVarName)) {
                String oldValue = envVar.getValue();
                if (Objects.equals(value, oldValue)) {
                    return false;
                } else {
                    envVar.setValue(value);
                    return true;
                }
            }
        }
        EnvVar env = new EnvVarBuilder().withName(name).withValue(value).build();
        envVarList.add(env);
        return true;
    }

    public static String getEnvVar(List<EnvVar> envVarList, String name, String defaultValue) {
        String answer = defaultValue;
        if (envVarList != null) {
            for (EnvVar envVar : envVarList) {
                String envVarName = envVar.getName();
                if (Objects.equals(name, envVarName)) {
                    String value = envVar.getValue();
                    if (StringUtils.isNotBlank(value)) {
                        return value;
                    }
                }
            }
        }
        return answer;
    }

    public static boolean removeEnvVar(List<EnvVar> envVarList, String name) {
        boolean removed = false;
        for (Iterator<EnvVar> it = envVarList.iterator(); it.hasNext(); ) {
            EnvVar envVar = it.next();
            String envVarName = envVar.getName();
            if (name.equals(envVarName)) {
                it.remove();
                removed = true;
            }
        }
        return removed;
    }


    /**
     * Get a specific resource fragment ending with some suffix in a specified directory
     *
     * @param resourceDirFinal resource directory
     * @param remotes list remote fragments if provided
     * @param resourceNameSuffix resource name suffix
     * @param log log object
     * @return file if present or null
     */
    public static File getResourceFragmentFromSource(File resourceDirFinal, List<String> remotes, String resourceNameSuffix, KitLogger log) {
        for (File file : listResourceFragments(remotes, log, resourceDirFinal)) {
            if (file.getName().endsWith(resourceNameSuffix)) {
                return file;
            }
        }
        return null;
    }

    /**
     * Get requests or limit objects from string hashmaps
     *
     * @param quantity hashmap of strings
     * @return hashmap of string to quantity
     */
    public static Map<String, Quantity> getQuantityFromString(Map<String, String> quantity) {
        Map<String, Quantity> stringQuantityMap = new HashMap<>();
        if (quantity != null && !quantity.isEmpty()) {
            for (Map.Entry<String, String> entry : quantity.entrySet()) {
                stringQuantityMap.put(entry.getKey(), new Quantity(entry.getValue()));
            }
        }
        return stringQuantityMap;
    }

    public static File[] listResourceFragments(List<String> remotes, KitLogger log, List<File> resourceDirs) {
        return listResourceFragments(remotes, log, resourceDirs.stream().filter(Objects::nonNull).toArray(File[]::new));
    }

    public static File[] listResourceFragments(List<String> remotes, KitLogger log, File... resourceDirs) {
        final List<File> resourceFiles = new ArrayList<>();
        for (File resourceDir : resourceDirs) {
            final File[] resourceFragments = listResourceFragments(resourceDir);
            if (resourceFragments != null) {
                Collections.addAll(resourceFiles, resourceFragments);
            }
        }

        if(remotes != null) {
            File[] remoteResourceFiles = listRemoteResourceFragments(remotes, log);
            if (remoteResourceFiles.length > 0) {
                Collections.addAll(resourceFiles, remoteResourceFiles);
            }
        }
        return resourceFiles.toArray(new File[0]);
    }

    public static File[] listResourceFragments(File resourceDir) {
        if (resourceDir == null) {
            return new File[0];
        }
        return resourceDir.listFiles((File dir, String name) -> FILENAME_PATTERN.matcher(name).matches() && !PROFILES_PATTERN.matcher(name).matches());
    }

    private static File[] listRemoteResourceFragments(List<String> remotes, KitLogger log) {
        if (!remotes.isEmpty()) {
            final File remoteResources = FileUtil.createTempDirectory();
            FileUtil.downloadRemotes(remoteResources, remotes, log);

            if (remoteResources.isDirectory()) {
                return remoteResources.listFiles();
            }
        }
        return new File[0];
    }

    public static String getFullyQualifiedApiGroupWithKind(HasMetadata item) {
        return ApiVersionUtil.joinApiGroupAndVersion(ApiVersionUtil.trimGroup(item.getApiVersion()), ApiVersionUtil.trimVersion(item.getApiVersion())) +
            "#" + item.getKind();
    }

    public static String getNewestApplicationPodName(KubernetesClient client, String namespace, Collection<HasMetadata> resources) {
        LabelSelector selector = extractPodLabelSelector(resources);
        final PodList pods;
        if (namespace != null) {
            pods = client.pods().inNamespace(namespace).withLabelSelector(selector).list();
        } else {
            pods = client.pods().withLabelSelector(selector).list();
        }
        Pod newestPod = KubernetesHelper.getNewestPod(pods.getItems());
        if (newestPod != null) {
            return newestPod.getMetadata().getName();
        }
        return null;
    }

    public static boolean isExposeService(Service service) {
        String expose = KubernetesHelper.getLabels(service).get("expose");
        return expose != null && expose.equalsIgnoreCase("true");
    }

    public static String getAnnotationValue(HasMetadata item, String annotationKey) {
        if (item != null) {
            return getOrCreateAnnotations(item).get(annotationKey);
        }
        return null;
    }

    public static boolean containsPort(List<ContainerPort> ports, String portValue) {
        for (ContainerPort port : ports) {
            Integer containerPort = port.getContainerPort();
            if (containerPort != null && containerPort == Integer.parseInt(portValue)) {
                return true;
            }
        }
        return false;
    }

    public static ContainerPort addPort(String portNumberText, String portName, KitLogger log) {
        if (StringUtils.isBlank(portNumberText)) {
            return null;
        }
        int portValue;
        try {
            portValue = Integer.parseInt(portNumberText);
        } catch (NumberFormatException e) {
            log.warn("Could not parse remote debugging port %s as an integer: %s", portNumberText, e);
            return null;
        }
        return new ContainerPortBuilder().withName(portName).withContainerPort(portValue).build();
    }

    public static boolean isControllerResource(HasMetadata h) {
        return Arrays.stream(POD_CONTROLLER_KINDS).anyMatch(c -> c.equals(h.getKind()));
    }

    public static List<HTTPHeader> convertMapToHTTPHeaderList(Map<String, String> headers) {
        List<HTTPHeader> httpHeaders = new ArrayList<>();
        if (headers != null) {
            for (Map.Entry<String, String> header : headers.entrySet()) {
                httpHeaders.add(new HTTPHeader(header.getKey(), header.getValue()));
            }
        }
        return httpHeaders;
    }
}

