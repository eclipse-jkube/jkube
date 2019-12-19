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

import java.util.UUID;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class RandomResponse {

    private UUID id;
    private int random;

    RandomResponse(UUID id, int random) {
        this.id = id;
        this.random = random;
    }

    public String getId() {
        return id.toString();
    }

    public int getRandom() {
        return random;
    }
}
