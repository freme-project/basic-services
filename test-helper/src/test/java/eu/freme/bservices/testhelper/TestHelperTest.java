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
package eu.freme.bservices.testhelper;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import eu.freme.bservices.testhelper.api.IntegrationTestSetup;
import eu.freme.common.starter.FREMEStarter;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by Arne on 07.01.2016.
 */
public class TestHelperTest {

    Logger logger = Logger.getLogger(TestHelperTest.class);
    //TestHelper testHelper = new TestHelper("test-helper-test-package.xml");
 
   	ApplicationContext context = IntegrationTestSetup.getContext("test-helper-test-package.xml");//FREMEStarter.startPackageFromClasspath("test-helper-test-package.xml");

    @Test
    public void testAuthenticatedTestHelper() throws UnirestException, IOException {
        AuthenticatedTestHelper authenticatedTestHelper=context.getBean(AuthenticatedTestHelper.class);
        authenticatedTestHelper.authenticateUsers();
        authenticatedTestHelper.removeAuthenticatedUsers();
    }


    @Test
    public void testMockupEndpoint() throws UnirestException {
    	
    	TestHelper testHelper =context.getBean(TestHelper.class);
        String filename = "ELINK.ttl";
        logger.info("request file: "+filename);
        HttpResponse<String> response = Unirest.post(testHelper.getAPIBaseUrl() + "/mockups/file/"+filename).asString();
        assertEquals(HttpStatus.OK.value(), response.getStatus());
    }
 }
