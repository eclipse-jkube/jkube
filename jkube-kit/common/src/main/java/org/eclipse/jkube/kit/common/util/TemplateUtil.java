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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TemplateUtil {
  private static final String HELM_DIRECTIVE_REGEX = "\\{\\{.*\\}\\}";

  private TemplateUtil() {
  }

  /**
   * This function will replace all Helm directives with a valid Yaml line containing the base64 encoded Helm directive.
   *
   * Helm lines that are by themselves will be converted like so:
   * <br/>
   * Input:
   *
   * <pre>
   * {{- $myDate := .Value.date }}
   * {{ include "something" . }}
   * someKey: {{ a bit of Helm }}
   * someOtherKey: {{ another bit of Helm }}
   * </pre>
   *
   * Output:
   *
   * <pre>
   * escapedHelm0: BASE64STRINGOFCHARACTERS=
   * escapedHelm1: ANOTHERBASE64STRING=
   * someKey: escapedHelmValueBASE64STRING==
   * someOtherKey: escapedHelmValueBASE64STRING
   * </pre>
   *
   * The <strong>escapedHelm</strong> and <strong>escapedHelmValue</strong> flags are needed for unescaping.
   *
   * @param yaml the input Yaml with Helm directives to be escaped
   * @return the same Yaml, only with Helm directives converted to valid Yaml
   * @see #unescapeYamlTemplate(String)
   */
  public static String escapeYamlTemplate(final String yaml) {
    return escapeYamlTemplateLines(escapeYamlTemplateValues(yaml));
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
  public static String unescapeYamlTemplate(final String template) {
    return unescapeYamlTemplateLines(unescapeYamlTemplateValues(template));
  }

  /**
   * This function is responsible for escaping the Helm directives that are stand-alone.
   * For example:
   *
   * <pre>
   * {{ include "something" . }}
   * </pre>
   *
   * @see #unescapeYamlTemplateLines(String)
   */
  private static String escapeYamlTemplateLines(String template) {
    long escapedHelmIndex = 0;
    final Pattern compile = Pattern.compile("^( *-? *)(" + HELM_DIRECTIVE_REGEX + ".*)$", Pattern.MULTILINE);
    Matcher matcher = compile.matcher(template);
    while (matcher.find()) {
      final String indentation = matcher.group(1);
      final String base64Line = Base64Util.encodeToString(matcher.group(2));
      template = matcher.replaceFirst(indentation + "escapedHelm" + escapedHelmIndex + ": " + base64Line);
      matcher = compile.matcher(template);
      escapedHelmIndex++;
    }
    return template;
  }

  /**
   * This function is responsible for reinstating the stand-alone Helm directives.
   * For example:
   *
   * <pre>
   * BASE64STRINGOFCHARACTERS=
   * </pre>
   *
   * It is the opposite of {@link #escapeYamlTemplateLines(String)}.
   *
   * @see #escapeYamlTemplateLines(String)
   */
  private static String unescapeYamlTemplateLines(String template) {
    final Pattern compile = Pattern.compile("^( *-? *)escapedHelm[\\d]+: \"?(.*?)\"?$", Pattern.MULTILINE);
    Matcher matcher = compile.matcher(template);
    while (matcher.find()) {
      final String indentation = matcher.group(1);
      final String helmLine = Base64Util.decodeToString(matcher.group(2));
      template = matcher.replaceFirst(indentation + helmLine.replace("$", "\\$"));
      matcher = compile.matcher(template);
    }
    return template;
  }

  /**
   * This function is responsible for escaping the Helm directives that are Yaml values.
   * For example:
   *
   * <pre>
   * someKey: {{ a bit of Helm }}
   * </pre>
   *
   * @see #unescapeYamlTemplateValues(String)
   */
  private static String escapeYamlTemplateValues(String template) {
    final Pattern compile = Pattern.compile("^( *[^ ]+ *): *(" + HELM_DIRECTIVE_REGEX + ".*)$", Pattern.MULTILINE);
    Matcher matcher = compile.matcher(template);
    while (matcher.find()) {
      final String indentation = matcher.group(1);
      final String base64Value = Base64Util.encodeToString(matcher.group(2));
      template = matcher.replaceFirst(indentation + ": escapedHelmValue" + base64Value);
      matcher = compile.matcher(template);
    }
    return template;
  }

  /**
   * This function is responsible for reinstating the Helm directives that were Yaml values.
   * For example:
   *
   * <pre>
   * someKey: escapedHelmValueBASE64STRING==
   * </pre>
   *
   * It is the opposite of {@link #escapeYamlTemplateValues(String)}.
   *
   * @see #escapeYamlTemplateValues(String)
   */
  private static String unescapeYamlTemplateValues(String template) {
    final Pattern compile = Pattern.compile("^( *[^ ]+ *): *\"?escapedHelmValue(.*?)\"?$", Pattern.MULTILINE);
    Matcher matcher = compile.matcher(template);
    while (matcher.find()) {
      final String indentation = matcher.group(1);
      final String helmValue = Base64Util.decodeToString(matcher.group(2));
      template = matcher.replaceFirst(indentation + ": " + helmValue.replace("$", "\\$"));
      matcher = compile.matcher(template);
    }
    return template;
  }
}
