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
package org.eclipse.jkube.quickstart.kit;

import org.eclipse.jkube.api.JKubePlugin;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Set;

public class FooScriptLoader implements JKubePlugin {
  private static final String FOO_SCRIPT = "foo-java.sh";
  private static final String LOCATION_FOO_SCRIPT = "/foo-java-sh/fp-files/" + FOO_SCRIPT;

  @Override
  public void addExtraFiles(File targetDir) {
    try {
      File runJavaDir = new File(targetDir, "foo-java");
      if (!runJavaDir.exists()) {
        if (!runJavaDir.mkdir()) {
          throw new IOException("Couldn't create directory " + runJavaDir.getPath());
        }
      }
      copyFooScript(new File(runJavaDir, FOO_SCRIPT));
    } catch (IOException ioException) {
      throw new IllegalStateException("Failure in adding extra files ", ioException);
    }
  }

  private static File copyFooScript(File destination) throws IOException {
    Path targetPath;
    if (destination.isDirectory()) {
      targetPath = new File(destination, FOO_SCRIPT).toPath();
    } else {
      if (!destination.getAbsoluteFile().getParentFile().exists()) {
        throw new IOException(String.format("%s is not a directory", destination.getParentFile()));
      }
      targetPath = destination.toPath();
    }
    Files.copy(FooScriptLoader.class.getResourceAsStream(LOCATION_FOO_SCRIPT), targetPath, StandardCopyOption.REPLACE_EXISTING);
    setPermissionOnUnix(targetPath);
    return targetPath.toFile();
  }

  private static void setPermissionOnUnix(Path targetPath) throws IOException {

    if (hasPosixFileSystem(targetPath)) {
      Set<PosixFilePermission> perms = new HashSet<>(Files.getPosixFilePermissions(targetPath));
      perms.add(PosixFilePermission.OWNER_EXECUTE);
      perms.add(PosixFilePermission.GROUP_EXECUTE);
      perms.add(PosixFilePermission.OTHERS_EXECUTE);
      Files.setPosixFilePermissions(targetPath, perms);
    }
  }

  private static boolean hasPosixFileSystem(Path targetPath) {
    return targetPath.getFileSystem().provider().getFileAttributeView(targetPath, PosixFileAttributeView.class) != null;
  }
}