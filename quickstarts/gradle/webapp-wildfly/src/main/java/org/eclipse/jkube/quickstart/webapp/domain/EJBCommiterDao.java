/*
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
package org.eclipse.jkube.quickstart.webapp.domain;

import java.util.List;

import javax.ejb.Stateful;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

@Stateful
public class EJBCommiterDao implements CommiterDao {

    @Inject
    private EntityManager em;

    @Override
    public List<Commiter> getCommiters() {
        TypedQuery<Commiter> query = em.createQuery("select c from Commiter c", Commiter.class);
        return query.getResultList();
    }

}
