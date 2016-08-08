package eu.freme.bservices.controllers.nifconverter;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import eu.freme.bservices.internationalization.okapi.nif.filter.RDFConstants;
import eu.freme.bservices.testhelper.TestHelper;
import eu.freme.bservices.testhelper.api.IntegrationTestSetup;
import eu.freme.common.conversion.rdf.RDFConstants.RDFSerialization;
import eu.freme.common.rest.NIFParameterSet;
import eu.freme.common.rest.RestHelper;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by Arne on 10.03.2016.
 */
public class NifConverterControllerTest {
    private Logger logger = Logger.getLogger(NifConverterControllerTest.class);
    String url;
    RestHelper restHelper;

    public NifConverterControllerTest() {
        ApplicationContext context = IntegrationTestSetup.getContext("nif-converter-test-package.xml");
        TestHelper th = context.getBean(TestHelper.class);
        restHelper =  context.getBean(RestHelper.class);
        url = th.getAPIBaseUrl() + "/toolbox/nif-converter";
    }

    public void testConversion(RDFSerialization informat, RDFSerialization outformat) throws Exception {
        logger.info("check conversion: " + informat.name() + " -> " + outformat.name());

        String sourceSerialization = "Hello world!";
        // convert text to input format
        if(informat != RDFSerialization.PLAINTEXT) {
            NIFParameterSet convertParameters = new NIFParameterSet(sourceSerialization, RDFSerialization.PLAINTEXT, informat, restHelper.getDefaultPrefix());
            sourceSerialization = restHelper.serializeNif(restHelper.convertInputToRDFModel(convertParameters), convertParameters.getOutformat());
        }
        NIFParameterSet parameters = new NIFParameterSet(sourceSerialization, informat, outformat, restHelper.getDefaultPrefix());
        HttpResponse<String> response = restHelper.sendNifRequest(parameters, url);

        assertEquals(HttpStatus.SC_OK, response.getStatus());

        String expectedSerialization = restHelper.serializeNif(restHelper.convertInputToRDFModel(parameters), parameters.getOutformat());
        assertEquals(expectedSerialization, response.getBody());
    }

//    @Test
//    public void testRDFandPLaintextToNIF() throws Exception {
//        List<RDFSerialization> informats = new ArrayList<>();
//        List<RDFSerialization> outformats = new ArrayList<>();
//        outformats.add(RDFSerialization.TURTLE);
//        outformats.add(RDFSerialization.JSON_LD);
//        outformats.add(RDFSerialization.N3);
//        outformats.add(RDFSerialization.N_TRIPLES);
//        outformats.add(RDFSerialization.RDF_XML);
//
//        informats.addAll(outformats);
//        informats.add(RDFSerialization.PLAINTEXT);
//
//        for(RDFSerialization informat: informats){
//            for(RDFSerialization outformat: outformats){
//                testConversion(informat, outformat);
//            }
//        }
//    }

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
                .header("Accept", RDFSerialization.TURTLE.contentType())
                .body(fileContent)
                .asString();

        assertEquals(HttpStatus.SC_OK, response.getStatus());
        Model responseModel = restHelper.unserializeNif(response.getBody(), RDFSerialization.TURTLE);


        Reader expectedReader = new InputStreamReader(getClass()
                .getResourceAsStream(expectedResource), "UTF-8");
        Model expectedModel = ModelFactory.createDefaultModel();
        expectedModel.read(expectedReader, null,
                RDFConstants.RDFSerialization.TURTLE.toRDFLang());
        assertTrue(responseModel.isIsomorphicWith(expectedModel));
    }


    public void testByteConversionToTURTLE(String sourceResource, String expectedResource, String sourceMimeType) throws Exception {

        logger.info("CONVERT "+ sourceMimeType + " to turtle");
        InputStream is = getClass().getResourceAsStream(sourceResource);
        byte[] byteArr = IOUtils.toByteArray(is);

        HttpResponse<String> response =  Unirest.post(url)
                .header("Content-Type", sourceMimeType)
                .header("Accept", RDFSerialization.TURTLE.contentType())
                .body(byteArr)
                .asString();

        assertEquals(HttpStatus.SC_OK, response.getStatus());
        String cleanedBody = response.getBody().replaceAll("http://freme-project.eu/[^#]*#char", "http://freme-project.eu/test#char");
        Model responseModel = restHelper.unserializeNif(cleanedBody, RDFSerialization.TURTLE);

        is = getClass().getResourceAsStream(expectedResource);
        String fileContent = new Scanner(is, "utf-8").useDelimiter("\\Z").next();
        String cleanedfileContent = fileContent.replaceAll("http://freme-project.eu/[^#]*#char", "http://freme-project.eu/test#char");

        Model expectedModel = restHelper.unserializeNif(cleanedfileContent, RDFSerialization.TURTLE);

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
                .header("Accept", RDFSerialization.TURTLE.contentType())
				.queryString("informat", "TIKAFile")
                .field("inputFile", file)
                .asString();

        assertEquals(HttpStatus.SC_OK, response.getStatus());
        String cleanedBody = response.getBody().replaceAll("http://freme-project.eu/[^#]*#char", "http://freme-project.eu/test#char");
        Model responseModel = restHelper.unserializeNif(cleanedBody, RDFSerialization.TURTLE);

        cpr = new ClassPathResource(expectedResource);
        file = cpr.getFile();
        InputStream is = new FileInputStream(file);
        String fileContent = new Scanner(is, "utf-8").useDelimiter("\\Z").next();
        String cleanedfileContent = fileContent.replaceAll("http://freme-project.eu/[^#]*#char", "http://freme-project.eu/test#char");

//        System.out.println(cleanedBody);
        
        Model expectedModel = restHelper.unserializeNif(cleanedfileContent, RDFSerialization.TURTLE);

        assertTrue(responseModel.isIsomorphicWith(expectedModel));
    }
}
