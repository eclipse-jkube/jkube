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

source "$BASEDIR/common.sh"

function calculateReleaseVersion() {
  PROJECT_VERSION=$(getJKubeVersion)
  echo $PROJECT_VERSION | sed 's/-SNAPSHOT//g'
}

function calculateNextSnapshotVersion() {
  if [ -z "$1" ]; then
    echo "Error calculating next snapshot version, missing current version"
  fi
  CURRENT_VERSION=$1
  MAJOR_VERSION=$(echo $CURRENT_VERSION | cut -d. -f1)
  MINOR_VERSION=$(echo $CURRENT_VERSION | cut -d. -f2)
  PATCH_VERSION=$(echo $CURRENT_VERSION | cut -d. -f3)
  NEW_PATCH_VERSION=$(($PATCH_VERSION + 1))
  echo "$MAJOR_VERSION.$MINOR_VERSION.$NEW_PATCH_VERSION-SNAPSHOT"

}

function setAutoReleaseVersion() {
  setSpecificReleaseVersion $(calculateReleaseVersion)
}

function setReleaseVersion() {
  RELEASE_VERSION=$1
  echo "Setting release version for project to $RELEASE_VERSION"
  mvn -f "$PROJECT_ROOT/pom.xml" versions:set -DnewVersion="$RELEASE_VERSION" -DgenerateBackupPoms=false
}

if declare -f "$1" > /dev/null ; then
  "$@"
elif [ -z "$1" ]; then
  echo "Please specify a function name" >&2
else
  echo "'$1' is not a known function name" >&2
  exit 1
fi

