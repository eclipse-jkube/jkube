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
package org.eclipse.jkube.helidon.generator;


import org.eclipse.jkube.generator.api.FromSelector;

@FunctionalInterface
public interface HelidonFromSelector {

  FromSelector fromSelector(HelidonGenerator helidonGenerator);

  HelidonFromSelector NATIVE = helidonGenerator -> new FromSelector.Default(helidonGenerator.getContext(), "helidon-native");
  HelidonFromSelector STANDARD = helidonGenerator -> new FromSelector.Default(helidonGenerator.getContext(), "java");
}
