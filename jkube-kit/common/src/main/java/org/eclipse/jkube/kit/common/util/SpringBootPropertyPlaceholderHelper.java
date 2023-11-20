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

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

/**
 * Ported/inspired from
 * <a href="https://github.com/spring-projects/spring-framework/blob/7b14606d92bfe36fea048137e794cfff18e9e42a/spring-core/src/main/java/org/springframework/util/PropertyPlaceholderHelper.java">PropertyPlaceholderHelper</a>
 */
public class SpringBootPropertyPlaceholderHelper {

  private static final Map<String, String> wellKnownSimplePrefixes = new HashMap<>(4);

  private final String placeholderPrefix;

  private final String placeholderSuffix;

  private final String simplePrefix;

  private final String valueSeparator;

  private final boolean ignoreUnresolvablePlaceholders;

  public SpringBootPropertyPlaceholderHelper(String placeholderPrefix, String placeholderSuffix) {
    this(placeholderPrefix, placeholderSuffix, null, true);
  }

  public SpringBootPropertyPlaceholderHelper(String placeholderPrefix, String placeholderSuffix,
                                             String valueSeparator, boolean ignoreUnresolvablePlaceholders) {

    Objects.requireNonNull(placeholderPrefix, "'placeholderPrefix' must not be null");
    Objects.requireNonNull(placeholderSuffix, "'placeholderSuffix' must not be null");
    this.placeholderPrefix = placeholderPrefix;
    this.placeholderSuffix = placeholderSuffix;
    String simplePrefixForSuffix = wellKnownSimplePrefixes.get(this.placeholderSuffix);
    if (simplePrefixForSuffix != null && this.placeholderPrefix.endsWith(simplePrefixForSuffix)) {
      this.simplePrefix = simplePrefixForSuffix;
    } else {
      this.simplePrefix = this.placeholderPrefix;
    }
    this.valueSeparator = valueSeparator;
    this.ignoreUnresolvablePlaceholders = ignoreUnresolvablePlaceholders;
  }

  public Properties replaceAllPlaceholders(Properties properties) {
    Objects.requireNonNull(properties, "'properties' must not be null");
    final Properties result = new Properties();
    for (Map.Entry<Object, Object> entry : properties.entrySet()) {
      result.put(entry.getKey(), replacePlaceholders((String) entry.getValue(), properties));
    }
    return result;
  }

  public String replacePlaceholders(String value, final Properties properties) {
    Objects.requireNonNull(properties, "'properties' must not be null");
    return replacePlaceholders(value, properties::getProperty);
  }

  public String replacePlaceholders(String value, PlaceholderResolver placeholderResolver) {
    Objects.requireNonNull(value, "'value' must not be null");
    return parseStringValue(value, placeholderResolver, null);
  }

  private String parseStringValue(
    String value, PlaceholderResolver placeholderResolver,  Set<String> visitedPlaceholders) {

    int startIndex = value.indexOf(this.placeholderPrefix);
    if (startIndex == -1) {
      return value;
    }

    StringBuilder result = new StringBuilder(value);
    while (startIndex != -1) {
      int endIndex = findPlaceholderEndIndex(result, startIndex);
      if (endIndex != -1) {
        String placeholder = result.substring(startIndex + this.placeholderPrefix.length(), endIndex);
        String originalPlaceholder = placeholder;
        if (visitedPlaceholders == null) {
          visitedPlaceholders = new HashSet<>(4);
        }
        if (!visitedPlaceholders.add(originalPlaceholder)) {
          throw new IllegalArgumentException(
            "Circular placeholder reference '" + originalPlaceholder + "' in property definitions");
        }
        // Recursive invocation, parsing placeholders contained in the placeholder key.
        placeholder = parseStringValue(placeholder, placeholderResolver, visitedPlaceholders);
        // Now obtain the value for the fully resolved key...
        String propVal = placeholderResolver.resolvePlaceholder(placeholder);
        if (propVal == null && this.valueSeparator != null) {
          int separatorIndex = placeholder.indexOf(this.valueSeparator);
          if (separatorIndex != -1) {
            String actualPlaceholder = placeholder.substring(0, separatorIndex);
            String defaultValue = placeholder.substring(separatorIndex + this.valueSeparator.length());
            propVal = placeholderResolver.resolvePlaceholder(actualPlaceholder);
            if (propVal == null) {
              propVal = defaultValue;
            }
          }
        }
        if (propVal != null) {
          // Recursive invocation, parsing placeholders contained in the
          // previously resolved placeholder value.
          propVal = parseStringValue(propVal, placeholderResolver, visitedPlaceholders);
          result.replace(startIndex, endIndex + this.placeholderSuffix.length(), propVal);
          startIndex = result.indexOf(this.placeholderPrefix, startIndex + propVal.length());
        }
        else if (this.ignoreUnresolvablePlaceholders) {
          // Proceed with unprocessed value.
          startIndex = result.indexOf(this.placeholderPrefix, endIndex + this.placeholderSuffix.length());
        }
        else {
          throw new IllegalArgumentException("Could not resolve placeholder '" +
            placeholder + "'" + " in value \"" + value + "\"");
        }
        visitedPlaceholders.remove(originalPlaceholder);
      }
      else {
        startIndex = -1;
      }
    }
    return result.toString();
  }

  private int findPlaceholderEndIndex(CharSequence buf, int startIndex) {
    int index = startIndex + this.placeholderPrefix.length();
    int withinNestedPlaceholder = 0;
    while (index < buf.length()) {
      if (StringUtils.startsWith(buf.subSequence(index, buf.length()), this.placeholderSuffix)) {
        if (withinNestedPlaceholder > 0) {
          withinNestedPlaceholder--;
          index = index + this.placeholderSuffix.length();
        }
        else {
          return index;
        }
      }
      else if (StringUtils.startsWith(buf.subSequence(index, buf.length()), this.simplePrefix)) {
        withinNestedPlaceholder++;
        index = index + this.simplePrefix.length();
      }
      else {
        index++;
      }
    }
    return -1;
  }

  /**
   * Strategy interface used to resolve replacement values for placeholders contained in Strings.
   */
  @FunctionalInterface
  public interface PlaceholderResolver {

    /**
     * Resolve the supplied placeholder name to the replacement value.
     * @param placeholderName the name of the placeholder to resolve
     * @return the replacement value, or {@code null} if no replacement is to be made
     */
    String resolvePlaceholder(String placeholderName);
  }
}
