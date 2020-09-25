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
package org.eclipse.jkube.kit.resource.service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.stream.Collectors;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.openshift.api.model.Parameter;
import io.fabric8.openshift.api.model.Template;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

class TemplateUtil {

  private TemplateUtil() { }

  /**
   * Returns the Template if the list contains a single Template only otherwise returns null
   */
  static Template getSingletonTemplate(KubernetesList resources) {
    // if the list contains a single Template lets unwrap it
    if (resources != null) {
      List<HasMetadata> items = resources.getItems();
      if (items != null && items.size() == 1) {
        HasMetadata singleEntity = items.get(0);
        if (singleEntity instanceof Template) {
          return (Template) singleEntity;
        }
      }
    }
    return null;
  }

  static void interpolateTemplateVariables(KubernetesList resources, File kubernetesYaml) throws IOException {
    final List<Parameter> parameters = listAllParameters(resources);
    if (parameters.isEmpty()) {
      return;
    }
    String kubernetesYamlContent;
    try {
      kubernetesYamlContent = FileUtils.readFileToString(kubernetesYaml, Charset.defaultCharset());
    } catch (IOException e) {
      throw new IOException("Failed to load " + kubernetesYaml + " for template variable replacement", e);
    }
    final String interpolatedKubernetesYamlContent = interpolateTemplateVariables(parameters, kubernetesYamlContent);
    if (!kubernetesYamlContent.equals(interpolatedKubernetesYamlContent)) {
      try {
        FileUtils.writeStringToFile(kubernetesYaml, interpolatedKubernetesYamlContent, Charset.defaultCharset());
      } catch (IOException e) {
        throw new IOException("Failed to save " + kubernetesYaml + " after replacing template expressions", e);
      }
    }
  }

  private static String interpolateTemplateVariables(List<Parameter> parameters, String text) {
    for (Parameter parameter : parameters) {
      final String from = "${" + parameter.getName() + "}";
      final String to = parameter.getValue();
      if (StringUtils.isNotBlank(to)) {
        text = text.replace(from, to);
      }
    }
    return text;
  }

  private static List<Parameter> listAllParameters(KubernetesList resources) {
    return resources.getItems().stream()
        .filter(template -> template instanceof Template)
        .map(Template.class::cast)
        .map(Template::getParameters)
        .flatMap(List::stream)
        .collect(Collectors.toList());
  }
}
