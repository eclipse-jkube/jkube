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

BASEDIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
source "$BASEDIR/common.sh"
QUICKSTARTS="$PROJECT_ROOT/quickstarts"

function mvnVersion() {
  JKUBE_VERSION=$1
  echo "Updating quickstarts pom files to JKube $JKUBE_VERSION"
  cd "$QUICKSTARTS" || exit 1
  find . -type f -name "pom.xml" -print0 | xargs -0 -L 1 -P 1 -I{} sh -c \
    "mvn -f '{}' versions:set -DnewVersion=$JKUBE_VERSION -DgenerateBackupPoms=false || exit 255"
}

function gradleVersion() {
  JKUBE_VERSION=$1
  echo "Updating quickstarts Gradle files to JKube $JKUBE_VERSION"
  cd "$QUICKSTARTS" || exit 1
  find . -type f -name "build.gradle" \
    -exec sed -i "s/id 'org.eclipse.jkube.kubernetes' version .*$/id 'org.eclipse.jkube.kubernetes' version '$JKUBE_VERSION'/g" {} \;            \
    -exec sed -i "s/id 'org.eclipse.jkube.openshift' version .*$/id 'org.eclipse.jkube.openshift' version '$JKUBE_VERSION'/g" {} \;              \
    -exec sed -i "s/id(\"org.eclipse.jkube.kubernetes\") version .*$/id(\"org.eclipse.jkube.kubernetes\") version \"${JKUBE_VERSION}\"/g" {} \;  \
    -exec sed -i "s/id(\"org.eclipse.jkube.openshift\") version .*$/id(\"org.eclipse.jkube.openshift\") version \"${JKUBE_VERSION}\"/g" {} \;    \
    -exec sed -i "s/\:org.eclipse.jkube.openshift.gradle.plugin\:.*$/\:org.eclipse.jkube.openshift.gradle.plugin\:$JKUBE_VERSION'/g" {} \;       \
    -exec sed -i "s/\:org.eclipse.jkube.kubernetes.gradle.plugin\:.*$/:org.eclipse.jkube.kubernetes.gradle.plugin\:$JKUBE_VERSION'/g" {} \;      \
    -exec sed -i "s/\:jkube-kit-api\:.*$/\:jkube-kit-api\:$JKUBE_VERSION'/g" {} \;                                                               \
    -exec sed -i "s/\:jkube-kit-enricher-api\:.*$/\:jkube-kit-enricher-api\:$JKUBE_VERSION'/g" {} \;                                             \
    -exec sed -i "s/\:jkube-kit-generator-api\:.*$/\:jkube-kit-generator-api\:$JKUBE_VERSION'/g" {} \;

}

function version() {
  JKUBE_VERSION=$(getJKubeVersion)
  mvnVersion "$JKUBE_VERSION"
  gradleVersion "$JKUBE_VERSION"
}

function packageMaven() {
  echo "Packaging all Maven quickstart projects (excluding sub-modules)"
  cd "$QUICKSTARTS" || exit 1
  find . -type f -name "pom.xml" -exec grep -q -z -v '../pom.xml</relativePath>' {} \; -print0 | \
    xargs -0 -L 1 -P 1 -I{} sh -c "mvn -f '{}' clean package || exit 255"
}

function packageGradle() {
  echo "Packaging all Gradle quickstart projects (excluding sub-modules)"
  cd "$QUICKSTARTS" || exit 1
  find . -type f -name "build.gradle" -execdir ./gradlew build \;
}

function package() {
  packageMaven
  packageGradle
}

if declare -f "$1" > /dev/null ; then
  "$@"
elif [ -z "$1" ]; then
  echo "Please specify a function name" >&2
else
  echo "'$1' is not a known function name" >&2
  exit 1
fi

