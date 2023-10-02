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
package org.eclipse.jkube.kit.common;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;

import com.consol.citrus.context.TestContext;
import com.consol.citrus.spi.SimpleReferenceResolver;
import com.consol.citrus.validation.json.JsonMessageValidationContext;
import com.consol.citrus.validation.json.JsonTextMessageValidator;
import com.consol.citrus.validation.matcher.DefaultValidationMatcherLibrary;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;
import org.springframework.util.FileCopyUtils;

import static com.jayway.jsonpath.Option.REQUIRE_PROPERTIES;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author roland
 * @since 09/12/16
 */
@SuppressWarnings("java:S5960")
public class ResourceVerify {

  private ResourceVerify() {
  }

  public static void verifyResourceDescriptors(File actualPath, File expectedPath) throws IOException, ParseException {
    verifyResourceDescriptors(actualPath, expectedPath, false);
  }

  public static void verifyResourceDescriptors(File actualPath, File expectedPath, boolean strict)
      throws IOException, ParseException {
    verifyResourceDescriptors(readFile(actualPath), readFile(expectedPath), strict);
  }

  public static void verifyResourceDescriptors(String actualText, String expectedText, boolean strict)
      throws IOException, ParseException {
    JsonTextMessageValidator validator = new JsonTextMessageValidator();
    validator.setStrict(strict);

    DocumentContext actualContext = JsonPath.parse(actualText);
    validator.validateJson("", newMessage(actualText),
        newMessage(expectedText),
        new JsonMessageValidationContext(),
        createTestContext(),
        actualContext);
  }

  public static void verifyContentEquals(File actualPath, File expectedPath) throws IOException {
    assertThat(readFile(actualPath)).isEqualTo(readFile(expectedPath));
  }

  public static void verifyAbsent(File file, String path) throws IOException {
    try {
      readWithPath(file, path);
      throw new AssertionError("Path " + path + " is present in file " + file.getAbsolutePath());
    } catch (PathNotFoundException a) {
      // Everything fine
    }
  }

  public static Object readWithPath(File file, String path) throws IOException {
    String json = asJson(readFile(file));
    return JsonPath.parse(json, Configuration.builder().options(REQUIRE_PROPERTIES).build()).read(path);
  }

  public static String readFile(File path) throws IOException {
    return new String(FileCopyUtils.copyToByteArray(Files.newInputStream(path.toPath())), Charset.defaultCharset());
  }

  public static TestContext createTestContext() {
    TestContext context = new TestContext();
    context.getValidationMatcherRegistry()
        .getValidationMatcherLibraries()
        .add(new DefaultValidationMatcherLibrary());
    context.setReferenceResolver(new SimpleReferenceResolver());
    return context;
  }

  public static JSONObject newMessage(String txt) throws ParseException, IOException {
    return (JSONObject) new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE).parse(asJson(txt));
  }

  public static String asJson(String txt) throws IOException {
    Object obj = new ObjectMapper(new YAMLFactory()).readValue(jsonCompatibleYaml(txt), Object.class);
    return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(obj);
  }

  public static String jsonCompatibleYaml(String txt) {
    return txt.replace("{{", "{").replace("}}", "}");
  }
}
