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
package eu.freme.bservices.controllers.xsltconverter;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import eu.freme.bservices.testhelper.AuthenticatedTestHelper;
import eu.freme.bservices.testhelper.OwnedResourceManagingHelper;
import eu.freme.bservices.testhelper.SimpleEntityRequest;
import eu.freme.bservices.testhelper.api.IntegrationTestSetup;
import eu.freme.common.persistence.dao.XsltConverterDAO;
import eu.freme.common.persistence.model.XsltConverter;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

import static org.junit.Assert.assertEquals;

/**
 * Created by Arne Binder (arne.b.binder@gmail.com) on 19.07.2016.
 */
public class XsltConverterTest {
    final static String manageServiceUrl = "/toolbox/xslt-converter/manage";
    final static String transformServiceUrl = "/toolbox/xslt-converter/documents";

    private Logger logger = Logger.getLogger(XsltConverterTest.class);
    private AuthenticatedTestHelper ath;
    private OwnedResourceManagingHelper<XsltConverter> ormh;

    private XsltConverterDAO xsltConverterDAO;

    public XsltConverterTest() throws UnirestException, IOException {
        ApplicationContext context = IntegrationTestSetup.getContext("xslt-converter-test-package.xml");
        ath = context.getBean(AuthenticatedTestHelper.class);
        ormh = new OwnedResourceManagingHelper<>(manageServiceUrl, XsltConverter.class, ath);
        ath.authenticateUsers();
        xsltConverterDAO = context.getBean(XsltConverterDAO.class);
    }

    @Test
    public void testXsltConverterManagement() throws UnirestException, IOException {
        logger.info("check xslt converter management...");
        String createdXsltName = "identity-transformation";
        String xsltIdentityTransform = loadResource("/data/identity-transform.xsl");
        String xsltWithParam = loadResource("/data/xslt-with-param.xsl");
        SimpleEntityRequest request = new SimpleEntityRequest(xsltIdentityTransform)
                .putParameter(xsltConverterDAO.getIdentifierName(), createdXsltName);
        SimpleEntityRequest updateRequest = new SimpleEntityRequest(xsltWithParam);
        XsltConverter expectedCreatedEntity = new XsltConverter();
        expectedCreatedEntity.setName(createdXsltName);
        expectedCreatedEntity.setStylesheet(xsltIdentityTransform);
        XsltConverter expectedUpdatedEntity = new XsltConverter();
        expectedUpdatedEntity.setName(createdXsltName);
        expectedUpdatedEntity.setStylesheet(xsltWithParam);

        ormh.checkCRUDOperations(request, updateRequest, expectedCreatedEntity, expectedUpdatedEntity, "xxxx");
    }

    @Test
    public void testTransformationTeiToHtml() throws IOException, UnirestException {
        logger.info("test transform tei to html...");
        String tei2tempHtml = loadResource("/data/tei2temp-html.xsl");

        HttpResponse<String> response;
        String createdXsltName = "tei2temp-html";
        SimpleEntityRequest request = new SimpleEntityRequest(tei2tempHtml)
                .putParameter(xsltConverterDAO.getIdentifierName(), createdXsltName);

        ormh.createEntity(request, ath.getTokenWithPermission(), HttpStatus.OK);



        String teiSample = loadResource("/data/source_tei-sample.xml");
        response = Unirest.post(ath.getAPIBaseUrl() + transformServiceUrl +"/"+createdXsltName)
                .header("Accept", "text/html")
                .body(teiSample)
                .asString();

        // check default result content-type: has to be text/xml
        assertEquals(HttpStatus.OK.value(), response.getStatus());
        String resultContentType = response.getHeaders().getFirst("Content-Type");
        Assert.assertNotEquals(null, resultContentType);
        assertEquals("text/html", resultContentType.split(";")[0]);

        // check content
        // merge all whitespace
        String expectedContent = loadResource("/data/expected_tei-sample.html").replaceAll("\\s+", " ");
        assertEquals(expectedContent, response.getBody().replaceAll("\\s+", " "));

        ormh.deleteEntity(createdXsltName, ath.getTokenWithPermission(), HttpStatus.OK);
    }

    @Test
    public void testTransformationHtmlToXml() throws IOException, UnirestException {
        logger.info("transform html to xml");
        String identityTransform = loadResource("/data/identity-transform.xsl");

        HttpResponse<String> response;
        String createdXsltName = "identity-transformation";
        SimpleEntityRequest request = new SimpleEntityRequest(identityTransform)
                .putParameter(xsltConverterDAO.getIdentifierName(), createdXsltName);

        ormh.createEntity(request, ath.getTokenWithPermission(), HttpStatus.OK);

        String teiSample = loadResource("/data/source_simple.html");
        response = Unirest.post(ath.getAPIBaseUrl() + transformServiceUrl +"/"+createdXsltName)
                .header("Content-Type", "text/html")
                .body(teiSample)
                .asString();

        // check default result content-type: has to be text/xml
        assertEquals(HttpStatus.OK.value(), response.getStatus());
        String resultContentType = response.getHeaders().getFirst("Content-Type");
        Assert.assertNotEquals(null, resultContentType);
        assertEquals("text/xml", resultContentType.split(";")[0]);

        // check content
        // merge all whitespace
        String expectedContent = loadResource("/data/expected_simple.xml").replaceAll("\\s+", " ");
        assertEquals(expectedContent, response.getBody().replaceAll("\\s+", " "));

        ormh.deleteEntity(createdXsltName, ath.getTokenWithPermission(), HttpStatus.OK);
    }

    @Test
    public void testTransformationWithParameter() throws IOException, UnirestException {
        HttpResponse<String> response;
        String createdXsltName = "xslt-with-param";
        String xsltWithParam = loadResource("/data/xslt-with-param.xsl");
        SimpleEntityRequest request = new SimpleEntityRequest(xsltWithParam)
                .putParameter(xsltConverterDAO.getIdentifierName(), createdXsltName);

        ormh.createEntity(request, ath.getTokenWithPermission(), HttpStatus.OK);

        logger.info("transform without parameter");
        String teiSample = loadResource("/data/source_tei-sample.xml");
        response = Unirest.post(ath.getAPIBaseUrl() + transformServiceUrl +"/"+createdXsltName)
                .body(teiSample)
                .asString();

        // check default result content-type: has to be text/xml
        assertEquals(HttpStatus.OK.value(), response.getStatus());
        String resultContentType = response.getHeaders().getFirst("Content-Type");
        Assert.assertNotEquals(null, resultContentType);
        assertEquals("text/xml", resultContentType.split(";")[0]);

        //check content: has to contain "parametercontent"
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>set internally", response.getBody());

        logger.info("transform with parameter");
        response = Unirest.post(ath.getAPIBaseUrl() + transformServiceUrl +"/"+createdXsltName)
                .queryString("myparam","parametercontent")
                .body(teiSample)
                .asString();

        // check default result content-type: has to be text/xml
        assertEquals(HttpStatus.OK.value(), response.getStatus());
        resultContentType = response.getHeaders().getFirst("Content-Type");
        Assert.assertNotEquals(null, resultContentType);
        assertEquals("text/xml", resultContentType.split(";")[0]);

        //check content: has to contain "parametercontent"
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>parametercontent", response.getBody());

        ormh.deleteEntity(createdXsltName, ath.getTokenWithPermission(), HttpStatus.OK);
    }

    private String loadResource(String location) throws FileNotFoundException {
        try {
            InputStream is = getClass().getResourceAsStream(location);
            return new Scanner(is, "utf-8").useDelimiter("\\Z").next();
        }catch(Exception e){
            throw new FileNotFoundException("Can not read from resource: "+ location + " ("+ e.getMessage()+")");
        }
    }
}
