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

public class TemplateUtil {

  private TemplateUtil() {
  }

  /**
   * Ported from https://github.com/fabric8io/fabric8-maven-plugin/commit/d6bdaa37e06863677bc01cefa31f7d23c6d5f0f9
   *
   * @param template String to escape
   * @return the escaped Yaml template
   */
  public static String escapeYamlTemplate(String template) {
    StringBuilder answer = new StringBuilder();
    int count = 0;
    char last = 0;
    for (int i = 0, size = template.length(); i < size; i++) {
      char ch = template.charAt(i);
      if (ch == '{' || ch == '}') {
        if (count == 0) {
          last = ch;
          count = 1;
        } else {
          if (ch == last) {
            answer.append(ch == '{' ? "{{\"{{\"}}" : "{{\"}}\"}}");
          } else {
            answer.append(last);
            answer.append(ch);
          }
          count = 0;
          last = 0;
        }
      } else {
        if (count > 0) {
          answer.append(last);
        }
        answer.append(ch);
        count = 0;
        last = 0;
      }
    }
    if (count > 0) {
      answer.append(last);
    }
    return answer.toString();
  }
}
