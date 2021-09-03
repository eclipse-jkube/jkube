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
package org.eclipse.jkube.maven.it;

import java.io.File;
import java.io.IOException;

import org.eclipse.jkube.kit.common.ResourceVerify;

import com.consol.citrus.context.TestContext;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.ParseException;

/**
 * Kept for compatibility reasons
 *
 * @author roland
 * @since 09/12/16
 */
public class Verify {

  private Verify() {
  }

  public static void verifyResourceDescriptors(File actualPath, File expectedPath) throws IOException, ParseException {
    ResourceVerify.verifyResourceDescriptors(actualPath, expectedPath);
  }

  public static void verifyResourceDescriptors(File actualPath, File expectedPath, boolean strict)
      throws IOException, ParseException {
    ResourceVerify.verifyResourceDescriptors(actualPath, expectedPath, strict);
  }

  public static void verifyContentEquals(File actualPath, File expectedPath) throws IOException {
    ResourceVerify.verifyContentEquals(actualPath, expectedPath);
  }

  public static void verifyAbsent(File file, String path) throws IOException {
    ResourceVerify.verifyAbsent(file, path);
  }

  public static Object readWithPath(File file, String path) throws IOException {
    return ResourceVerify.readWithPath(file, path);
  }

  public static TestContext createTestContext() {
    return ResourceVerify.createTestContext();
  }

  public static JSONObject newMessage(String txt) throws ParseException, IOException {
    return ResourceVerify.newMessage(txt);
  }

  public static String asJson(String txt) throws IOException {
    return ResourceVerify.asJson(txt);
  }

  public static String jsonCompatibleYaml(String txt) {
    return ResourceVerify.jsonCompatibleYaml(txt);
  }
}
