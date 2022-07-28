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

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.HasMetadataComparator;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.openshift.api.model.Template;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.ResourceFileType;
import org.eclipse.jkube.kit.common.util.KubernetesHelper;
import org.eclipse.jkube.kit.common.util.ResourceUtil;
import org.eclipse.jkube.kit.common.util.SummaryUtil;
import org.eclipse.jkube.kit.enricher.api.util.KubernetesResourceUtil;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.eclipse.jkube.kit.resource.service.TemplateUtil.getSingletonTemplate;

class WriteUtil {

  private static final HasMetadataComparator COMPARATOR = new HasMetadataComparator();

  private WriteUtil(){ }

  static File writeResourcesIndividualAndComposite(
      KubernetesList resources, File resourceFileBase, ResourceFileType resourceFileType, KitLogger log) throws IOException {

    resources.getItems().sort(COMPARATOR);
    // entity is object which will be sent to writeResource for openshift.yml
    // if generateRoute is false, this will be set to resources with new list
    // otherwise it will be set to resources with old list.
    KubernetesResource entity = resources;

    // if the list contains a single Template lets unwrap it
    // in resources already new or old as per condition is set.
    // no need to worry about this for dropping Route.
    final Template template = getSingletonTemplate(resources);
    if (template != null) {
      template.getObjects().sort(COMPARATOR);
      entity = template;
    }

    File file = writeResource(resourceFileBase, entity, resourceFileType);

    // write separate files, one for each resource item
    // resources passed to writeIndividualResources is also new one.
    writeIndividualResources(resources, resourceFileBase, resourceFileType, log);
    return file;
  }

  private static void writeIndividualResources(
      KubernetesList resources, File targetDir, ResourceFileType resourceFileType, KitLogger log) throws IOException {
    final Map<String, Integer> generatedFiles = new HashMap<>();
    for (HasMetadata item : resources.getItems()) {
      String name = KubernetesHelper.getName(item);
      if (StringUtils.isBlank(name)) {
        log.error("No name for generated item %s", item);
        continue;
      }
      String fileName = KubernetesResourceUtil.getNameWithSuffix(name, item.getKind());
      int fileCount = generatedFiles.compute(fileName, (f, i) -> i == null ? 0 : i + 1);
      if (fileCount > 0) {
        fileName = KubernetesResourceUtil.getNameWithSuffix(name + "-" + fileCount, item.getKind());
      }

      // Here we are writing individual file for all the resources.
      File itemTarget = new File(targetDir, fileName);
      SummaryUtil.addGeneratedResourceFile(Optional.ofNullable(resourceFileType)
          .map(r -> r.addExtensionIfMissing(itemTarget))
          .orElse(itemTarget));
      writeResource(itemTarget, item, resourceFileType);
    }
  }

  static File writeResource(File resourceFileBase, KubernetesResource entity, ResourceFileType resourceFileType)
      throws IOException {
    try {
      return ResourceUtil.save(resourceFileBase, entity, resourceFileType);
    } catch (IOException e) {
      throw new IOException("Failed to write resource to " + resourceFileBase + ".", e);
    }
  }
}
