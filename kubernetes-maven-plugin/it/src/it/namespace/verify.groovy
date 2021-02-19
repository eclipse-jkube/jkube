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
import org.eclipse.jkube.maven.it.Verify

[
  "default", "create-namespace", "set-namespace", "create-and-set-namespace", "create-and-set-different-namespace"
].each {
  Verify.verifyResourceDescriptors(
    new File(basedir, sprintf("/%s/classes/META-INF/jkube/kubernetes.yml", it)),
    new File(basedir, sprintf("/expected/%s/kubernetes.yml", it)))
}

true
