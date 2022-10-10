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
package org.eclipse.jkube.generator.api.support;

import java.util.Map;

import org.eclipse.jkube.generator.api.PortsExtractor;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.PrefixedLogger;
import org.eclipse.jkube.kit.common.util.FileUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

class AbstractPortsExtractorTest {

    private JavaProject project;
    private PrefixedLogger logger;

    @BeforeEach
    void setUp() {
        project = mock(JavaProject.class);
        logger = new PrefixedLogger("test", new KitLogger.SilentLogger());
    }

    @Nested
    @DisplayName("read config")
    class ReadConfig {

      @DisplayName("from valid file")
      @ParameterizedTest(name = "''{0}'' file, config should be added")
      @ValueSource(strings = { ".json", ".yaml", "-nested.yaml", ".properties", "++suffix.yaml" })
      void readConfig(String path) {
        Map<String, Integer> map = extractFromFile("vertx.config", "AbstractPortsExtractorTest" + path);
        assertThat(map)
            .containsEntry("http.port", 80)
            .containsEntry("https.port", 443);
      }

      @Test
      @DisplayName("from invalid extension file, should throw exception")
      void fromInvalidFileExtension_shouldThrowException() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> extractFromFile("vertx.config.test", "AbstractPortsExtractorTest" + ".bla"))
            .withMessageContaining("Unknown file extension.");
      }

      @Test
      @DisplayName("without file, config should be empty")
      void withNoFile_shouldBeEmpty() {
        Map<String, Integer> map = extractFromFile("vertx.config", null);
        assertThat(map).isNotNull().isEmpty();
      }

      @Test
      @DisplayName("from non-existing file, config should be empty")
      void configFileDoesNotExist_shouldBeEmpty() {
        final String nonExistingFile = "/bla/blub/lalala/config.yml";
        System.setProperty("vertx.config.test", nonExistingFile);
        try {
          Map<String, Integer> map = extractFromFile("vertx.config.test", null);
          assertThat(map).isNotNull().isEmpty();
        } finally {
          System.getProperties().remove("vertx.config.test");
        }
      }
    }

    @Test
    void keyPatterns() {
      Map<String, Integer> map = extractFromFile("vertx.config", getClass().getSimpleName() + "-pattern-keys.yml");
      assertThat(map)
          .containsKey("web.port")
          .containsKey("web_port")
          .containsKey("webPort")
          .doesNotContainKey("ssl.support")
          .doesNotContainKey("ports")
          .doesNotContainKey("ports.http")
          .doesNotContainKey("ports.https");
    }

    @Test
    void addPortToList() {
      Map<String, Integer> map = extractFromFile("vertx.config", getClass().getSimpleName() + "-pattern-values.yml");
      assertThat(map)
          .containsEntry("http.port", 8080)
          .containsEntry("https.port", 443)
          .containsEntry("ssh.port", 22)
          .doesNotContainKey("ssh.enabled");
    }

    @Test
    @DisplayName("without property, config should be empty")
    void noProperty_shouldBeEmpty() {
        Map<String, Integer> map = extractFromFile(null, getClass().getSimpleName() + ".yml");
        assertThat(map).isNotNull().isEmpty();
    }

    // ===========================================================================================================

    private Map<String, Integer> extractFromFile(final String propertyName, final String path) {
        PortsExtractor extractor = new AbstractPortsExtractor(logger) {
            @Override
            public String getConfigPathPropertyName() {
                return propertyName;
            }

            @Override
            public String getConfigPathFromProject(JavaProject project) {
                // working on Windows: https://stackoverflow.com/a/31957696/3309168
                return path != null ? FileUtil.getAbsolutePath(getClass().getResource(path)) : null;
            }
        };
        return extractor.extract(project);
    }
}
