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
package org.eclipse.jkube.kit.build.service.docker.helper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class DeepCopy {

    private DeepCopy() { }

    /**
     * Returns a copy of the object, or null if the object cannot
     * be serialized.
     *
     * @param orig object provided
     * @param <T> type of object
     * @return returns copy of the object or null
     */
    public static <T> T copy(T orig) {
        if (orig == null) {
            return null;
        }
        try {
            // Write the object out to a byte array
            ByteArrayOutputStream fbos = new ByteArrayOutputStream();

            try (ObjectOutputStream out = new ObjectOutputStream(fbos)) {
                out.writeObject(orig);
                out.flush();
            }

            // Retrieve an input stream from the byte array and read
            // a copy of the object back in.
            try (ByteArrayInputStream fbis = new ByteArrayInputStream(fbos.toByteArray());
                 ObjectInputStream in = new ObjectInputStream(fbis))  {
                return (T) in.readObject();
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException("Cannot copy " + orig, e);
        }
    }
}
