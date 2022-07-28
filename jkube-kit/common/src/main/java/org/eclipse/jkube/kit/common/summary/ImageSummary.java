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
package org.eclipse.jkube.kit.common.summary;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
public class ImageSummary {
  private String baseImageName;
  private String dockerfilePath;
  private String imageStreamUsed;
  private String imageSha;

  public ImageSummary(String baseImageName, String dockerfilePath, String imageStreamUsed, String imageSha) {
    this.baseImageName = baseImageName;
    this.dockerfilePath = dockerfilePath;
    this.imageStreamUsed = imageStreamUsed;
    this.imageSha = imageSha;
  }
}
