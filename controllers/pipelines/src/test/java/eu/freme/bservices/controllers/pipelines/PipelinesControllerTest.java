package eu.freme.bservices.controllers.pipelines;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import eu.freme.bservices.testhelper.AuthenticatedTestHelper;
import eu.freme.bservices.testhelper.LoggingHelper;
import eu.freme.bservices.testhelper.OwnedResourceManagingHelper;
import eu.freme.bservices.testhelper.SimpleEntityRequest;
import eu.freme.bservices.testhelper.api.IntegrationTestSetup;
import eu.freme.common.conversion.rdf.RDFConstants;
import eu.freme.common.exception.BadRequestException;
import eu.freme.common.persistence.dao.PipelineDAO;
import eu.freme.common.persistence.dao.UserDAO;
import eu.freme.common.persistence.model.OwnedResource;
import eu.freme.common.persistence.model.Pipeline;
import eu.freme.common.persistence.model.SerializedRequest;
import eu.freme.common.persistence.model.User;
import org.apache.http.HttpStatus;
import org.apache.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.*;

import static eu.freme.common.conversion.SerializationFormatMapper.JSON;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by Arne Binder (arne.b.binder@gmail.com) on 19.01.2016.
 */
public class PipelinesControllerTest {
    final static String serviceUrl = "/pipelining";

    private Logger logger = Logger.getLogger(PipelinesControllerTest.class);
    AuthenticatedTestHelper ath;
    OwnedResourceManagingHelper<Pipeline> ormh;
    MockupRequestFactory rf;

    private PipelineDAO pipelineDAO;
    private UserDAO userDAO;

    public PipelinesControllerTest() throws UnirestException, IOException {
        ApplicationContext context = IntegrationTestSetup.getContext("pipelines-test-package.xml");
        ath = context.getBean(AuthenticatedTestHelper.class);
        ormh = new OwnedResourceManagingHelper<>("/pipelining/templates", Pipeline.class, ath);
        ath.authenticateUsers();
        rf = new MockupRequestFactory(ath.getAPIBaseUrl());
        pipelineDAO = context.getBean(PipelineDAO.class);
        userDAO = context.getBean(UserDAO.class);
    }


    @Test
    public void testPipelineRepository() throws JsonProcessingException {
        long pipelineCountBefore = pipelineDAO.count();
        long userCountBefore = userDAO.count();

        logger.info("Create user");
        User user = new User("hello", "world", User.roleUser);
        user = userDAO.save(user);

        AuthenticationManager am = new SampleAuthenticationManager();
        Authentication request = new UsernamePasswordAuthenticationToken(user, user.getPassword());
        Authentication result = am.authenticate(request);
        SecurityContextHolder.getContext().setAuthentication(result);

        assertTrue(userDAO.findAll().iterator().hasNext());



        logger.info("Create pipeline");
        SerializedRequest request1 = new SerializedRequest(SerializedRequest.HttpMethod.GET, "endpoint1", new HashMap<>(), new HashMap<>(), "body1");
        Pipeline pipeline = constructPipeline(OwnedResource.Visibility.PRIVATE, "label1", "description1", false, request1);
        pipeline.setOwner(user);
        pipeline = pipelineDAO.save(pipeline);
        assertTrue(pipelineDAO.findAll().iterator().hasNext());
        logger.info("Pipeline count: " + pipelineDAO.count());

        logger.info("create 2nd pipeline");
        SerializedRequest request2 = new SerializedRequest(SerializedRequest.HttpMethod.POST, "endpoint2", new HashMap<>(), new HashMap<>(), "body2");
        Pipeline pipeline2 = constructPipeline(OwnedResource.Visibility.PUBLIC, "label2", "description2", true, request2);
        pipeline2.setOwner(user);
        pipeline = pipelineDAO.save(pipeline2);
        assertEquals(pipelineCountBefore + 2, pipelineDAO.count());

        Pipeline pipeline1FromStore = pipelineDAO.findOneByIdentifier(pipeline.getId()+"");
        assertEquals(pipeline, pipeline1FromStore);

        Pipeline pipeline2FromStore = pipelineDAO.findOneByIdentifier(pipeline2.getId()+"");
        assertEquals(pipeline2, pipeline2FromStore);

        logger.info("Deleting first pipeline");
        pipelineDAO.delete(pipeline);
        assertEquals(pipelineCountBefore + 1, pipelineDAO.count());

        logger.info("Deleting user, should also delete second pipeline.");
        userDAO.delete(user);
        assertEquals(userCountBefore, userDAO.count());
        assertEquals(pipelineCountBefore, pipelineDAO.count());
    }

    class SampleAuthenticationManager implements AuthenticationManager {

        @Override
        public Authentication authenticate(Authentication authentication) throws AuthenticationException {
            return authentication;
        }
    }

    @Test
    public void testExecuteDefaultPipeline() throws UnirestException, IOException {
        Pipeline pipeline = createDefaultTemplate(OwnedResource.Visibility.PUBLIC);
        String id = pipeline.getIdentifier();
        String contents = "The Atomium in Brussels is the symbol of Belgium.";
        HttpResponse<String> response = sendRequest(ath.getTokenWithPermission(), HttpStatus.SC_OK, id, contents, RDFConstants.RDFSerialization.PLAINTEXT);
        ormh.deleteEntity(pipeline.getIdentifier(), ath.getTokenWithPermission(), org.springframework.http.HttpStatus.OK);
    }

    @Test
    public void testExecuteDefaultSingle() throws JsonProcessingException, UnirestException {
        SerializedRequest entityRequest = rf.createEntitySpotlight("en");
        SerializedRequest linkRequest = rf.createLink("3");    // Geo pos
        String contents = "The Atomium in Brussels is the symbol of Belgium.";
        HttpResponse<String> response = sendRequest(HttpStatus.SC_OK, contents, entityRequest, linkRequest);
    }

    @Test
    public void testCreateDefaultPipeline() throws UnirestException, IOException {
        Pipeline pipelineInfo = createDefaultTemplate(OwnedResource.Visibility.PUBLIC);
        assertFalse(pipelineInfo.isPersist());
        assertTrue(pipelineInfo.getId() > 0);
        ormh.deleteEntity(pipelineInfo.getIdentifier(), ath.getTokenWithPermission(), org.springframework.http.HttpStatus.OK);
    }

    // test pipeline privacy
    @Test
    public void testPipelining() throws IOException, UnirestException {
        Pipeline pipeline1 = createDefaultTemplate(OwnedResource.Visibility.PRIVATE);

        // use pipeline as
        String contents = "The Atomium in Brussels is the symbol of Belgium.";
        logger.info("execute private default pipeline with content=\""+contents+"\" as userWithPermission");
        sendRequest(ath.getTokenWithPermission(), HttpStatus.SC_OK, pipeline1.getIdentifier(), contents, RDFConstants.RDFSerialization.PLAINTEXT);

        logger.info("execute private pipeline as userWithoutPermission");
        LoggingHelper.loggerIgnore(LoggingHelper.accessDeniedExceptions);
        sendRequest(ath.getTokenWithoutPermission(), HttpStatus.SC_UNAUTHORIZED, pipeline1.getIdentifier(), contents, RDFConstants.RDFSerialization.PLAINTEXT);
        LoggingHelper.loggerUnignore(LoggingHelper.accessDeniedExceptions);

        // delete pipelines
        ormh.deleteEntity(pipeline1.getIdentifier(), ath.getTokenWithPermission(), org.springframework.http.HttpStatus.OK);
    }

    @Test
    public void testSomethingThatWorks() throws UnirestException, JsonProcessingException {
        String input = "With just 200,000 residents, Reykjavík ranks as one of Europe’s smallest capital cities. But when Iceland’s total population only hovers around 300,000, it makes sense that the capital is known as the “big city” and offers all the cultural perks of a much larger place.\n" +
                "\n" +
                "“From live music almost every night to cosy cafes, colourful houses and friendly cats roaming the street, Reykjavík has all the charms of a small town in a fun capital city,” said Kaelene Spence, ";
        SerializedRequest entityRequest = rf.createEntityFremeNER("en", "dbpedia");
        SerializedRequest linkRequest = rf.createLink("3");	// Geo pos
        SerializedRequest terminologyRequest = rf.createTerminology("en", "nl");

        sendRequest(HttpStatus.SC_OK, input, entityRequest, linkRequest, terminologyRequest);
    }

    //// test pipeline with link

    @Test
    public void testSpotlight() throws UnirestException, JsonProcessingException {
        String data = "This summer there is the Zomerbar in Antwerp, one of the most beautiful cities in Belgium.";
        SerializedRequest entityRequest = rf.createEntitySpotlight("en");
        SerializedRequest linkRequest = rf.createLink("3");	// Geo pos

        sendRequest(HttpStatus.SC_OK, data, entityRequest, linkRequest);
    }

    /**
     * e-Entity using FREME NER with database viaf and e-Link using template 3 (Geo pos). All should go well.
     * @throws UnirestException
     */
    @Test
    public void testFremeNER() throws UnirestException, JsonProcessingException {
        String data = "This summer there is the Zomerbar in Antwerp, one of the most beautiful cities in Belgium.";
        SerializedRequest entityRequest = rf.createEntityFremeNER("en", "viaf");
        SerializedRequest linkRequest = rf.createLink("3");	// Geo pos

        sendRequest(HttpStatus.SC_OK, data, entityRequest, linkRequest);
    }

    /**
     * e-Entity using an unexisting data set to test error reporting.
     */
    @Test
    @Ignore // doesnt work with mockup endpoint
    public void testWrongDatasetEntity() throws UnirestException, JsonProcessingException {
        String data = "This summer there is the Zomerbar in Antwerp, one of the most beautiful cities in Belgium.";
        SerializedRequest entityRequest = rf.createEntityFremeNER("en", "anunexistingdatabase");
        SerializedRequest linkRequest = rf.createLink("3");	// Geo pos

        sendRequest(HttpStatus.SC_BAD_REQUEST, data, entityRequest, linkRequest);
    }

    /**
     * e-Entity using an unexisting language set to test error reporting.
     */
    @Test
    @Ignore // doesnt work with mockup endpoint
    public void testWrongLanguageEntity() throws UnirestException, JsonProcessingException {
        String data = "This summer there is the Zomerbar in Antwerp, one of the most beautiful cities in Belgium.";
        SerializedRequest entityRequest = rf.createEntityFremeNER("zz", "viaf");
        SerializedRequest linkRequest = rf.createLink("3");	// Geo pos

        sendRequest(HttpStatus.SC_BAD_REQUEST, data, entityRequest, linkRequest);
    }


    //// pipeline management

    @Test
    public void testPipelineManagement() throws UnirestException, IOException {
        ClassLoader classLoader = getClass().getClassLoader();


        Pipeline pipeline1 = constructPipeline(OwnedResource.Visibility.PUBLIC, "entity, translate", "First e-Entity, then e-Translate", false, rf.createEntitySpotlight("en"), rf.createTerminology("en","en"));
        Pipeline pipeline2 = constructPipeline(OwnedResource.Visibility.PUBLIC, "Spotlight-Link", "Recognises entities using Spotlight en enriches with geo information.", false, rf.createEntitySpotlight("en"), rf.createLink("3"));

        ormh.checkCRUDOperations(
                new SimpleEntityRequest(pipeline1.toJson()),
                new SimpleEntityRequest(pipeline2.toJson()),
                pipeline1,
                pipeline2,
                "99999");

    }

    @Test
    public void testUnParametrization(){
        logger.info("test un-parametrization");
        SerializedRequest request = new SerializedRequest(SerializedRequest.HttpMethod.POST, "testendpoint", null, null, "This is a test body.");
        HashMap<String, Object> replacements = new HashMap<>();
        logger.info("no replacement necessary: should work");
        request.unParametrize(replacements);

        logger.info("add parametrized parameter: should not work without replacement. Exception is ok.");
        //LoggingHelper.loggerIgnore("eu.freme.common.exception.BadRequestException");
        request.addParameter("parama", "$replacea$");
        try {
            request.unParametrize(replacements);
        }catch(BadRequestException e){
            logger.info(e.getMessage());
        }
        //LoggingHelper.loggerUnignore(BadRequestException.class.getName());

        logger.info("add replacement: should work now");
        replacements.put("replacea", "valuea");
        request.unParametrize(replacements);
        assertEquals("valuea", request.getParameters().get("parama"));

        logger.info("add parametrized header: should not work without replacement. Exception is ok.");
        //LoggingHelper.loggerIgnore("eu.freme.common.exception.BadRequestException");
        request.addHeader("headera", "$replaceb$");
        try {
            request.unParametrize(replacements);
        }catch(BadRequestException e){
            logger.info(e.getMessage());
        }
        //LoggingHelper.loggerUnignore(BadRequestException.class.getName());

        logger.info("add replacement: should work now");
        replacements.put("replaceb", "valueb");
        request.unParametrize(replacements);
        assertEquals("valueb", request.getHeaders().get("headera"));


        logger.info("set parametrized endpoint: should not work without replacement. Exception is ok.");
        //LoggingHelper.loggerIgnore("eu.freme.common.exception.BadRequestException");
        request.setEndpoint("xyz$endpointa$xyz");
        try {
            request.unParametrize(replacements);
        }catch(BadRequestException e){
            logger.info(e.getMessage());
        }
        //LoggingHelper.loggerUnignore(BadRequestException.class.getName());

        logger.info("add replacement: should work now");
        replacements.put("endpointa", "valuec");
        request.unParametrize(replacements);
        assertEquals("xyzvaluecxyz", request.getEndpoint());
    }

    @Test
    public void testParametrizedRequest() throws JsonProcessingException, UnirestException {
        String content = "This summer there is the Zomerbar in Antwerp, one of the most beautiful cities in Belgium.";
        SerializedRequest entityRequest = rf.createEntitySpotlight("$language$");
        SerializedRequest linkRequest = rf.createLink("$templateid$");	// Geo pos

        List<SerializedRequest> serializedRequests = Arrays.asList(entityRequest, linkRequest);
        serializedRequests.get(0).setBody(content);
        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        String body = ow.writeValueAsString(serializedRequests);

        HttpResponse<String> response;

        logger.info("call parametrized pipeline without replacements. Should fail, exception is ok.");
        LoggingHelper.loggerIgnore(BadRequestException.class.getName());
        //try {
            response = Unirest.post(ath.getAPIBaseUrl() + serviceUrl + "/chain")
                    .header("content-type", JSON)
                    .body(body)
                    .asString();
        //}catch(BadRequestException e){
        //    logger.info(e.getMessage());
        //}
        LoggingHelper.loggerUnignore(BadRequestException.class.getName());

        logger.info("call parametrized pipeline with replacements. Should work.");
        response = Unirest.post(ath.getAPIBaseUrl() + serviceUrl + "/chain")
                .queryString("language", "en")
                .queryString("templateid", "3")
                .header("content-type", JSON)
                .body(body)
                .asString();

    }


    //////////////////////////////////////////////////////////////////////////////////
    //   PipelinesCommon                                                            //
    //////////////////////////////////////////////////////////////////////////////////

    /**
     * Sends the actual pipeline request. It serializes the request objects to JSON and puts this into the body of
     * the request.
     * @param expectedResponseCode	The expected HTTP response code. Will be checked against.
     * @param requests		The serialized requests to send.
     * @return				The result of the request. This can either be the result of the pipelined requests, or an
     *                      error response with some explanation what went wrong in the body.
     * @throws UnirestException
     */
    protected HttpResponse<String> sendRequest(int expectedResponseCode, String content, final SerializedRequest... requests) throws UnirestException, JsonProcessingException {
        List<SerializedRequest> serializedRequests = Arrays.asList(requests);
        serializedRequests.get(0).setBody(content);
        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        String body = ow.writeValueAsString(requests);

        HttpResponse<String> response =   Unirest.post(ath.getAPIBaseUrl() + serviceUrl + "/chain")
                .header("content-type", RDFConstants.RDFSerialization.JSON.contentType())
                .body(body)
                .asString();

        RDFConstants.RDFSerialization responseContentType = RDFConstants.RDFSerialization.fromValue(response.getHeaders().getFirst("content-type"));
        RDFConstants.RDFSerialization accept = getContentTypeOfLastResponse(serializedRequests);
        assertEquals(expectedResponseCode, response.getStatus());
        if (expectedResponseCode / 100 != 2) {
            assertEquals(RDFConstants.RDFSerialization.JSON, responseContentType);
        } else {
            assertEquals(responseContentType, accept);
        }

        return response;
    }

    protected HttpResponse<String> sendRequest(final String token, int expectedResponseCode, String identifier, final String contents, final RDFConstants.RDFSerialization contentType) throws UnirestException {
        HttpResponse<String> response = ath.addAuthentication(Unirest.post(ath.getAPIBaseUrl() + serviceUrl + "/chain/"+identifier), token)
                .header("content-type", contentType.contentType())
                .body(contents)
                .asString();

        assertEquals(expectedResponseCode, response.getStatus());
        return response;
    }

    /**
     * Helper method that returns the content type of the response of the last request (or: the value of the 'accept'
     * header of the last request).
     * @param serializedRequests	The requests that (will) serve as input for the pipelining service.
     * @return						The content type of the response that the service will return.
     */
    protected static RDFConstants.RDFSerialization getContentTypeOfLastResponse(final List<SerializedRequest> serializedRequests) {
        String contentType = "";
        if (!serializedRequests.isEmpty()) {
            SerializedRequest lastRequest = serializedRequests.get(serializedRequests.size() - 1);
            Map<String, String> headers = lastRequest.getHeaders();
            if (headers.containsKey("accept")) {
                contentType = headers.get("accept");
            } else {
                Map<String, Object> parameters = lastRequest.getParameters();
                if (parameters.containsKey("outformat")) {
                    contentType = parameters.get("outformat").toString();
                }
            }
        }
        RDFConstants.RDFSerialization serialization = RDFConstants.RDFSerialization.fromValue(contentType);
        return serialization != null ? serialization : RDFConstants.RDFSerialization.TURTLE;
    }

    protected Pipeline createDefaultTemplate(final OwnedResource.Visibility visibility) throws UnirestException, IOException {
        SerializedRequest entityRequest = rf.createEntitySpotlight("en");
        SerializedRequest linkRequest = rf.createLink("3");    // Geo pos
        return createTemplate(visibility, "a label", "a description", entityRequest, linkRequest);
    }

    protected Pipeline createTemplate(final OwnedResource.Visibility visibility, final String label, final String description, final SerializedRequest... requests) throws UnirestException, IOException {
        Pipeline pipeline = constructPipeline(visibility,label,description,false, requests);
        // send json
        pipeline = ormh.createEntity(new SimpleEntityRequest(pipeline.toJson()), ath.getTokenWithPermission(), org.springframework.http.HttpStatus.OK);
        return pipeline;
    }

    protected Pipeline constructPipeline(final OwnedResource.Visibility visibility, final String label, final String description, final boolean persist, final SerializedRequest... requests) throws JsonProcessingException {
        List<SerializedRequest> serializedRequests = Arrays.asList(requests);

        // create local Entity to build json
        Pipeline pipeline = new Pipeline();
        pipeline.setVisibility(visibility);
        pipeline.setLabel(label);
        pipeline.setDescription(description);
        pipeline.setSerializedRequests(serializedRequests);
        pipeline.setPersist(persist);
        return pipeline;
    }
}
