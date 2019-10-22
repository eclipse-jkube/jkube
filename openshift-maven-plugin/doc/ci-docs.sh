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

set -e

echo ============================================
echo Deploying openshift-maven-plugin documentation
echo ============================================

export MAVEN_OPTS="-Xmx3000m"

mvn -B install -DskipTests=true
cd doc
mvn -B -Phtml,pdf package
git clone -b gh-pages https://fabric8cd:$GH_TOKEN@github.com/jkubeio/openshift-maven-plugin gh-pages
cp -rv target/generated-docs/* gh-pages/
cd gh-pages
mv index.pdf openshift-maven-plugin.pdf
git add --ignore-errors *
git commit -m "generated documentation"
git push origin gh-pages
cd ..
rm -rf gh-pages target
