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
package org.eclipse.jkube.kit.config.resource;

import java.io.File;
import java.io.IOException;

import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.ResourceClassifier;

import io.fabric8.kubernetes.api.model.KubernetesList;

public interface ResourceService {

  KubernetesList generateResources(PlatformMode platformMode, EnricherManager enricherManager, KitLogger log)
      throws IOException;

  File writeResources(KubernetesList resources, ResourceClassifier classifier, KitLogger log) throws IOException;

  @FunctionalInterface
  interface ResourceFileProcessor {

    File[] processResources(File[] resources) throws IOException;
  }
}
