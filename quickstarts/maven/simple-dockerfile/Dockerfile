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

FROM openjdk:latest
COPY maven/target/zero-config-dockerfile.jar /tmp/zero-config-dockerfile.jar
# Copying a file inside project root directory
COPY maven/static-dir-in-project-root/my-file.txt /tmp/my-file.txt
CMD ["java", "-jar", "/tmp/zero-config-dockerfile.jar"]
