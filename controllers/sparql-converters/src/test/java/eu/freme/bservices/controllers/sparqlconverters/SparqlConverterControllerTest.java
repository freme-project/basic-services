package eu.freme.bservices.controllers.sparqlconverters;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResourceFactory;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import eu.freme.bservices.testhelper.AuthenticatedTestHelper;
import eu.freme.bservices.testhelper.LoggingHelper;
import eu.freme.bservices.testhelper.OwnedResourceManagingHelper;
import eu.freme.bservices.testhelper.SimpleEntityRequest;
import eu.freme.bservices.testhelper.api.IntegrationTestSetup;
import eu.freme.common.conversion.rdf.JenaRDFConversionService;
import eu.freme.common.conversion.rdf.RDFConstants;
import eu.freme.common.conversion.rdf.RDFConversionService;
import eu.freme.common.persistence.model.SparqlConverter;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by Arne Binder (arne.b.binder@gmail.com) on 12.01.2016.
 */
public class SparqlConverterControllerTest {
    private Logger logger = Logger.getLogger(SparqlConverterControllerTest.class);
    private AuthenticatedTestHelper ath;
    private OwnedResourceManagingHelper<SparqlConverter> ormh;
    final static String serviceUrl = "/toolbox/convert";

    public SparqlConverterControllerTest() throws UnirestException, IOException {
        ApplicationContext context = IntegrationTestSetup.getContext("sparql-converter-controller-test-package.xml");
        ath = context.getBean(AuthenticatedTestHelper.class);
        ormh = new OwnedResourceManagingHelper<>("/toolbox/convert/manage",SparqlConverter.class, ath);
        ath.authenticateUsers();
    }

    final String entityHeader = "entity";
    final String propertyIdentifier = "http://www.w3.org/2005/11/its/rdf#taIdentRef";
    final String resourceIdentifier = "http://dbpedia.org/resource/Berlin";

    final String sparqlConverterSelect = "SELECT ?"+ entityHeader +" WHERE {[] <"+propertyIdentifier+"> ?"+ entityHeader +"}";
    final String sparqlConverterConstruct = "CONSTRUCT {?s <"+propertyIdentifier+"> ?"+ entityHeader +"} WHERE {?s <"+propertyIdentifier+"> ?"+ entityHeader +"}";


    @Test
    public void testSparqlConverterManagement() throws UnirestException, IOException {
        SimpleEntityRequest request = new SimpleEntityRequest(sparqlConverterSelect)
                .putParameter(SparqlConverterManagingController.identifierParameterName,"select-sparqlConverter");
        SimpleEntityRequest updateRequest = new SimpleEntityRequest(sparqlConverterConstruct);
                //.putParameter(SparqlConverterController.identifierParameterName,"construct-sparqlConverter");
        SparqlConverter expectedCreatedEntity = new SparqlConverter();
        expectedCreatedEntity.setName("select-sparqlConverter");
        expectedCreatedEntity.setQuery(sparqlConverterSelect);
        SparqlConverter expectedUpdatedEntity = new SparqlConverter();
        expectedUpdatedEntity.setName("select-sparqlConverter");
        expectedUpdatedEntity.setQuery(sparqlConverterConstruct);

        ormh.checkCRUDOperations(request, updateRequest, expectedCreatedEntity, expectedUpdatedEntity, "xxxx");
    }

    @Test
    public void testCreateWithAlreadyExistingName() throws UnirestException, IOException {
        SimpleEntityRequest request = new SimpleEntityRequest(sparqlConverterSelect)
                .putParameter(SparqlConverterManagingController.identifierParameterName,"select-sparqlConverter");
        SparqlConverter sparqlConverter = ormh.createEntity(request, ath.getTokenWithPermission(),HttpStatus.OK);

        LoggingHelper.loggerIgnore("eu.freme.common.exception.BadRequestException");
        ormh.createEntity(request,ath.getTokenWithPermission(),HttpStatus.BAD_REQUEST);
        LoggingHelper.loggerUnignore("eu.freme.common.exception.BadRequestException");

        ormh.deleteEntity(sparqlConverter.getIdentifier(),ath.getTokenWithPermission(), HttpStatus.OK);
    }

    @Test
    public void testConverting() throws Exception {
        HttpResponse<String> response;

        logger.info("get all sparqlConverters");
        ormh.getAllEntities(ath.getTokenWithPermission());

        logger.info("create sparqlConverter1");
        ormh.createEntity(
                new SimpleEntityRequest(sparqlConverterSelect)
                        .putParameter(SparqlConverterManagingController.identifierParameterName, "sparqlConverter1"),
                ath.getTokenWithPermission(), HttpStatus.OK);

        logger.info("create sparqlConverter2");
        ormh.createEntity(
                new SimpleEntityRequest(sparqlConverterConstruct)
                        .putParameter(SparqlConverterManagingController.identifierParameterName, "sparqlConverter2"),
                ath.getTokenWithPermission(), HttpStatus.OK);

        String nifContent =
                " @prefix nif:   <http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core#> .\n" +
                        " @prefix itsrdf: <http://www.w3.org/2005/11/its/rdf#> .\n" +
                        "\n" +
                        "<http://127.0.0.1:9995/spotlight#char=0,15>\n" +
                        " a                     nif:Context , nif:Sentence , nif:RFC5147String ;\n" +
                        " nif:beginIndex        \"0\" ;\n" +
                        " nif:endIndex          \"15\" ;\n" +
                        " nif:isString          \"This is Berlin.\" ;\n" +
                        " nif:referenceContext  <http://127.0.0.1:9995/spotlight#char=0,15> .\n" +
                        "\n" +
                        "<http://127.0.0.1:9995/spotlight#char=8,14>\n" +
                        " a                     nif:Word , nif:RFC5147String ;\n" +
                        " nif:anchorOf          \"Berlin\" ;\n" +
                        " nif:beginIndex        \"8\" ;\n" +
                        " nif:endIndex          \"14\" ;\n" +
                        " nif:referenceContext  <http://127.0.0.1:9995/spotlight#char=0,15> ;\n" +
                        " <"+propertyIdentifier+">     <"+resourceIdentifier+"> .";


        logger.info("convert nif with sparqlConverter1(select)");
        response = Unirest.post(ath.getAPIBaseUrl() + serviceUrl + "/documents/sparqlConverter1")
                .queryString("informat", RDFConstants.RDFSerialization.TURTLE.contentType())
                .queryString("outformat", RDFConstants.RDFSerialization.JSON.contentType())
                .body(nifContent)
                .asString();
        assertEquals(HttpStatus.OK.value(), response.getStatus());
        InputStream stream = new ByteArrayInputStream(response.getBody().getBytes(StandardCharsets.UTF_8));
        ResultSet resultSet = ResultSetFactory.fromJSON(stream);
        // check resultSet content
        assertTrue(resultSet.nextSolution().get(entityHeader).asResource().equals(ResourceFactory.createResource(resourceIdentifier)));
        assertFalse(resultSet.hasNext());

        logger.info("convert nif with sparqlConverter2(construct)");
        response = Unirest.post(ath.getAPIBaseUrl() + serviceUrl +"/documents/sparqlConverter2")
                .queryString("informat", RDFConstants.RDFSerialization.TURTLE.contentType())
                .queryString("outformat", RDFConstants.RDFSerialization.TURTLE.contentType())
                .body(nifContent)
                .asString();
        //check status code
        assertEquals(HttpStatus.OK.value(), response.getStatus());
        RDFConversionService rdfConversionService = new JenaRDFConversionService();
        Model resultModel = rdfConversionService.unserializeRDF(response.getBody(),RDFConstants.RDFSerialization.TURTLE);
        // check, if the result model contains the required triple
        Query askQuery = QueryFactory.create("ASK {[] <"+propertyIdentifier+"> <"+resourceIdentifier+">}");
        QueryExecution qexec = QueryExecutionFactory.create(askQuery, resultModel) ;
        assertTrue(qexec.execAsk());

        // check, if the result model contains no more triples
        Query countQuery = QueryFactory.create("SELECT (COUNT(*) as ?count) WHERE { ?s ?p ?o. }");
        qexec = QueryExecutionFactory.create(countQuery, resultModel) ;
        resultSet = qexec.execSelect();
        assertEquals(1,resultSet.nextSolution().getLiteral("count").getInt());

        logger.info("delete sparqlConverter1");
        ormh.deleteEntity("sparqlConverter1", ath.getTokenWithPermission(), HttpStatus.OK);

        logger.info("delete sparqlConverter2");
        ormh.deleteEntity("sparqlConverter2", ath.getTokenWithPermission(), HttpStatus.OK);
     }

}