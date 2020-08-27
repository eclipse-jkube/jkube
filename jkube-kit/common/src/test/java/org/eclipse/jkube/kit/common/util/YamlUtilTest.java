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
package org.eclipse.jkube.kit.common.util;


import java.net.URL;
import java.util.List;
import java.util.Properties;

import com.fasterxml.jackson.databind.JsonMappingException;
import org.junit.Test;

import static org.eclipse.jkube.kit.common.util.YamlUtil.getPropertiesFromYamlString;
import static org.eclipse.jkube.kit.common.util.YamlUtil.splitYamlResource;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;

public class YamlUtilTest {

  @Test
  public void getPropertiesFromYamlStringEmptyStringTest() throws Exception {
    // Given
    final String yamlString = "";
    // When
    final Properties result = getPropertiesFromYamlString(yamlString);
    // Then
    assertThat(result, notNullValue());
    assertThat(result.size(), is(0));
  }

  @Test
  public void getPropertiesFromYamlStringNullStringTest() throws Exception {
    // When
    final Properties result = getPropertiesFromYamlString(null);
    // Then
    assertThat(result, notNullValue());
    assertThat(result.size(), is(0));
  }

  @Test(expected = JsonMappingException.class)
  public void getPropertiesFromYamlStringInvalidStringTest() throws Exception {
    // Given
    final String yamlString = "not\na\nvalid\nyaml";
    // When
    getPropertiesFromYamlString(yamlString);
    // Then
    fail();
  }

  @Test
  public void getPropertiesFromYamlStringValidStringTest() throws Exception {
    // Given
    final String yamlString = "---\ntest: 1\nlist:\n  - name: item 1\n    value: value 1\nstill-test: 1";
    // When
    final Properties result = getPropertiesFromYamlString(yamlString);
    // Then
    assertThat(result, notNullValue());
    assertThat(result.size(), is(4));
    assertThat(result.getProperty("test"), is("1"));
    assertThat(result.getProperty("list[0].name"), is("item 1"));
    assertThat(result.getProperty("list[0].value"), is("value 1"));
    assertThat(result.getProperty("still-test"), is("1"));
  }

  // https://bugs.eclipse.org/bugs/show_bug.cgi?id=561261
  @Test
  public void getPropertiesFromYamlCWE502Test() throws Exception {
    // Given
    final String yamlString = "maps: !!javax.script.ScriptEngineManager [!!java.net.URLClassLoader [[!!java.net.URL [\\\"http://localhost:9000/\\\"]]]]";
    // When
    final Properties result = getPropertiesFromYamlString(yamlString);
    // Then
    assertThat(result, notNullValue());
    assertThat(result.size(), is(1));
    assertThat(result.getProperty("maps[0][0][0][0]"), is("\\\"http://localhost:9000/\\\""));
  }

  @Test
  public void splitYamlResourceTest() throws Exception {
    // Given
    final URL resource = YamlUtilTest.class.getResource("/util/yaml-list.yml");
    // When
    final List<String> result = splitYamlResource(resource);
    // Then
    assertThat(result, notNullValue());
    assertThat(result, hasSize(4));
    assertThat(result.get(1), containsString("name: \"YAML --- 2\""));
    assertThat(result.get(3), startsWith("---\nname: \"Edge case --- 1"));
  }

}