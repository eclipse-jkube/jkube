/*
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
package org.eclipse.jkube.kit.resource.helm.oci;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.input.CountingInputStream;

import java.io.IOException;

@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@EqualsAndHashCode
public class OCIManifestLayer {

  private String mediaType;
  private String digest;
  private long size;

  public static OCIManifestLayer from(CountingInputStream blobStream) throws IOException {
    blobStream.mark(Integer.MAX_VALUE);
    final String digest = "sha256:" + DigestUtils.sha256Hex(blobStream);
    final long size = blobStream.getByteCount();
    blobStream.reset();
    blobStream.resetByteCount();
    return OCIManifestLayer.builder()
      .digest(digest)
      .size(size)
      .build();
  }
}
