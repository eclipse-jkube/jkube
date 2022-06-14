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

REPOSITORY="https://repo.eclipse.org/content/repositories/dash-licenses/org/eclipse/dash/org.eclipse.dash.licenses/0.0.1-SNAPSHOT"
DEPENDENCY_LIST="$PROJECT_ROOT/target/dependencies.txt"
TOOL_JAR="$PROJECT_ROOT/target/eclipse-dash.jar"

function downloadTool() {
  if [[ ! -f "$TOOL_JAR" ]]
  then
    echo "Downloading eclipse-dash.jar"
    version=$(curl -s "$REPOSITORY/maven-metadata.xml" | xpath -q -e "/metadata/versioning/snapshotVersions/snapshotVersion[extension='jar']/value/text()")
    curl "$REPOSITORY/org.eclipse.dash.licenses-${version}.jar" -o "$TOOL_JAR"
  fi
}

function generateDependencyList() {
  echo "Generating dependency list"
  rm "$PROJECT_ROOT/target/dependencies.txt"
  mvn -f "$PROJECT_ROOT/pom.xml" dependency:list -DskipTests -Dmaven.javadoc.skip=true -DappendOutput=true -DoutputFile="$DEPENDENCY_LIST"
}

function runTool() {
  java -jar "$TOOL_JAR" "$DEPENDENCY_LIST" -summary "$PROJECT_ROOT/target/dependencies-resolved.csv" "$@"
}

downloadTool
generateDependencyList
runTool "$@"
