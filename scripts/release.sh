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

function setReleaseVersion() {
  RELEASE_VERSION=$(calculateReleaseVersion)
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

##!groovy
#
#pipeline {
#  agent any
#  tools {
#    maven 'apache-maven-latest'
#    jdk 'adoptopenjdk-hotspot-jdk8-latest'
#  }
#  stages {
#    stage('Build') {
#        steps {
#            withCredentials([file(credentialsId: 'secret-subkeys.asc', variable: 'KEYRING')]) {
#                sh 'gpg --batch --import "${KEYRING}"'
#                sh 'for fpr in $(gpg --list-keys --with-colons  | awk -F: \'/fpr:/ {print $10}\' | sort -u); do echo -e "5\ny\n" |  gpg --batch --command-fd 0 --expert --edit-key ${fpr} trust; done'
#            }
#            sshagent (['github-bot-ssh']) {
#                sh '''
#
#
#                    # Find Project release version
#                    PROJECT_VERSION=$(mvn -q -Dexec.executable=echo -Dexec.args='${project.version}' --non-recursive exec:exec)
#                    NEXT_RELEASE_VERSION=`echo $PROJECT_VERSION | sed 's/-SNAPSHOT//g'`
#                    if [ ${#NEXT_RELEASE_VERSION} -eq 3 ]; then
#                        NEXT_RELEASE_VERSION=`echo "$NEXT_RELEASE_VERSION.0"`
#                    fi
#
#                    echo "Releasing project with version $NEXT_RELEASE_VERSION"
#
#                    # Prepare project for release, modify pom to new release version
#                    mvn versions:set -DnewVersion=$NEXT_RELEASE_VERSION
#                    find . -iname *.versionsBackup -exec rm {} +
#                    git add . && git commit -m "[RELEASE] Modified Project pom version to $NEXT_RELEASE_VERSION"
#                    #git tag $NEXT_RELEASE_VERSION
#                    #git push origin $NEXT_RELEASE_VERSION
#                    #git push origin master
#
#                    mvn clean -B
#                    mvn -V -B -e -U install org.sonatype.plugins:nexus-staging-maven-plugin:1.6.7:deploy -P release -DnexusUrl=https://oss.sonatype.org -DserverId=ossrh
#                    # read repo_id from *.properties file and set it
#                    repo_id=$(cat target/nexus-staging/staging/*.properties | grep id | awk -F'=' '{print $2}')
#                    mvn -B org.sonatype.plugins:nexus-staging-maven-plugin:1.6.5:rc-release -DserverId=ossrh -DnexusUrl=https://oss.sonatype.org -DstagingRepositoryId=${repo_id} -Ddescription=\"Next release is ready\" -DstagingProgressTimeoutMinutes=60
#
#                    # Modify poms back to SNAPSHOT VERSIONS
#                    MAJOR_VERSION=`echo $NEXT_RELEASE_VERSION | cut -d. -f1`
#                    MINOR_VERSION=`echo $NEXT_RELEASE_VERSION | cut -d. -f2`
#                    PATCH_VERSION=`echo $NEXT_RELEASE_VERSION | cut -d. -f3`
#                    PATCH_VERSION=$(($PATCH_VERSION + 1))
#                    NEXT_SNAPSHOT_VERSION=`echo "$MAJOR_VERSION.$MINOR_VERSION.$PATCH_VERSION-SNAPSHOT"`
#                    mvn versions:set -DnewVersion=$NEXT_SNAPSHOT_VERSION
#                    find . -iname *.versionsBackup -exec rm {} +
#                    #git add . && git commit -m "[RELEASE] Prepare project for next development iteration $NEXT_SNAPSHOT_VERSION"
#                    #git push origin master
#
#                    '''
#            }
#        }
#    }
#  }
#}
