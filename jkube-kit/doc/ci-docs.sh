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

echo ============================================
echo Deploying jkube-maven-plugin documentation
echo ============================================

cd doc && \
mvn -Phtml,pdf package && \
git clone -b gh-pages https://rhuss:${GITHUB_TOKEN}@github.com/jkubeio/jkube-kit.git gh-pages && \
git config --global user.email "travis@jkube.io" && \
git config --global user.name "Travis" && \
cp -rv target/generated-docs/* gh-pages/ && \
cd gh-pages && \
mv index.pdf jkube-kit.pdf && \
git add --ignore-errors * && \
git commit -m "generated documentation" && \
git push origin gh-pages && \
cd .. && \
rm -rf gh-pages target
