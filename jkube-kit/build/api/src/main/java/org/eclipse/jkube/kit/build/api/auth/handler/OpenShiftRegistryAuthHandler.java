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
package org.eclipse.jkube.kit.build.api.auth.handler;

import java.util.Optional;
import java.util.Properties;
import java.util.function.UnaryOperator;

import org.eclipse.jkube.kit.build.api.auth.AuthConfig;
import org.eclipse.jkube.kit.build.api.auth.RegistryAuthConfig;
import org.eclipse.jkube.kit.build.api.auth.RegistryAuthHandler;
import org.eclipse.jkube.kit.common.KitLogger;

import static org.eclipse.jkube.kit.build.api.helper.KubernetesConfigAuthUtil.readKubeConfigAuth;

/**
 * @author roland
 */
public class OpenShiftRegistryAuthHandler implements RegistryAuthHandler {

    public static final String AUTH_USE_OPENSHIFT_AUTH = "useOpenShiftAuth";

    private final RegistryAuthConfig registryAuthConfig;
    private final KitLogger log;

    public OpenShiftRegistryAuthHandler(RegistryAuthConfig registryAuthConfig, KitLogger log) {
        this.registryAuthConfig = registryAuthConfig;
        this.log = log;
    }

    @Override
    public String getId() {
        return "openshift";
    }

    @Override
    public AuthConfig create(RegistryAuthConfig.Kind kind, String user, String registry, UnaryOperator<String> decryptor) {
        // Check for openshift authentication either from the plugin config or from system props
        Properties props = System.getProperties();
        String useOpenAuthMode = registryAuthConfig.extractFromProperties(props, kind, AUTH_USE_OPENSHIFT_AUTH);
        // Check for system property
        if (useOpenAuthMode != null) {
            boolean useOpenShift = Boolean.parseBoolean(useOpenAuthMode);
            if (!useOpenShift) {
                return null;
            }
            log.debug("AuthConfig: OpenShift credentials");
            return validateMandatoryOpenShiftLogin(readKubeConfigAuth());
        }

        boolean useOpenShiftAuth =
            Optional.ofNullable(registryAuthConfig.getConfigForHandler(getId(), AUTH_USE_OPENSHIFT_AUTH))
                    .map(Boolean::parseBoolean)
                    .orElse(false);
        if (useOpenShiftAuth) {
            log.debug("AuthConfig: OpenShift credentials");
            return validateMandatoryOpenShiftLogin(readKubeConfigAuth());
        }

        return null;
    }

    private AuthConfig validateMandatoryOpenShiftLogin(AuthConfig openShiftRegistryAuth) {
        if (openShiftRegistryAuth != null) {
            return openShiftRegistryAuth;
        }
        // No login found
        String kubeConfigEnv = System.getenv("KUBECONFIG");
        throw new IllegalArgumentException(
            String.format("OpenShift auth check enabled, but not active user and/or token found in %s. " +
                          "Please use 'oc login' for connecting to OpenShift.", kubeConfigEnv != null ? kubeConfigEnv : "~/.kube/config"));
    }

}
