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
package org.eclipse.jkube.kit.common;

import io.fabric8.kubernetes.api.builder.BaseFluent;
import io.fabric8.kubernetes.api.builder.VisitableBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;

import java.util.HashMap;

public class GenericCustomResourceBuilder extends BaseFluent<GenericCustomResourceBuilder>
  implements VisitableBuilder<GenericCustomResource, GenericCustomResourceBuilder> {

  private final GenericCustomResource genericCustomResource;
  private final ObjectMetaBuilder metadata;

  public GenericCustomResourceBuilder(GenericCustomResource item) {
    this.genericCustomResource = new GenericCustomResource();
    this.genericCustomResource.setApiVersion(item.getApiVersion());
    this.genericCustomResource.setKind(item.getKind());
    this.genericCustomResource.setAdditionalProperties(new HashMap<>(item.getAdditionalProperties()));
    metadata = item.getMetadata() == null ? new ObjectMetaBuilder() : new ObjectMetaBuilder(item.getMetadata());
    _visitables.get("metadata").add(this.metadata);
  }

  @Override
  public GenericCustomResource build() {
    genericCustomResource.setMetadata(metadata.build());
    return genericCustomResource;
  }

}
