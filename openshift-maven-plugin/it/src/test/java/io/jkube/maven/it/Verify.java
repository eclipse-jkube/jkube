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
package io.jkube.maven.it;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import com.consol.citrus.context.TestContext;
import com.consol.citrus.validation.json.JsonMessageValidationContext;
import com.consol.citrus.validation.json.JsonTextMessageValidator;
import com.consol.citrus.validation.matcher.ValidationMatcherConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;
import org.springframework.util.FileCopyUtils;

/**
 * @author roland
 * @since 09/12/16
 */
public class Verify {

    public static void verifyResourceDescriptors(File actualPath, File expectedPath) throws IOException, ParseException {
        verifyResourceDescriptors(actualPath, expectedPath, false);
    }

    public static void verifyResourceDescriptors(File actualPath, File expectedPath, boolean strict) throws IOException, ParseException {
        String actualText = readFile(actualPath);
        String expectedText = readFile(expectedPath);


        JsonTextMessageValidator validator = new JsonTextMessageValidator();
        validator.setStrict(strict);

        DocumentContext actualContext = JsonPath.parse(actualText);
        validator.validateJson(newMessage(actualText),
                               newMessage(expectedText),
                               new JsonMessageValidationContext(),
                               createTestContext(),
                               actualContext);
    }

    public static void verifyAbsent(File file, String path) throws IOException {
        try {
            readWithPath(file, path);
            throw new RuntimeException("Path " + path + " is present in file " + file.getAbsolutePath());
        } catch(PathNotFoundException a) {
            // Everything fine
        }
    }

    public static Object readWithPath(File file, String path) throws IOException {
        String json = asJson(readFile(file));
        return JsonPath.parse(json).read(path);
    }

    private static String readFile(File path) throws IOException {
        return new String(FileCopyUtils.copyToByteArray(new FileInputStream(path)), Charset.defaultCharset());
    }


    public static TestContext createTestContext() {
        TestContext context = new TestContext();
        context.getValidationMatcherRegistry()
               .getValidationMatcherLibraries()
               .add(new ValidationMatcherConfig().getValidationMatcherLibrary());
        return context;
    }

    public static JSONObject newMessage(String txt) throws ParseException, IOException {
        return (JSONObject) new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE).parse(asJson(txt));
    }

    public static String asJson(String txt) throws IOException {
        Object obj = new ObjectMapper(new YAMLFactory()).readValue(txt, Object.class);
        return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(obj);
    }
}