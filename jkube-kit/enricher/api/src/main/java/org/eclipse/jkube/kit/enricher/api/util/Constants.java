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
package org.eclipse.jkube.kit.enricher.api.util;

/**
 */
// TODO-F8SPEC : Should be move the to AppCatalog mojo and must not be in the general available util package
// Also consider whether the Constants class pattern makes (should probably change to real enums ???)
public class Constants {
    public static final String RESOURCE_SOURCE_URL_ANNOTATION = "maven.jkube.io/source-url";
    public static final String RESOURCE_APP_CATALOG_ANNOTATION = "maven.jkube.io/app-catalog";

    private Constants() { }
}

