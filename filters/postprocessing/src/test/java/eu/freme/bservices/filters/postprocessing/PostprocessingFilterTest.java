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
package eu.freme.bservices.filters.postprocessing;

import com.hp.hpl.jena.query.ResultSetFactory;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.sparql.resultset.ResultsFormat;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import eu.freme.bservices.controllers.sparqlconverters.SparqlConverterController;
import eu.freme.bservices.controllers.sparqlconverters.SparqlConverterManagingController;
import eu.freme.bservices.testhelper.AuthenticatedTestHelper;
import eu.freme.bservices.testhelper.LoggingHelper;
import eu.freme.bservices.testhelper.OwnedResourceManagingHelper;
import eu.freme.bservices.testhelper.SimpleEntityRequest;
import eu.freme.bservices.testhelper.api.IntegrationTestSetup;
import eu.freme.common.conversion.SerializationFormatMapper;
import eu.freme.common.conversion.rdf.RDFConstants;
import com.hp.hpl.jena.query.ResultSet;
import eu.freme.common.exception.AccessDeniedException;
import eu.freme.common.exception.BadRequestException;
import eu.freme.common.exception.OwnedResourceNotFoundException;
import eu.freme.common.persistence.dao.SparqlConverterDAO;
import eu.freme.common.persistence.model.OwnedResource;
import eu.freme.common.persistence.model.SparqlConverter;
import eu.freme.common.rest.OwnedResourceManagingController;
import org.apache.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by Arne Binder (arne.b.binder@gmail.com) on 19.01.2016.
 */
@Ignore
public class PostprocessingFilterTest {

    AuthenticatedTestHelper ath;
    Logger logger = Logger.getLogger(PostprocessingFilterTest.class);
    private OwnedResourceManagingHelper<SparqlConverter> ormh;
    private final String idName;
    final static String serviceUrl = "/toolbox/convert";
    final static String managingURL = "/toolbox/convert/manage";

    final static String filterSelect = "PREFIX itsrdf: <http://www.w3.org/2005/11/its/rdf#> SELECT ?entity WHERE {?charsequence itsrdf:taIdentRef ?entity}";
    final static String filterName = "extract-entities-only";
    final static String csvResponse = "entity\n" +
            "http://dbpedia.org/resource/Champ_de_Mars\n" +
            "http://dbpedia.org/resource/Eiffel_Tower\n" +
            "http://dbpedia.org/resource/France\n" +
            "http://dbpedia.org/resource/Paris\n" +
            "http://dbpedia.org/resource/Eiffel_(programming_language)";

    public PostprocessingFilterTest() throws UnirestException, IOException {
        ApplicationContext context = IntegrationTestSetup.getContext("postprocessing-filter-test-package.xml");
        ath = context.getBean(AuthenticatedTestHelper.class);
        ath.authenticateUsers();
        ormh = new OwnedResourceManagingHelper<>(managingURL,SparqlConverter.class, ath);
        SparqlConverterDAO sparqlConverterDAO = new SparqlConverterDAO();
        idName = sparqlConverterDAO.getIdentifierName();
    }

    @Test
    public void testPostprocessing() throws UnirestException, IOException {
        HttpResponse<String> response;

        logger.info("create filter "+filterName);
        ormh.createEntity(new SimpleEntityRequest(filterSelect).putParameter(idName, filterName),
                AuthenticatedTestHelper.getTokenWithPermission(),
                HttpStatus.OK);

        String filename = "postprocessing-data.ttl";
        logger.info("result as csv");
        response = Unirest.post(ath.getAPIBaseUrl() + "/mockups/file/"+filename+"?filter="+filterName).header("Accept", SparqlConverterController.CSV).asString();
        assertEquals(HttpStatus.OK.value(), response.getStatus());
        // clean line endings and check content
        assertEquals(csvResponse.trim(), response.getBody().trim().replaceAll("\r",""));

        logger.info("result as xml");
        response = Unirest.post(ath.getAPIBaseUrl() + "/mockups/file/"+filename+"?filter="+filterName).header("Accept", SparqlConverterController.XML).asString();
        assertEquals(HttpStatus.OK.value(), response.getStatus());
        ResultSet responseRS = ResultSetFactory.load(new ByteArrayInputStream(response.getBody().trim().getBytes(StandardCharsets.UTF_8)), ResultsFormat.FMT_RS_XML);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ResultSetFormatter.outputAsCSV(outputStream, responseRS);
        String result = new String(outputStream.toByteArray());
        assertEquals(csvResponse.trim(), result.trim().replaceAll("\r",""));

        logger.info("result as json");
        response = Unirest.post(ath.getAPIBaseUrl() + "/mockups/file/"+filename+"?filter="+filterName).header("Accept", SerializationFormatMapper.JSON).asString();
        assertEquals(HttpStatus.OK.value(), response.getStatus());
        responseRS = ResultSetFactory.load(new ByteArrayInputStream(response.getBody().trim().getBytes(StandardCharsets.UTF_8)), ResultsFormat.FMT_RS_JSON);
        outputStream = new ByteArrayOutputStream();
        ResultSetFormatter.outputAsCSV(outputStream, responseRS);
        result = new String(outputStream.toByteArray());
        // clean line endings and check content
        assertEquals(csvResponse.trim(), result.trim().replaceAll("\r",""));

        logger.info("result as turtle");
        response = Unirest.post(ath.getAPIBaseUrl() + "/mockups/file/"+filename+"?filter="+filterName).header("Accept", RDFConstants.TURTLE).asString();
        assertEquals(HttpStatus.OK.value(), response.getStatus());
        responseRS = ResultSetFactory.load(new ByteArrayInputStream(response.getBody().trim().getBytes(StandardCharsets.UTF_8)), ResultsFormat.FMT_RDF_TURTLE);
        outputStream = new ByteArrayOutputStream();
        ResultSetFormatter.outputAsCSV(outputStream, responseRS);
        result = new String(outputStream.toByteArray());
        // clean line endings and check content
        assertEquals(csvResponse.trim(), result.trim().replaceAll("\r",""));

        logger.info("result as ld-json");
        response = Unirest.post(ath.getAPIBaseUrl() + "/mockups/file/"+filename+"?filter="+filterName).header("Accept", RDFConstants.JSON_LD).asString();
        assertEquals(HttpStatus.OK.value(), response.getStatus());
        //// TODO: add content check for ld-json
        //assertEquals(ldjsonResponse.trim(), response.getBody().trim().replaceAll("\r",""));

        logger.info("result as rdf-xml");
        response = Unirest.post(ath.getAPIBaseUrl() + "/mockups/file/"+filename+"?filter="+filterName).header("Accept", RDFConstants.RDF_XML).asString();
        assertEquals(HttpStatus.OK.value(), response.getStatus());
        responseRS = ResultSetFactory.load(new ByteArrayInputStream(response.getBody().trim().getBytes(StandardCharsets.UTF_8)), ResultsFormat.FMT_RDF_XML);
        outputStream = new ByteArrayOutputStream();
        ResultSetFormatter.outputAsCSV(outputStream, responseRS);
        result = new String(outputStream.toByteArray());
        // clean line endings and check content
        assertEquals(csvResponse.trim(), result.trim().replaceAll("\r",""));

        logger.info("result as n3");
        response = Unirest.post(ath.getAPIBaseUrl() + "/mockups/file/"+filename+"?filter="+filterName).header("Accept", RDFConstants.N3).asString();
        assertEquals(HttpStatus.OK.value(), response.getStatus());
        responseRS = ResultSetFactory.load(new ByteArrayInputStream(response.getBody().trim().getBytes(StandardCharsets.UTF_8)), ResultsFormat.FMT_RDF_N3);
        outputStream = new ByteArrayOutputStream();
        ResultSetFormatter.outputAsCSV(outputStream, responseRS);
        result = new String(outputStream.toByteArray());
        // clean line endings and check content
        assertEquals(csvResponse.trim(), result.trim().replaceAll("\r",""));

        logger.info("result as n-triples");
        response = Unirest.post(ath.getAPIBaseUrl() + "/mockups/file/"+filename+"?filter="+filterName).header("Accept", RDFConstants.N_TRIPLES).asString();
        assertEquals(HttpStatus.OK.value(), response.getStatus());
        responseRS = ResultSetFactory.load(new ByteArrayInputStream(response.getBody().trim().getBytes(StandardCharsets.UTF_8)), ResultsFormat.FMT_RDF_NT);
        outputStream = new ByteArrayOutputStream();
        ResultSetFormatter.outputAsCSV(outputStream, responseRS);
        result = new String(outputStream.toByteArray());
        // clean line endings and check content
        assertEquals(csvResponse.trim(), result.trim().replaceAll("\r",""));

        logger.info("delete filter extract-entities-only");
        ormh.deleteEntity(filterName, AuthenticatedTestHelper.getTokenWithPermission(), HttpStatus.OK);
    }


    @Test
    public void testPostprocessingWithNotExisting() throws UnirestException, IOException {
        HttpResponse<String> response;

        String filename = "postprocessing-data.ttl";
        logger.info("request file: "+filename);
        LoggingHelper.loggerIgnore(OwnedResourceNotFoundException.class.getCanonicalName());
        response = Unirest.post(ath.getAPIBaseUrl() + "/mockups/file/"+filename+"?filter="+filterName+"&outformat=csv").asString();
        LoggingHelper.loggerUnignore(OwnedResourceNotFoundException.class.getCanonicalName());
        assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatus());
    }

    @Test
    public void testPostprocessingWithPrivateFilter() throws UnirestException, IOException {
        HttpResponse<String> response;

        logger.info("create filter "+filterName);
        ormh.createEntity(new SimpleEntityRequest(filterSelect)
                .putParameter(idName, filterName)
                .putParameter(OwnedResourceManagingController.visibilityParameterName, OwnedResource.Visibility.PRIVATE.toString()),
                AuthenticatedTestHelper.getTokenWithPermission(),
                HttpStatus.OK);

        String filename = "postprocessing-data.ttl";

        logger.info("request file and try to postprocess as anonymous user: "+filename +". Should fail.");
        LoggingHelper.loggerIgnore(AccessDeniedException.class.getCanonicalName());
        response = Unirest.post(ath.getAPIBaseUrl() + "/mockups/file/"+filename+"?filter="+filterName+"&outformat=csv")
                .asString();
        LoggingHelper.loggerUnignore(AccessDeniedException.class.getCanonicalName());
        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatus());

        logger.info("request file and try to postprocess as userWithoutPermission: "+filename +". Should fail.");
        LoggingHelper.loggerIgnore(AccessDeniedException.class.getCanonicalName());
        response = ath.addAuthenticationWithoutPermission(Unirest.post(ath.getAPIBaseUrl() + "/mockups/file/"+filename+"?filter="+filterName+"&outformat=csv"))
                .asString();
        LoggingHelper.loggerUnignore(AccessDeniedException.class.getCanonicalName());
        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatus());

        logger.info("request file and try to postprocess as owner: "+filename);
        response = ath.addAuthentication(Unirest.post(ath.getAPIBaseUrl() + "/mockups/file/"+filename+"?filter="+filterName+"&outformat=csv"))
                        .asString();
        assertEquals(HttpStatus.OK.value(), response.getStatus());
        // clean line endings and check content
        assertEquals(csvResponse.trim(), response.getBody().trim().replaceAll("\r",""));

        logger.info("request file and try to postprocess as admin: "+filename);
        response = ath.addAuthenticationWithAdmin(Unirest.post(ath.getAPIBaseUrl() + "/mockups/file/"+filename+"?filter="+filterName+"&outformat=csv"))
                .asString();
        assertEquals(HttpStatus.OK.value(), response.getStatus());
        // clean line endings and check content
        assertEquals(csvResponse.trim(), response.getBody().trim().replaceAll("\r",""));

        logger.info("delete filter extract-entities-only");
        ormh.deleteEntity(filterName, AuthenticatedTestHelper.getTokenWithPermission(), HttpStatus.OK);
    }

    @Test
    public void testPostprocessingWithWrongOutformat() throws UnirestException, IOException {
        HttpResponse<String> response;

        logger.info("create filter "+filterName);
        ormh.createEntity(new SimpleEntityRequest(filterSelect).putParameter(idName, filterName),
                AuthenticatedTestHelper.getTokenWithPermission(),
                HttpStatus.OK);

        String filename = "postprocessing-data.ttl";
        logger.info("try to filter with outformat: blub. Should fail.");
        LoggingHelper.loggerIgnore(BadRequestException.class.getCanonicalName());
        response = Unirest.post(ath.getAPIBaseUrl() + "/mockups/file/"+filename+"?filter="+filterName+"&outformat=blub").asString();
        LoggingHelper.loggerUnignore(BadRequestException.class.getCanonicalName());
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());

        logger.info("try to filter with accept header: blub. Should fail.");
        LoggingHelper.loggerIgnore(BadRequestException.class.getCanonicalName());
        response = Unirest.post(ath.getAPIBaseUrl() + "/mockups/file/"+filename+"?filter="+filterName)
                .header("Accept", "blub")
                .asString();
        LoggingHelper.loggerUnignore(BadRequestException.class.getCanonicalName());
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());

        logger.info("delete filter extract-entities-only");
        ormh.deleteEntity(filterName, AuthenticatedTestHelper.getTokenWithPermission(), HttpStatus.OK);
    }
}
