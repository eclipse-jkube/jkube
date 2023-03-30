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
package org.eclipse.jkube.itests;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.startsWith;

import org.arquillian.cube.openshift.impl.enricher.AwaitRoute;
import org.arquillian.cube.openshift.impl.enricher.RouteURL;
import org.arquillian.cube.openshift.impl.requirement.RequiresOpenshift;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

@RequiresOpenshift
@RunWith(ArquillianConditionalRunner.class)
public class SampleIT {

	private static final String SAMPLE_APP = "openliberty";

	@RouteURL(SAMPLE_APP)
	@AwaitRoute(timeout = 30, timeoutUnit = TimeUnit.SECONDS, path = "/hello")
	private String sampleURL;

	@Test
	public void testSample() {
		given().baseUri(sampleURL)
				.when().get("/hello")
				.then().statusCode(200).contentType("text/plain").body(startsWith("Hello"));
	}

}