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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

public class PropertiesUtil {

  private PropertiesUtil() {}

  /**
   * Returns the given properties resource on the project classpath if found or an empty properties object if not
   *
   * @param resource resource url
   * @return properties
   */
  public static Properties getPropertiesFromResource(URL resource) {
    Properties ret = new Properties();
    if (resource != null) {
      try(InputStream stream = resource.openStream()) {
        ret.load(stream);
      } catch (IOException e) {
        throw new IllegalStateException("Error while reading resource from URL " + resource, e);
      }
    }
    return ret;
  }
}
