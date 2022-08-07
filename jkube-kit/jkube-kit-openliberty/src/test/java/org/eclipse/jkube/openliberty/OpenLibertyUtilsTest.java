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
package org.eclipse.jkube.openliberty;

import org.eclipse.jkube.kit.common.JavaProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OpenLibertyUtilsTest {

  @TempDir
  File temporaryFolder;
  private JavaProject javaProject;

  @BeforeEach
  void setup() {
    javaProject = mock(JavaProject.class, RETURNS_DEEP_STUBS);
    when(javaProject.getBaseDirectory()).thenReturn(temporaryFolder);
  }

  @Test
  void isMicroProfileHealthEnabled_whenNoServerXmlFile_thenReturnsFalse() {
    // Given + When
    boolean result = OpenLibertyUtils.isMicroProfileHealthEnabled(javaProject);

    // Then
    assertThat(result).isFalse();
  }

  @Test
  void isMicroProfileHealthEnabled_whenServerXmlHasMicroprofileFeature_thenReturnsTrue() throws IOException {
    // Given
    createServerConfigFileWithFeature(createServerConfigFile(), "microProfile-5.0");

    // When
    boolean result = OpenLibertyUtils.isMicroProfileHealthEnabled(javaProject);

    // Then
    assertThat(result).isTrue();
  }

  @Test
  void isMicroProfileHealthEnabled_whenServerXmlHasMicroprofileHealthFeature_thenReturnsTrue() throws IOException {
    // Given
    createServerConfigFileWithFeature(createServerConfigFile(), "mpHealth-4.0");

    // When
    boolean result = OpenLibertyUtils.isMicroProfileHealthEnabled(javaProject);

    // Then
    assertThat(result).isTrue();
  }

  private File createServerConfigFile() throws IOException {
    final File serverConfigDir = Files.createDirectories(temporaryFolder.toPath().resolve("src").resolve("main")
            .resolve("liberty").resolve("config")).toFile();
    return new File(serverConfigDir, "server.xml");
  }

  private void createServerConfigFileWithFeature(File serverConfig, String feature) throws IOException {
    String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<server description=\"new server\">" +
        "<featureManager><feature>%s</feature></featureManager>" +
        "</server>";
    FileWriter writer = new FileWriter(serverConfig);
    writer.write(String.format(content, feature));
    writer.close();
  }
}
