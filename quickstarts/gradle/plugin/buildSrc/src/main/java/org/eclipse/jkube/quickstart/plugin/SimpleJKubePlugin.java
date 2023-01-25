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
package org.eclipse.jkube.quickstart.plugin;

import org.eclipse.jkube.api.JKubePlugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.file.Files;
import java.time.Instant;

public class SimpleJKubePlugin implements JKubePlugin {

  private static final String SPRING_BOOT_LATEST_DOC_URL =
    "https://docs.spring.io/spring-boot/docs/current/reference/pdf/spring-boot-reference.pdf";

  @Override
  public void addExtraFiles(File targetDir) {
    try {
      generateVersionTimestamp(targetDir);
      downloadSpringBootDocumentation(targetDir);
    } catch (IOException ioException) {
      throw new IllegalStateException("Failure in adding extra files ", ioException);
    }
  }

  private void downloadSpringBootDocumentation(File targetDir) throws IOException {
    try (FileOutputStream fos = new FileOutputStream(new File(targetDir, "spring-boot-doc.pdf"));
         InputStream is = new URL(SPRING_BOOT_LATEST_DOC_URL).openStream()) {
      fos.getChannel().transferFrom(Channels.newChannel(is), 0, Long.MAX_VALUE);
    }
  }

  private static void generateVersionTimestamp(File targetDir) throws IOException {
    Files.writeString(targetDir.toPath().resolve("build-timestamp"), Instant.now().toString());
  }
}
