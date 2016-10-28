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
package eu.freme.bservices.controllers.nifconverter;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import eu.freme.bservices.testhelper.TestHelper;
import eu.freme.bservices.testhelper.api.IntegrationTestSetup;
import eu.freme.common.conversion.SerializationFormatMapper;
import eu.freme.common.conversion.rdf.RDFConstants;
import eu.freme.common.conversion.rdf.RDFConversionService;
import eu.freme.common.rest.NIFParameterSet;
import eu.freme.common.rest.RestHelper;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;

import java.io.*;
import java.util.Scanner;

import static eu.freme.common.conversion.rdf.JenaRDFConversionService.JENA_TURTLE;
import static eu.freme.common.conversion.rdf.RDFConstants.TURTLE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by Arne on 10.03.2016.
 */
public class NifConverterControllerTest {
    private Logger logger = Logger.getLogger(NifConverterControllerTest.class);
    String url;
    RestHelper restHelper;
    RDFConversionService rdfConversionService;

    public NifConverterControllerTest() {
        ApplicationContext context = IntegrationTestSetup.getContext("nif-converter-test-package.xml");
        TestHelper th = context.getBean(TestHelper.class);
        restHelper =  context.getBean(RestHelper.class);
        rdfConversionService = context.getBean(RDFConversionService.class);
        url = th.getAPIBaseUrl() + "/toolbox/nif-converter";
    }

    public void testConversion(String informat, String outformat) throws Exception {
        logger.info("check conversion: " + informat + " -> " + outformat);

        String sourceSerialization = "Hello world!";
        // convert text to input format
        if(!informat.equals(SerializationFormatMapper.PLAINTEXT)) {
            NIFParameterSet convertParameters = new NIFParameterSet(sourceSerialization, SerializationFormatMapper.PLAINTEXT, informat, RDFConstants.fremePrefix);
            sourceSerialization = rdfConversionService.serializeRDF(restHelper.convertInputToRDFModel(convertParameters), convertParameters.getOutformatString());
        }
        NIFParameterSet parameters = new NIFParameterSet(sourceSerialization, informat, outformat, RDFConstants.fremePrefix);
        HttpResponse<String> response = restHelper.sendNifRequest(parameters, url);

        assertEquals(HttpStatus.SC_OK, response.getStatus());

        String expectedSerialization = rdfConversionService.serializeRDF(restHelper.convertInputToRDFModel(parameters), parameters.getOutformatString());
        assertEquals(expectedSerialization, response.getBody());
    }

    @Test
    public void testInternationalizationFormatsToTURTLE() throws Exception {

        testTextConversionToTURTLE("/data/source_html.html", "/data/expected_html.ttl", "text/html");
        testTextConversionToTURTLE("/data/source_xml.xml", "/data/expected_xml.ttl", "text/xml");
        testTextConversionToTURTLE("/data/source_xlf.xlf", "/data/expected_xlf.ttl", "application/x-xliff+xml");
        testByteConversionToTURTLE("/data/source_odt.odt", "/data/expected_odt.ttl", "application/x-openoffice");

        testTIKAConversionToTURTLE("/data/source_pdf.pdf", "/data/expected_pdf.ttl", "TIKAFile");
        testTIKAConversionToTURTLE("/data/source_word.docx", "/data/expected_word.ttl", "TIKAFile");
}

    public void testTextConversionToTURTLE(String sourceResource, String expectedResource, String sourceMimeType) throws Exception {

        logger.info("CONVERT "+ sourceMimeType + " to turtle");
        InputStream is = getClass().getResourceAsStream(sourceResource);
        String fileContent = new Scanner(is, "utf-8").useDelimiter("\\Z").next();

        HttpResponse<String> response =  Unirest.post(url)
                .header("Content-Type", sourceMimeType)
                .header("Accept", TURTLE)
                .body(fileContent)
                .asString();

        assertEquals(HttpStatus.SC_OK, response.getStatus());
        Model responseModel = rdfConversionService.unserializeRDF(response.getBody(), TURTLE);


        Reader expectedReader = new InputStreamReader(getClass()
                .getResourceAsStream(expectedResource), "UTF-8");
        Model expectedModel = ModelFactory.createDefaultModel();
        expectedModel.read(expectedReader, null,JENA_TURTLE);
        assertTrue(responseModel.isIsomorphicWith(expectedModel));
    }


    public void testByteConversionToTURTLE(String sourceResource, String expectedResource, String sourceMimeType) throws Exception {

        logger.info("CONVERT "+ sourceMimeType + " to turtle");
        InputStream is = getClass().getResourceAsStream(sourceResource);
        byte[] byteArr = IOUtils.toByteArray(is);

        HttpResponse<String> response =  Unirest.post(url)
                .header("Content-Type", sourceMimeType)
                .header("Accept", TURTLE)
                .body(byteArr)
                .asString();

        assertEquals(HttpStatus.SC_OK, response.getStatus());
        String cleanedBody = response.getBody().replaceAll("http://freme-project.eu/[^#]*#char", "http://freme-project.eu/test#char");
        Model responseModel = rdfConversionService.unserializeRDF(cleanedBody, TURTLE);

        is = getClass().getResourceAsStream(expectedResource);
        String fileContent = new Scanner(is, "utf-8").useDelimiter("\\Z").next();
        String cleanedfileContent = fileContent.replaceAll("http://freme-project.eu/[^#]*#char", "http://freme-project.eu/test#char");

        Model expectedModel = rdfConversionService.unserializeRDF(cleanedfileContent, TURTLE);

        assertTrue(responseModel.isIsomorphicWith(expectedModel));
    }

    public void testTIKAConversionToTURTLE(String sourceResource, String expectedResource, String sourceMimeType) throws Exception {
        logger.info("CONVERT "+ sourceMimeType + " to turtle");
//        InputStream is = getClass().getResourceAsStream(sourceResource);
//        byte[] byteArr = IOUtils.toByteArray(is);
//        
        ClassPathResource cpr = new ClassPathResource(sourceResource);
        File file = cpr.getFile();
        HttpResponse<String> response =  Unirest.post(url)
//                .header("Content-Type", sourceMimeType)
                .header("Accept", TURTLE)
				.queryString("informat", "TIKAFile")
                .field("inputFile", file)
                .asString();

        assertEquals(HttpStatus.SC_OK, response.getStatus());
        String cleanedBody = response.getBody().replaceAll("http://freme-project.eu/[^#]*#char", "http://freme-project.eu/test#char");
        Model responseModel = rdfConversionService.unserializeRDF(cleanedBody, TURTLE);

        cpr = new ClassPathResource(expectedResource);
        file = cpr.getFile();
        InputStream is = new FileInputStream(file);
        String fileContent = new Scanner(is, "utf-8").useDelimiter("\\Z").next();
        String cleanedfileContent = fileContent.replaceAll("http://freme-project.eu/[^#]*#char", "http://freme-project.eu/test#char");

//        System.out.println(cleanedBody);
        
        Model expectedModel = rdfConversionService.unserializeRDF(cleanedfileContent, TURTLE);

        assertTrue(responseModel.isIsomorphicWith(expectedModel));
    }
}
