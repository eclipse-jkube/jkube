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
package org.eclipse.jkube.maven.sample.javaee.webprofile.service;

import static javax.ws.rs.core.MediaType.TEXT_HTML;

import java.text.MessageFormat;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Path("/")
@ApplicationScoped
public class MeetingBookingService {
	
	private static final String BANNER = "Microservice Meeting Room Booking API Application";
	
	@GET
	@Path("/")
	@Produces(TEXT_HTML)
	public String info() {
		return BANNER;
	}
}
