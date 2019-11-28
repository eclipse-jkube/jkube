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

import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertNull;

[ "openshift" ].each {
  Verify.verifyResourceDescriptors(
          new File(basedir, sprintf("/target/classes/META-INF/jkube/%s.yml",it)),
          new File(basedir, sprintf("/expected/%s.yml",it)))
}

true
