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
package io.k8spatterns.demo.random;

import java.util.Random;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RandomGeneratorService {

    private static UUID id = UUID.randomUUID();
    private static Random random = new Random();

    public int getRandom() {
        return random.nextInt();
    }

    public UUID getUUID() {
        return id;
    }
}
