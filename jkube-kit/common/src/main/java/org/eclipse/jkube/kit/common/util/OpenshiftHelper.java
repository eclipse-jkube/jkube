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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.fabric8.openshift.api.model.Parameter;
import io.fabric8.openshift.api.model.Template;
import io.fabric8.openshift.client.OpenShiftClient;
import org.apache.commons.lang3.StringUtils;

/**
 * @author roland
 */
public class OpenshiftHelper {

    public static final String DEFAULT_API_VERSION = "v1";

    private OpenshiftHelper() {}

    public static OpenShiftClient asOpenShiftClient(KubernetesClient client) {
        if (client instanceof OpenShiftClient) {
            return (OpenShiftClient) client;
        }

        if (isOpenShift(client)) {
            return client.adapt(OpenShiftClient.class);
        }
        return null;
    }

    public static boolean isOpenShift(KubernetesClient client) {
        return client.adapt(OpenShiftClient.class).isSupported();
    }


    public static KubernetesList processTemplatesLocally(Template entity, boolean failOnMissingParameterValue) throws IOException {
        List<HasMetadata> objects = null;
        if (entity != null) {
            objects = entity.getObjects();
            if (objects == null || objects.isEmpty()) {
                return null;
            }
        }
        List<Parameter> parameters = entity != null ? entity.getParameters() : null;
        if (parameters != null && !parameters.isEmpty()) {
            String json = "{\"kind\": \"List\", \"apiVersion\": \"" + DEFAULT_API_VERSION + "\",\n" +
                    "  \"items\": " + ResourceUtil.toJson(objects) + " }";

            // let's make a few passes in case there's expressions in values
            for (int i = 0; i < 5; i++) {
                for (Parameter parameter : parameters) {
                    String name = parameter.getName();
                    String from = "${" + name + "}";
                    String value = parameter.getValue();

                    // TODO generate random strings for passwords etc!
                    if (StringUtils.isBlank(value)) {
                        if (failOnMissingParameterValue) {
                            throw new IllegalArgumentException("No value available for parameter name: " + name);
                        } else {
                            value = "";
                        }
                    }
                    json = json.replace(from, value);
                }
            }
            return  Serialization.jsonMapper().readerFor(KubernetesList.class).readValue(json);
        } else {
            KubernetesList answer = new KubernetesList();
            answer.setItems(objects);
            return answer;
        }
    }


    public static boolean isCancelled(String status) {
        return "Cancelled".equals(status);
    }

    public static boolean isFailed(String status) {
        return status != null && (status.startsWith("Fail") || status.startsWith("Error"));
    }

    public static boolean isCompleted(String status) {
        return "Complete".equals(status);
    }

    public static boolean isFinished(String status) {
        return isCompleted(status) || isFailed(status) || isCancelled(status);
    }

    public static Template combineTemplates(Template firstTemplate, Template template) {
        List<HasMetadata> objects = template.getObjects();
        if (objects != null) {
            for (HasMetadata object : objects) {
                addTemplateObject(firstTemplate, object);
            }
        }
        List<Parameter> parameters = firstTemplate.getParameters();
        if (parameters == null) {
            parameters = new ArrayList<>();
            firstTemplate.setParameters(parameters);
        }
        combineParameters(parameters, template.getParameters());
        String name = KubernetesHelper.getName(template);
        if (StringUtils.isNotBlank(name)) {
            // let's merge all the jkube annotations using the template id qualifier as a postfix
            Map<String, String> annotations = KubernetesHelper.getOrCreateAnnotations(firstTemplate);
            Map<String, String> otherAnnotations = KubernetesHelper.getOrCreateAnnotations(template);
            Set<Map.Entry<String, String>> entries = otherAnnotations.entrySet();
            for (Map.Entry<String, String> entry : entries) {
                annotations.putIfAbsent(entry.getKey(), entry.getValue());
            }
        }
        return firstTemplate;
    }

    // =============================================================================================

    private static void combineParameters(List<Parameter> parameters, List<Parameter> otherParameters) {
        if (otherParameters != null && !otherParameters.isEmpty()) {
            Map<String, Parameter> map = new HashMap<>();
            for (Parameter parameter : parameters) {
                map.put(parameter.getName(), parameter);
            }
            for (Parameter otherParameter : otherParameters) {
                String name = otherParameter.getName();
                Parameter original = map.get(name);
                if (original == null) {
                    parameters.add(otherParameter);
                } else {
                    if (StringUtils.isNotBlank(original.getValue())) {
                        original.setValue(otherParameter.getValue());
                    }
                }
            }
        }
    }

    private static void addTemplateObject(Template template, HasMetadata object) {
        List<HasMetadata> objects = template.getObjects();
        objects.add(object);
        template.setObjects(objects);
    }

    public static boolean isOpenShiftClient(KubernetesClient kubernetes) {
        return asOpenShiftClient(kubernetes) != null;
    }
}

