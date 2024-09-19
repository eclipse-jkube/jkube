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
package org.eclipse.jkube.kit.common.util;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TemplateUtil {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final String HELM_END_TAG = "}}";
  private static final String HELM_START_TAG = "{{";

  private static final String YAML_HELM_ESCAPE_KEY = "escapedHelm";
  private static final String YAML_LIST_TAG = "-";
  private static final String YAML_KEY_VALUE_SEPARATOR = ": ";

  private TemplateUtil() {
  }

  /**
   * This function will replace all single line Helm directives with a valid Yaml line containing the base64 encoded Helm
   * directive.
   * Helm directives that are values (so not a full line) will be left alone.
   *
   * Helm lines that are by themselves will be converted like so:
   * <br/>
   * Input:
   *
   * <pre>
   * {{- $myDate := .Value.date }}
   * {{ include "something" . }}
   * myKey: "Regular line"
   * someKey: {{ a bit of Helm }}
   * </pre>
   *
   * Output:
   *
   * <pre>
   * escapedHelm0: e3stICA6PSAuVmFsdWUuZGF0ZSB9fQ==
   * escapedHelm1: e3sgaW5jbHVkZSBzb21ldGhpbmcgLiB9fQ==
   * myKey: "Regular line"
   * someKey: {{ a bit of Helm }}
   * </pre>
   *
   * The <strong>escapedHelm</strong> and <strong>escapedHelmValue</strong> flags are needed for unescaping.
   *
   * @param yaml the input Yaml with Helm directives to be escaped
   * @return the same Yaml, only with Helm directives converted to valid Yaml
   * @see #unescapeYamlTemplate(String)
   */
  public static String escapeYamlTemplate(final String yaml) {
    final AtomicLong helmEscapeIndex = new AtomicLong();
    return iterateOverLines(yaml,
        (line, lineEnding, lineNumber) -> escapeYamlLine(line, lineNumber, helmEscapeIndex) + lineEnding);
  }

  private static String escapeYamlLine(final String line, final long lineNumber, final AtomicLong helmEscapeIndex) {
    if (line.trim().startsWith(HELM_START_TAG)) {
      // Line starts with optional indenting and then '{{', so replace whole line
      checkHelmStartAndEndTags(line, lineNumber);

      final int startOfHelm = line.indexOf(HELM_START_TAG);
      final String indentation = line.substring(0, startOfHelm);
      final String base64EncodedLine = Base64Util.encodeToString(line.substring(startOfHelm));
      final long currentHelmEscapeIndex = helmEscapeIndex.getAndIncrement();
      return indentation + YAML_HELM_ESCAPE_KEY + currentHelmEscapeIndex + YAML_KEY_VALUE_SEPARATOR + base64EncodedLine;
    } else if (line.trim().startsWith(YAML_LIST_TAG)) {
      // Line starts with optional indenting and then '-'. Strip the '-', parse again, readd the '-'.
      final int startOfRestOfLine = line.indexOf(YAML_LIST_TAG) + YAML_LIST_TAG.length();
      final String prefix = line.substring(0, startOfRestOfLine);
      final String restOfLine = line.substring(startOfRestOfLine);
      return prefix + escapeYamlLine(restOfLine, lineNumber, helmEscapeIndex);
    } else if (line.contains(YAML_KEY_VALUE_SEPARATOR)) {
      // Line is a "key: value" line. Strip the key, parse again, readd the key.
      final int startOfRestOfLine = line.indexOf(YAML_KEY_VALUE_SEPARATOR) + YAML_KEY_VALUE_SEPARATOR.length();
      final String key = line.substring(0, startOfRestOfLine);
      final String value = line.substring(startOfRestOfLine);

      if (value.trim().startsWith(HELM_START_TAG)) {
        checkHelmStartAndEndTags(value, lineNumber);

        final String base64EncodedLine = Base64Util.encodeToString(value);
        return key + YAML_HELM_ESCAPE_KEY + base64EncodedLine;
      } else {
        // Value was a regular value
        return line;
      }
    } else {
      // Line was a regular line
      return line;
    }
  }

  private static void checkHelmStartAndEndTags(final String line, final long lineNumber) {
    final int startCount = StringUtils.countMatches(line, HELM_START_TAG);
    final int endCount = StringUtils.countMatches(line, HELM_END_TAG);
    if (startCount != endCount) {
      LOG.warn("Found {} Helm start tag{} ('{}') but {} end tag{} ('{}') on line {}. "
          + "Expected this to be equal! Note that multi-line Helm directives are not supported.",
          startCount, startCount == 1 ? "" : "s", HELM_START_TAG,
              endCount, endCount == 1 ? "" : "s", HELM_END_TAG,
                  lineNumber);
    }
  }

  /**
   * This function will replace all escaped Helm directives by {@link #escapeYamlTemplate(String)} back to actual Helm.
   * <br/>
   * This function promises to be the opposite of {@link #escapeYamlTemplate(String)}.
   *
   * @param template the input Yaml that was returned by a call to {@link #escapeYamlTemplate(String)}
   * @return the Yaml that was originally provided to {@link #escapeYamlTemplate(String)}
   * @see #escapeYamlTemplate(String)
   */
  public static String unescapeYamlTemplate(final String yaml) {
    return iterateOverLines(yaml, (line, lineEnding, lineNumber) -> unescapeYamlLine(line) + lineEnding);
  }

  private static String unescapeYamlLine(final String line) {
    if (line.trim().startsWith(YAML_HELM_ESCAPE_KEY)) {
      // Line starts with optional indenting and then 'escapedHelm', so replace whole line
      final int endOfIndentation = line.indexOf(YAML_HELM_ESCAPE_KEY);
      final int startOfBase64Helm = line.indexOf(YAML_KEY_VALUE_SEPARATOR) + YAML_KEY_VALUE_SEPARATOR.length();
      final String indentation = line.substring(0, endOfIndentation);
      final String base64DecodedLine = Base64Util.decodeToString(line.substring(startOfBase64Helm));
      return indentation + base64DecodedLine;
    } else if (line.trim().startsWith(YAML_LIST_TAG)) {
      // Line starts with optional indenting and then '-'. Strip the '-', parse again, readd the '-'.
      final int startOfRestOfLine = line.indexOf(YAML_LIST_TAG) + YAML_LIST_TAG.length();
      final String prefix = line.substring(0, startOfRestOfLine);
      final String restOfLine = line.substring(startOfRestOfLine);
      return prefix + unescapeYamlLine(restOfLine);
    } else if (line.contains(YAML_KEY_VALUE_SEPARATOR)) {
      // Line is a "key: value" line. Strip the key, parse again, readd the key.
      final int startOfRestOfLine = line.indexOf(YAML_KEY_VALUE_SEPARATOR) + YAML_KEY_VALUE_SEPARATOR.length();
      final String key = line.substring(0, startOfRestOfLine);
      final String value = line.substring(startOfRestOfLine);
      if (value.trim().startsWith(YAML_HELM_ESCAPE_KEY)) {
        final String base64DecodedLine = Base64Util.decodeToString(value.substring(YAML_HELM_ESCAPE_KEY.length()));
        return key + base64DecodedLine;
      } else {
        // Value was a regular value
        return line;
      }
    } else {
      // Line was a regular line
      return line;
    }
  }

  private static String iterateOverLines(final String yaml, final IterateOverLinesCallback iterator) {
    final Matcher matcher = Pattern.compile("(.*)(\\R|$)").matcher(yaml);
    final StringBuilder result = new StringBuilder();
    int index = 0;
    long lineNumber = 0;
    while (matcher.find(index) && matcher.start() != matcher.end()) {
      final String line = matcher.group(1);
      final String lineEnding = matcher.group(2);

      final String escapedYamlLine = iterator.onLine(line, lineEnding, lineNumber);
      lineNumber++;

      result.append(escapedYamlLine);

      index = matcher.end();
    }
    return result.toString();
  }

  @FunctionalInterface
  private interface IterateOverLinesCallback {
    String onLine(String input, String lineEnding, long lineNumber);
  }
}
