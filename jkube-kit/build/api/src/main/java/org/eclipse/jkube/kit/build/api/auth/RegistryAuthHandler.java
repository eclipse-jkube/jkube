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
package org.eclipse.jkube.kit.build.api.auth;

import java.io.IOException;
import java.util.function.Function;

/**
 * @author roland
 * @since 21.10.18
 */
public interface RegistryAuthHandler {

    String getId();

    AuthConfig create(RegistryAuthConfig.Kind kind, String user, String registry, Function<String, String> decryptor);

    interface Extender {
        String getId();
        AuthConfig extend(AuthConfig given, String registry) throws IOException;
    }
}
















