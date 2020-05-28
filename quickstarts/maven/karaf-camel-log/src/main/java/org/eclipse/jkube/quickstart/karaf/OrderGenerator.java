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
package org.eclipse.jkube.quickstart.karaf;

import org.apache.camel.CamelContext;

import java.io.InputStream;
import java.util.Random;

public class OrderGenerator {
  private int count = 1;
  private Random random = new Random();

  public InputStream generateOrder(CamelContext camelContext) {
    final int number = random.nextInt(5) + 1;
    final String orderSource = String.format("data/order%s.xml", number);
    return camelContext.getClassResolver().loadResourceAsStream(orderSource);
  }

  public String generateFileName() {
    return String.format("order%s.xml", count++);
  }
}
