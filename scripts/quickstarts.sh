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

trap 'exit' ERR

BASEDIR=$(dirname "$BASH_SOURCE")
PROJECT_ROOT="$BASEDIR/.."
QUICKSTARTS="$PROJECT_ROOT/quickstarts"

function getJKubeVersion() {
  echo $(mvn -f "$PROJECT_ROOT/pom.xml" -q -Dexec.executable=echo -Dexec.args=\${project.version} --non-recursive exec:exec)
}

function version() {
  JKUBE_VERSION=$(getJKubeVersion)
  echo "Updating quickstarts pom files to JKube $JKUBE_VERSION"
  cd "$QUICKSTARTS" || exit 1
  find . -type f -name "pom.xml" -print0 | xargs -0 -L 1 -P 1 -I{} sh -c \
    "mvn -f '{}' versions:set -DnewVersion=$JKUBE_VERSION -DgenerateBackupPoms=false || exit 255"
}

function package() {
  echo "Packaging all quickstart projects (exluding sub-modules)"
  cd "$QUICKSTARTS" || exit 1
  find . -type f -name "pom.xml" -exec grep -q -z -v '../pom.xml</relativePath>' {} \; -print0 | \
    xargs -0 -L 1 -P 1 -I{} sh -c "mvn -f '{}' clean package || exit 255"
}

if declare -f "$1" > /dev/null ; then
  "$@"
elif [ -z "$1" ]; then
  echo "Please specify a function name" >&2
else
  echo "'$1' is not a known function name" >&2
  exit 1
fi