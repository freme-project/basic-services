/**
 * Copyright © 2016 Agro-Know, Deutsches Forschungszentrum für Künstliche Intelligenz, iMinds,
 * Institut für Angewandte Informatik e. V. an der Universität Leipzig,
 * Istituto Superiore Mario Boella, Tilde, Vistatec, WRIPL (http://freme-project.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.freme.bservices.filter.proxy;

import eu.freme.bservices.filter.proxy.exception.EndpointDoesNotExistException;
import eu.freme.bservices.testhelper.LoggingHelper;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import eu.freme.common.starter.FREMEStarter;

import java.net.UnknownHostException;

public class ProxyServiceTest {

	@Test
	public void testProxy() throws UnirestException {

		FREMEStarter.startPackageFromClasspath("proxy-filter-test-package.xml");

		HttpResponse<String> response = Unirest
				.post("http://localhost:8080")
				.asString();
		assertTrue(response.getStatus() == 404);
		
		response = Unirest
				.post("http://localhost:8080/e-proxy/test")
				.queryString("test", "value")
				.header("my-header", "header-value").body("body\nbody")
				.asString();
		
		assertTrue(response.getStatus() == 200);
		assertTrue(response.getBody().equals("response"));

		response = Unirest.get("http://localhost:8080/e-proxy/test2")
				.asString();
		assertTrue(response.getStatus() == 200);
		assertTrue(response.getBody().equals("response"));

		response = Unirest.get("http://localhost:8080/e-proxy/test2/")
				.asString();
		assertTrue(response.getStatus() == 200);
		assertTrue(response.getBody().equals("response"));

		LoggingHelper.loggerIgnore(UnknownHostException.class.getCanonicalName());
		response = Unirest.get("http://localhost:8080/e-proxy/test3")
				.asString();
		LoggingHelper.loggerUnignore(UnknownHostException.class.getCanonicalName());
		assertTrue(response.getStatus() == 502);
		
		LoggingHelper.loggerIgnore(EndpointDoesNotExistException.class.getCanonicalName());
		response = Unirest.get("http://localhost:8080/test-non-existing")
				.asString();
		assertTrue(response.getStatus() == 404);
		LoggingHelper.loggerUnignore(UnknownHostException.class.getCanonicalName());
	}
}
