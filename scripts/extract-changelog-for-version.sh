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

START_LINK=10
BASEDIR=$(dirname "$BASH_SOURCE")

function checkInput() {
  if [ "$#" -lt 1 ]; then
    echo -e "This script extracts chagnelog version contents from CHANGELOG.md"
    echo -e "Usage: ./extract-changelog-for-version.sh semVer [startLinkNumber]\n"
    echo -e "Must set a valid semantic version number (e.g. 1.3.37)"
    exit 1;
  fi
  dotCount=$(echo "$1" | tr -d -c '.' | wc -c)
  if [ "$dotCount" -ne 2 ]; then
      echo "Provided version has an invalid format, should be semver compliant (e.g. 1.3.37)"
      exit 1;
  fi
}

function extractChangelogPortion() {
  sed -e "/### ""$1""/,/###/!d" "$BASEDIR/../CHANGELOG.md"
}

function removeLastLine() {
  echo "$1" | sed '$d'
}

function replaceBullets() {
  echo -e "$1" | sed -e "s/^*/-/"
}

function addLinks() {
  lines=""
  links=""
  currentLink="$START_LINK"
  if [ -n "$2" ]; then currentLink="$2" ; fi
  while read -r line; do
    issueNumber=$(echo "$line" | sed -En 's/.*?#([0-9]+).*/\1/p')
    if [ -z "$issueNumber" ]; then
      lines+="$line\n";
    else
      lines+="$line [$currentLink]\n"
      links+="[$currentLink]: https://github.com/eclipse/jkube/issues/$issueNumber\n"
      currentLink=$((currentLink + 1));
    fi
  done < <(echo "$1")
  echo -e "$lines\n$links";
}

function processChangelog() {
  changelog=$1
  changelog=$(extractChangelogPortion "$changelog")
  changelog=$(removeLastLine "$changelog")
  changelog=$(replaceBullets "$changelog")
  changelog=$(addLinks "$changelog" "$2")
  echo "$changelog";
}

checkInput "$@"
processChangelog "$@"
