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
package eu.freme.bservices.filters.cors;

import org.junit.Test;
import org.springframework.context.ConfigurableApplicationContext;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import eu.freme.bservices.testhelper.TestHelper;
import eu.freme.common.starter.FREMEStarter;
import static org.junit.Assert.assertTrue;

public class CORSFilterTest{

	@Test
	public void test() throws UnirestException{
		ConfigurableApplicationContext context = FREMEStarter.startPackageFromClasspath("cors-filter-test-package.xml");
		TestHelper testHelper = context.getBean(TestHelper.class);
		String url = testHelper.getAPIBaseUrl();
		HttpResponse<String> response = Unirest.post(url + "/mockups/file/response.txt").asString();
		String header = response.getHeaders().get("access-control-allow-origin").get(0);
		assertTrue(header.equals("*"));
	}
}
