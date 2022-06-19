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

    final Path actualPath = actual.toPath().normalize();
    try (Stream<Path> pathStream = Files.walk(actualPath)) {
      return org.assertj.core.api.Assertions.assertThat(pathStream
              .filter(p -> !p.equals(actualPath) && !p.getFileName().toString().equals(GIT_KEEP_FILE))
              .map(p -> actualPath.relativize(p.normalize()).toString())
              .collect(Collectors.toList())
      );
    }
  }
}
