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
package org.eclipse.jkube.kit.config.service.kubernetes;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.fabric8.kubernetes.api.model.DeletionPropagation;
import io.fabric8.kubernetes.api.model.DoneablePod;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.LabelSelectorRequirement;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodCondition;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.PodStatus;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.apiextensions.v1beta1.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.ReplicaSet;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.FilterWatchListDeletable;
import io.fabric8.kubernetes.client.dsl.LogWatch;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.PodResource;
import io.fabric8.kubernetes.client.dsl.Scaleable;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.client.OpenShiftClient;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.KubernetesHelper;
import org.eclipse.jkube.kit.common.util.OpenshiftHelper;
import org.eclipse.jkube.kit.config.image.ImageName;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

/**
 * Utility class for executing common tasks using the Kubernetes client
 *
 * @author nicola
 */
public class KubernetesClientUtil {

    private KubernetesClientUtil() {}

    public static void resizeApp(KubernetesClient kubernetes, String namespace, Set<HasMetadata> entities, int replicas, KitLogger log) {
        for (HasMetadata entity : entities) {
            String name = KubernetesHelper.getName(entity);
            Scaleable<?> scalable = null;
            if (entity instanceof Deployment) {
                scalable = kubernetes.apps().deployments().inNamespace(namespace).withName(name);
            } else if (entity instanceof ReplicaSet) {
                scalable = kubernetes.apps().replicaSets().inNamespace(namespace).withName(name);
            } else if (entity instanceof ReplicationController) {
                scalable = kubernetes.replicationControllers().inNamespace(namespace).withName(name);
            } else if (entity instanceof DeploymentConfig) {
                OpenShiftClient openshiftClient = OpenshiftHelper.asOpenShiftClient(kubernetes);
                if (openshiftClient == null) {
                    log.warn("Ignoring DeploymentConfig %s as not connected to an OpenShift cluster", name);
                    continue;
                }
                scalable = openshiftClient.deploymentConfigs().inNamespace(namespace).withName(name);
            }
            if (scalable != null) {
                log.info("Scaling " + KubernetesHelper.getKind(entity) + " " + namespace + "/" + name + " to replicas: " + replicas);
                scalable.scale(replicas, true);
            }
        }
    }

    public static void deleteEntities(KubernetesClient kubernetes, String namespace, Set<HasMetadata> entities, KitLogger log) {
        List<HasMetadata> list = new ArrayList<>(entities);

        // lets delete in reverse order
        Collections.reverse(list);

        for (HasMetadata entity : list) {
            log.info("Deleting resource " + KubernetesHelper.getKind(entity) + " " + namespace + "/" + KubernetesHelper.getName(entity));
            kubernetes.resource(entity).inNamespace(namespace).withPropagationPolicy(DeletionPropagation.BACKGROUND).delete();
        }
    }

    public static void deleteOpenShiftEntities(KubernetesClient kubernetes, String namespace, Set<HasMetadata> entities, String s2iBuildNameSuffix, KitLogger log) {
        // For OpenShift cluster, also delete s2i buildconfig
        OpenShiftClient openshiftClient = OpenshiftHelper.asOpenShiftClient(kubernetes);
        if (openshiftClient == null) {
            return;
        }
        for (HasMetadata entity : entities) {
            if ("ImageStream".equals(KubernetesHelper.getKind(entity))) {
                ImageName imageName = new ImageName(entity.getMetadata().getName());
                String buildName = getS2IBuildName(imageName, s2iBuildNameSuffix);
                log.info("Deleting resource BuildConfig %s/%s and Builds", namespace, buildName);
                openshiftClient.builds().inNamespace(namespace).withLabel("buildconfig", buildName).delete();
                openshiftClient.buildConfigs().inNamespace(namespace).withName(buildName).delete();
            }
        }
    }

    private static String getS2IBuildName(ImageName imageName, String s2iBuildNameSuffix) {
        return imageName.getSimpleName() + s2iBuildNameSuffix;
    }


    public static FilterWatchListDeletable<Pod, PodList, Boolean, Watch> withSelector(NonNamespaceOperation<Pod, PodList, DoneablePod, PodResource<Pod, DoneablePod>> pods, LabelSelector selector, KitLogger log) {
        FilterWatchListDeletable<Pod, PodList, Boolean, Watch> answer = pods;
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

    public static String getPodStatusDescription(Pod pod) {
        return KubernetesHelper.getPodPhase(pod) + " " + getPodCondition(pod);
    }

    public static String getPodStatusMessagePostfix(Watcher.Action action) {
        String message = "";
        switch (action) {
        case DELETED:
            message = ": Pod Deleted";
            break;
        case ERROR:
            message = ": Error";
            break;
        }
        return message;
    }

    protected static String getPodCondition(Pod pod) {
        PodStatus podStatus = pod.getStatus();
        if (podStatus == null) {
            return "";
        }
        List<PodCondition> conditions = podStatus.getConditions();
        if (conditions == null || conditions.isEmpty()) {
            return "";
        }


        for (PodCondition condition : conditions) {
            String type = condition.getType();
            if (StringUtils.isNotBlank(type)) {
                if ("ready".equalsIgnoreCase(type)) {
                    String statusText = condition.getStatus();
                    if (StringUtils.isNotBlank(statusText)) {
                        if (Boolean.parseBoolean(statusText)) {
                            return type;
                        }
                    }
                }
            }
        }
        return "";
    }

    public static Map<String, Object> doCreateCustomResource(KubernetesClient kubernetesClient, CustomResourceDefinitionContext crdContext, String namespace, File customResourceFile) throws IOException {
        if ("Namespaced".equals(crdContext.getScope())) {
            return kubernetesClient.customResource(crdContext).create(namespace, new FileInputStream(customResourceFile.getAbsolutePath()));
        } else {
            return kubernetesClient.customResource(crdContext).create(new FileInputStream(customResourceFile.getAbsolutePath()));
        }
    }

    public static Map<String, Object> doEditCustomResource(KubernetesClient kubernetesClient, CustomResourceDefinitionContext crdContext, String namespace, String name, File customResourceFile) throws IOException {
        if ("Namespaced".equals(crdContext.getScope())) {
            return kubernetesClient.customResource(crdContext).edit(namespace, name, new FileInputStream(customResourceFile.getAbsolutePath()));
        } else {
            return kubernetesClient.customResource(crdContext).edit(name, doGetCustomResourceAsString(customResourceFile));
        }
    }

    public static Map<String, Object> doDeleteCustomResource(
        KubernetesClient kubernetesClient, CustomResourceDefinitionContext crdContext, String namespace, String name)
        throws IOException{

        if ("Namespaced".equals(crdContext.getScope())) {
            return kubernetesClient.customResource(crdContext).delete(namespace, name);
        } else {
            return kubernetesClient.customResource(crdContext).delete(name);
        }
    }

    public static Map<String, Object> doGetCustomResource(KubernetesClient kubernetesClient, CustomResourceDefinitionContext crdContext, String namespace, String name) {
        try {
            if ("Namespaced".equals(crdContext.getScope())) {
                return kubernetesClient.customResource(crdContext).get(namespace, name);
            } else {
                return kubernetesClient.customResource(crdContext).get(name);
            }
        } catch (Exception exception) { // Not found exception
            return null;
        }
    }

    public static String doGetCustomResourceAsString(File customResourceFile) throws IOException {
        String yamlFileAsString = FileUtils.readFileToString(customResourceFile, "UTF-8");
        Object obj = new ObjectMapper(new YAMLFactory()).readValue(yamlFileAsString, Object.class);

        return new ObjectMapper().writeValueAsString(obj);
    }

    public static List<CustomResourceDefinitionContext> getCustomResourceDefinitionContext(KubernetesClient client, List<String> customResources) {
        List<CustomResourceDefinitionContext> crdContexts = new ArrayList<>();
        for(String customResource : customResources) {
            CustomResourceDefinition customResourceDefinition = client.apiextensions().v1beta1().customResourceDefinitions()
                    .withName(customResource).get();
            if(customResourceDefinition != null) {
                crdContexts.add(new CustomResourceDefinitionContext.Builder()
                        .withGroup(customResourceDefinition.getSpec().getGroup())
                        .withName(customResourceDefinition.getMetadata().getName())
                        .withPlural(customResourceDefinition.getSpec().getNames().getPlural())
                        .withVersion(customResourceDefinition.getSpec().getVersion())
                        .withScope(customResourceDefinition.getSpec().getScope())
                        .withKind(customResourceDefinition.getSpec().getNames().getKind())
                        .build());
            }
        }
        return crdContexts;
    }

}
