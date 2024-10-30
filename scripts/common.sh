#!/bin/bash
#
# Copyright (c) 2019 Red Hat, Inc.
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at:
#
#     https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#
# Contributors:
#   Red Hat, Inc. - initial API and implementation
#

PROJECT_ROOT="$BASEDIR/.."

function getJKubeVersion() {
  echo $(mvn -f "$PROJECT_ROOT/pom.xml" -q -Dexec.executable=echo -Dexec.args=\${project.version} --non-recursive exec:exec)
}
