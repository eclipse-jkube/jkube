/**
 * Copyright (c) 2019 Red Hat, Inc.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at:
 *
 *     https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.jkube.quickstart.webapp.web;

import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.jkube.quickstart.webapp.domain.Commiter;
import org.eclipse.jkube.quickstart.webapp.domain.CommiterDao;

@Named
@RequestScoped
public class CommitersController {

    @Inject
    private CommiterDao commiterDao;

    public List<Commiter> getCommiters() {
        return commiterDao.getCommiters();
    }

}
