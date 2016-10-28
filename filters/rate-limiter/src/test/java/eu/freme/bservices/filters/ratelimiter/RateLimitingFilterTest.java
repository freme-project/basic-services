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
package eu.freme.bservices.filters.ratelimiter;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import eu.freme.bservices.testhelper.*;
import eu.freme.bservices.testhelper.api.IntegrationTestSetup;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * Created by Jonathan Sauder (jonathan.sauder@student.hpi.de) on 25.11.15.
 */

public class RateLimitingFilterTest {

    AuthenticatedTestHelper ath;

    public RateLimitingFilterTest() throws  UnirestException {
    	ApplicationContext context = IntegrationTestSetup.getContext("ratelimiter-test-package.xml");// FREMEStarter.startPackageFromClasspath("ratelimiter-test-package.xml");
        ath = context.getBean(AuthenticatedTestHelper.class);
    }


    HttpResponse<String> response;

    Logger logger = Logger.getLogger(RateLimitingFilterTest.class);


    String testusername="ratelimitertestuser";
    String testpassword="ratelimiterpassword";

    @Test
    public void testRatelimiting() throws UnirestException, IOException {
        logger.info("starting ratelimiter test");

        logger.info("creating User for ratelimiter test");
        ath.createUser(testusername, testpassword);
        logger.info("authenticating this user");
        String ratelimiterToken = ath.authenticateUser(testusername, testpassword);
        logger.info("trying /e-link/templates call as ratelimitertestuser - should work the first time");

        response = ath.addAuthentication(Unirest.get(ath.getAPIBaseUrl()+"/mockups/file/ratelimiting_requests"), ratelimiterToken).asString();
        logger.info(response.getBody());
        assertNotEquals(HttpStatus.TOO_MANY_REQUESTS.value(),response.getStatus());
        logger.info("trying /e-entity/freme-ner/datasets call as ratelimitertestuser - should not work the second time");
        LoggingHelper.loggerIgnore("TooManyRequestsException");
        response = ath.addAuthentication(Unirest.get(ath.getAPIBaseUrl()+"/mockups/file/ratelimiting_requests"), ratelimiterToken).asString();
        assertEquals(HttpStatus.TOO_MANY_REQUESTS.value(),response.getStatus());
        LoggingHelper.loggerUnignore("TooManyRequestsException");

        logger.info("trying /e-translate/tilde test with huge size as ratelimitertestuser - should not work");
        LoggingHelper.loggerIgnore("TooManyRequestsException");

        //String content = testHelper.getResourceContent("mockups/file/ratelimiting");
        response = ath.addAuthentication(Unirest.post(ath.getAPIBaseUrl()+"/mockups/file/ratelimiting_size"), ratelimiterToken)
                .queryString("informat", "text")
                .queryString("outformat","turtle")
                .queryString("source-lang","en")
                .queryString("target-lang","de")
                .body("Some input longer than 1 character")
                .asString();
        assertEquals(HttpStatus.TOO_MANY_REQUESTS.value(),response.getStatus());
        LoggingHelper.loggerUnignore("TooManyRequestsException");

        logger.info("trying anoter call for which there is no rate-limiting, should work");
        response = Unirest.get(ath.getAPIBaseUrl()+"/mockups/file/ratelimiting_default").asString();
        assertNotEquals(HttpStatus.TOO_MANY_REQUESTS.value(), response.getStatus());
    }
}