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

REPOSITORY="https://repo.eclipse.org/content/repositories/dash-licenses/org/eclipse/dash/org.eclipse.dash.licenses"
TARGET_DIR="$PROJECT_ROOT/target"
DEPENDENCY_LIST="$TARGET_DIR/dependencies.txt"
TOOL_JAR="$TARGET_DIR/eclipse-dash.jar"

function downloadTool() {
  if [[ ! -f "$TOOL_JAR" ]]
  then
    if [[ -z "$ECLIPSE_DASH_VERSION" ]]
    then
      echo "Getting latest Eclipse Dash 0.0.1-SNAPSHOT version"
      version=$(curl -s "${REPOSITORY}/0.0.1-SNAPSHOT/maven-metadata.xml" | xpath -q -e "/metadata/versioning/snapshotVersions/snapshotVersion[extension='jar']/value/text()")
      downloadUrl="${REPOSITORY}/0.0.1-SNAPSHOT/org.eclipse.dash.licenses-${version}.jar"
    else
      echo "Using provided Eclipse Dash version ($ECLIPSE_DASH_VERSION)"
      downloadUrl="${REPOSITORY}/${ECLIPSE_DASH_VERSION}/org.eclipse.dash.licenses-${ECLIPSE_DASH_VERSION}.jar"
    fi
    echo "Downloading eclipse-dash.jar"
    mkdir -p "$TARGET_DIR"
    curl "$downloadUrl" -o "$TOOL_JAR"
  fi
}

function generateDependencyList() {
  echo "Generating dependency list"
  rm "$PROJECT_ROOT/target/dependencies.txt"
  # https://gitlab.eclipse.org/eclipsefdn/emo-team/iplab/-/issues/9839#note_1198470
  mvn -f "$PROJECT_ROOT/pom.xml" dependency:list                                                \
      -DskipTests -Dmaven.javadoc.skip=true -DappendOutput=true -DoutputFile="$DEPENDENCY_LIST" \
      -DexcludeGroupIds=org.gradle
}

function runTool() {
  java -jar "$TOOL_JAR" "$DEPENDENCY_LIST" -summary "$PROJECT_ROOT/target/dependencies-resolved.csv" "$@"
}

downloadTool
generateDependencyList
runTool "$@"
