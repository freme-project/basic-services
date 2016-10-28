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
package eu.freme.bservices.testhelper.api;

import java.util.HashMap;

import org.springframework.context.ConfigurableApplicationContext;

import eu.freme.common.starter.FREMEStarter;

/**
 * Setup integration tests across multiple test classes. Each class can call
 * IntegrationTestSetup.getContext(). The first starts the application context. All
 * integration tests can get the application context through
 * IntegrationTestSetup.
 * 
 * The class can register multiple application contexts. It stores all
 * application contexts in a hashmap with the path to the package as key.
 * 
 * @author Jan Nehring - jan.nehring@dfki.de
 */
public class IntegrationTestSetup {

	private static HashMap<String, ConfigurableApplicationContext> applicationContexts;

	public static ConfigurableApplicationContext getContext(String packagePath) {

		if (applicationContexts == null) {
			applicationContexts = new HashMap<String, ConfigurableApplicationContext>();
		}

		if (applicationContexts.containsKey(packagePath)) {
			return applicationContexts.get(packagePath);
		} else {
			ConfigurableApplicationContext applicationContext = FREMEStarter
					.startPackageFromClasspath(packagePath);
			applicationContexts.put(packagePath, applicationContext);
			return applicationContext;
		}
	}
}
