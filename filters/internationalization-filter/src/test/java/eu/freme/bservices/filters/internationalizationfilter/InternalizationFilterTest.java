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
package eu.freme.bservices.filters.internationalizationfilter;
import java.io.File;
import java.io.IOException;

import eu.freme.bservices.testhelper.TestHelper;
import eu.freme.bservices.testhelper.ValidationHelper;
import eu.freme.bservices.testhelper.api.IntegrationTestSetup;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;

import static eu.freme.common.conversion.rdf.RDFConstants.TURTLE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.HttpRequestWithBody;

import eu.freme.common.conversion.rdf.RDFConstants;

import org.springframework.context.ApplicationContext;

/**
 * Created by Jonathan Sauder (jonathan.sauder@student.hpi.de) on 06.08.15.
 */
public class InternalizationFilterTest {

    TestHelper th;
    ValidationHelper vh;
    ClassLoader classLoader;
    Logger logger = Logger.getLogger(InternalizationFilterTest.class);

    public InternalizationFilterTest() throws  UnirestException {
        ApplicationContext context = IntegrationTestSetup.getContext("internationalization-filter-test-package.xml");// FREMEStarter.startPackageFromClasspath("ratelimiter-test-package.xml");
        th = context.getBean(TestHelper.class);
        vh = context.getBean(ValidationHelper.class);
        classLoader = getClass().getClassLoader();
    }

    String dataset = "dbpedia";
    String[] sample_xliff = {"internationalization/testdata1.xlf"};
    String[] sample_html = {"internationalization/aa324.html", "internationalization/test10.html", "internationalization/test12.html"};

    @Test
    public void TestEInternationalization() throws IOException,
            UnirestException {
        logger.info("TestEInternationalizaton with xliff");
        for (String sample_file : sample_xliff) {
            logger.info("Testing file " + sample_file);
            testContentTypeandInformat(
                    "application/x-xliff+xml",
                    FileUtils.readFileToString(new File(classLoader.getResource(sample_file).getFile())));
        }

        logger.info("TestEInternationalization with html");
        for (String sample_file : sample_html) {
            logger.info("Testing file "+sample_file);
            testContentTypeandInformat(
                    "text/html",
                    FileUtils.readFileToString(new File(classLoader.getResource(sample_file).getFile())));
        }
    }

    protected HttpRequestWithBody baseRequestPost() {
        return Unirest.post("").queryString("dataset", dataset);
    }

    private void testContentTypeandInformat(String format, String data)
            throws UnirestException, IOException {
        HttpResponse<String> response;
        // With Content-Type header
        response = Unirest.post(th.getAPIBaseUrl()+"/mockups/file/internationalization-NER-this-is-Germany.ttl")
                .header("Content-Type", format)
                //     .header("Accept","text/turtle")
                .queryString("language", "en")
                .queryString("outformat","turtle")

                .body(data).asString();
        logger.info(response.getHeaders());
        vh.validateNIFResponse(response, TURTLE);

        // With informat QueryString
        response = Unirest.post(th.getAPIBaseUrl() + "/mockups/file/internationalization-NER-this-is-Germany.ttl")
                .queryString("informat", format)
                .queryString("language", "en")
                .queryString("outformat","turtle")
                .body(data)
                .asString();
        logger.info(response.getHeaders());
        vh.validateNIFResponse(response, TURTLE);
    }

    @Test
    @Ignore
    public void testRoundTripping() throws UnirestException, IOException {

        logger.info("Testing EInternationalization Round Tripping");
        HttpResponse<String> response = Unirest
                .post(th.getAPIBaseUrl() + "/mockups/file/internationalization-NER-this-is-Germany.ttl")
                .queryString("language", "en")
                .queryString("dataset", "dbpedia")
                .queryString("informat", "text/html")
                .queryString("outformat", "text/html")
                .body("<p>Berlin is a city in Germany</p>").asString();

        assertEquals(200,response.getStatus());
        assertTrue(response.getBody().length() > 0);

        String xliff = FileUtils.readFileToString(new File(classLoader.getResource(
                "internationalization/testdata1.xlf").getFile()));
        response = Unirest
                .post(th.getAPIBaseUrl() + "/mockups/file/internationalization-NER-this-is-Germany.ttl")
                .queryString("language", "en")
                .queryString("dataset", "dbpedia")
                .queryString("informat", "text/html")
                .queryString("outformat", "text/html").body(xliff).asString();

        assertEquals(200,response.getStatus());
        assertTrue(response.getBody().length() > 0);


    }

    @Test
    public void testRoundTrippingLong() throws UnirestException, IOException {
        String longHtml = FileUtils.readFileToString(new File(classLoader.getResource("internationalization/long-html.html").getFile()));
        HttpResponse<String> response = Unirest
                .post(th.getAPIBaseUrl() + "/mockups/file/internationalization-NER-this-is-Germany.ttl")
                .queryString("language", "en")
                .queryString("dataset", "dbpedia")
                .queryString("informat", "text/html")
                .queryString("outformat", "text/html").body(longHtml).asString();

        assertEquals(200,response.getStatus());
        assertTrue(response.getBody().length() > 0);
    }

    @Test
    public void testXml() throws UnirestException {
        HttpResponse<String> response = Unirest
                .post(th.getAPIBaseUrl() + "/mockups/file/internationalization-NER-this-is-Germany.ttl")
                .queryString("language", "en")
                .queryString("dataset", "dbpedia")
                .queryString("informat", "text/xml")
                .body("<note><to>Tove</to><from>Jani</from><heading>Reminder</heading><body>Don't forget me this weekend!</body></note>")
                .asString();

        assertEquals(200, response.getStatus());
        assertTrue(response.getBody().length() > 0);
    }

    @Test
    public void testOdt() throws IOException, UnirestException{
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource("internationalization/odt-test.odt").getFile());
        byte data[] = FileUtils.readFileToByteArray(file);
        HttpResponse<String> response = Unirest
                .post(th.getAPIBaseUrl() + "/mockups/file/internationalization-NER-this-is-Germany.ttl")
                .queryString("language", "en")
                .queryString("dataset", "dbpedia")
                .queryString("informat", "application/x-openoffice")
                .body(data)
                .asString();

        assertEquals(200,response.getStatus());
        assertTrue(response.getBody().length() > 0);
    }

}

