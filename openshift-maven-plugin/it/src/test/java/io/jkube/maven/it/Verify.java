/**
 * Copyright 2016 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
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