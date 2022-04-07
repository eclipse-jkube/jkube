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
package org.eclipse.jkube.kit.common.assertj;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.assertj.core.api.AbstractFileAssert;
import org.assertj.core.api.AbstractListAssert;
import org.assertj.core.api.ListAssert;
import org.assertj.core.api.ObjectAssert;

public class FileAssertions extends AbstractFileAssert<FileAssertions> {

  private static final String GIT_KEEP_FILE = ".gitkeep";

  public FileAssertions(File actual) {
    super(actual, FileAssertions.class);
  }

  public static FileAssertions assertThat(File file) {
    return new FileAssertions(file);
  }

  public AbstractListAssert<ListAssert<String>, List<? extends String>, String, ObjectAssert<String>> fileTree()
      throws IOException {

    final Path actualPath = actual.toPath().normalize().toRealPath();
    final List<String> paths = new ArrayList<>();
    try (Stream<Path> pathStream = Files.walk(actualPath)) {
      for (Path temp : pathStream
          .filter(p -> !p.equals(actualPath))
          .filter(p -> !p.toFile().getName().equals(GIT_KEEP_FILE))
          .collect(Collectors.toList())) {
        paths.add(actualPath.relativize(temp.normalize().toRealPath()).toString());
      }
    }
    return org.assertj.core.api.Assertions.assertThat(paths);
  }
}
