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

// Verify k8s:resource output
["kubernetes", "kubernetes/helm-and-fragments-deployment", "kubernetes/helm-and-fragments-configmap"].each {
  Verify.verifyResourceDescriptors(
          new File(basedir, sprintf("/target/classes/META-INF/jkube/%s.yml", it)),
          new File(basedir, sprintf("/expected/resource/%s.yml", it)))
}

// Verify k8s:helm output
[
  "Chart", "values", "templates/helm-and-fragments-deployment", "templates/helm-and-fragments-configmap"
].each {
  Verify.verifyResourceDescriptors(
          new File(basedir, sprintf("/target/jkube/helm/helm-and-fragments/kubernetes/%s.yaml", it)),
          new File(basedir, sprintf("/expected/helm/%s.yaml", it)))
}

true
