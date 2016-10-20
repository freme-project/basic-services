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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import eu.freme.common.conversion.SerializationFormatMapper;
import eu.freme.common.conversion.rdf.RDFConstants;
import eu.freme.common.persistence.model.OwnedResource;
import eu.freme.common.rest.OwnedResourceManagingController;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by Arne Binder (arne.b.binder@gmail.com) on 20.01.2016.
 */
public class OwnedResourceManagingHelper<T extends OwnedResource> {
    Logger logger = Logger.getLogger(OwnedResourceManagingHelper.class);

    String service;
    Class clazz;

    public final String creationTimeIdentifier = "creationTime";
    public final String idIdentifier = "id";

    AuthenticatedTestHelper ath;

    public OwnedResourceManagingHelper(String service, Class clazz, AuthenticatedTestHelper ath){
        this.service = service;
        this.clazz = clazz;
        this.ath = ath;
    }

    public void checkCRUDOperations(SimpleEntityRequest request, SimpleEntityRequest updateRequest, T expectedCreatedEntity, T expectedUpdatedEntity, String notExistingIdentifier) throws IOException, UnirestException {
        logger.info("start check CRUD operations");
        checkCREATE(request, expectedCreatedEntity);
        checkGETonPUBLIC(request, expectedCreatedEntity, notExistingIdentifier);
        checkGETonPRIVATE(request, expectedCreatedEntity, notExistingIdentifier);
        checkGETALLonPUBLIC(request, expectedCreatedEntity);
        checkGETALLonPRIVATE(request, expectedCreatedEntity);
        checkUPDATE(request, updateRequest, expectedCreatedEntity, expectedUpdatedEntity, notExistingIdentifier);
        checkDELETE(request, expectedCreatedEntity, notExistingIdentifier);
        logger.info("finished check CRUD operations");
    }

    private void checkCREATE(SimpleEntityRequest request, T expectedCreatedEntity) throws IOException, UnirestException {
        T entity;
        int countAsAdmin;
        int currentCount;

        logger.info("check CREATE entity");

        // count existing entities
        countAsAdmin = getAllEntities(AuthenticatedTestHelper.getTokenAdmin()).size();

        //// check CREATE entities
        // as anonymous user
        logger.info("attempting creating entity as anonymous user: should return UNAUTHORIZED");
        LoggingHelper.loggerIgnore(LoggingHelper.accessDeniedExceptions);
        entity = createEntity(request, null, HttpStatus.UNAUTHORIZED);
        LoggingHelper.loggerUnignore(LoggingHelper.accessDeniedExceptions);

        // as userWithPermission
        logger.info("create entity with request body, parameters and headers");
        entity = createEntity(request, AuthenticatedTestHelper.getTokenWithPermission(),HttpStatus.OK);
        assertTrue(containsEntity(entity, expectedCreatedEntity));

        //// delete created
        logger.info("delete created entity");
        deleteEntity(entity.getIdentifier(), AuthenticatedTestHelper.getTokenWithPermission(), HttpStatus.OK);

        // check entity count
        currentCount = getAllEntities(AuthenticatedTestHelper.getTokenAdmin()).size();
        assertEquals(countAsAdmin, currentCount);
    }

    private void checkGETonPUBLIC(SimpleEntityRequest request, T expectedCreatedEntity, String notExistingIdentifier) throws IOException, UnirestException {
        T entity;
        String identifier;
        int countAsAdmin;
        int currentCount;
        SimpleEntityRequest localRequest = new SimpleEntityRequest(request);
        OwnedResource.Visibility visibilityBefore = expectedCreatedEntity.getVisibility();

        logger.info("check GET on PUBLIC entity");

        // count existing entites
        countAsAdmin = getAllEntities(AuthenticatedTestHelper.getTokenAdmin()).size();

        // assert visibility=PUBLIC
        expectedCreatedEntity.setVisibility(OwnedResource.Visibility.PUBLIC);
        localRequest.putParameter(OwnedResourceManagingController.visibilityParameterName, OwnedResource.Visibility.PUBLIC.name());

        //// create entity
        // as userWithPermission: use this in further requests
        logger.info("create entity with request body, parameters and headers");
        entity = createEntity(localRequest, AuthenticatedTestHelper.getTokenWithPermission(),HttpStatus.OK);
        assertTrue(containsEntity(entity, expectedCreatedEntity));
        identifier = entity.getIdentifier();

        //// check GET entity
        // as anonymous user
        logger.info("fetch entity as anonymous user");
        entity = getEntity(identifier, null, HttpStatus.OK);
        assertTrue(containsEntity(entity, expectedCreatedEntity));

        // as userWithoutPermission
        logger.info("fetch entity as userWithoutPermission");
        entity = getEntity(identifier, AuthenticatedTestHelper.getTokenWithoutPermission(), HttpStatus.OK);
        assertTrue(containsEntity(entity, expectedCreatedEntity));

        // as userWithPermission
        logger.info("fetch entity as userWithPermission");
        entity = getEntity(identifier, AuthenticatedTestHelper.getTokenWithPermission(), HttpStatus.OK);
        assertTrue(containsEntity(entity, expectedCreatedEntity));

        // as admin
        logger.info("fetch entity as admin");
        entity = getEntity(identifier, AuthenticatedTestHelper.getTokenAdmin(), HttpStatus.OK);
        assertTrue(containsEntity(entity, expectedCreatedEntity));

        //// delete created
        logger.info("delete created entity");
        deleteEntity(identifier, AuthenticatedTestHelper.getTokenWithPermission(), HttpStatus.OK);

        // try to fetch not existing entity
        logger.info("fetch not existing entity: should return NOT_FOUND");
        LoggingHelper.loggerIgnore(LoggingHelper.ownedResourceNotFoundException);
        getEntity(notExistingIdentifier, AuthenticatedTestHelper.getTokenAdmin(), HttpStatus.NOT_FOUND);
        LoggingHelper.loggerUnignore(LoggingHelper.ownedResourceNotFoundException);

        // check entity count
        currentCount = getAllEntities(AuthenticatedTestHelper.getTokenAdmin()).size();
        assertEquals(countAsAdmin, currentCount);

        expectedCreatedEntity.setVisibility(visibilityBefore);
    }

    private void checkGETonPRIVATE(SimpleEntityRequest request, T expectedCreatedEntity, String notExistingIdentifier) throws IOException, UnirestException {
        T entity;
        String identifier;
        int countAsAdmin;
        int currentCount;
        SimpleEntityRequest localRequest = new SimpleEntityRequest(request);
        OwnedResource.Visibility visibilityBefore = expectedCreatedEntity.getVisibility();

        logger.info("check GET on PRIVATE entity");

        // count existing entites
        countAsAdmin = getAllEntities(AuthenticatedTestHelper.getTokenAdmin()).size();

        // assert visibility=PRIVATE
        expectedCreatedEntity.setVisibility(OwnedResource.Visibility.PRIVATE);
        localRequest.putParameter(OwnedResourceManagingController.visibilityParameterName, OwnedResource.Visibility.PRIVATE.name());

        //// create entity
        // as userWithPermission: use this in further requests
        logger.info("create entity with request body, parameters and headers");
        entity = createEntity(localRequest, AuthenticatedTestHelper.getTokenWithPermission(),HttpStatus.OK);
        assertTrue(containsEntity(entity, expectedCreatedEntity));
        identifier = entity.getIdentifier();

        //// check GET entity
        // as anonymous user
        logger.info("fetch entity as anonymous user: should return UNAUTHORIZED");
        LoggingHelper.loggerIgnore(LoggingHelper.accessDeniedExceptions);
        entity = getEntity(identifier, null, HttpStatus.UNAUTHORIZED);
        LoggingHelper.loggerUnignore(LoggingHelper.accessDeniedExceptions);

        // as userWithoutPermission
        logger.info("fetch entity as userWithoutPermission: should return UNAUTHORIZED");
        LoggingHelper.loggerIgnore(LoggingHelper.accessDeniedExceptions);
        entity = getEntity(identifier, AuthenticatedTestHelper.getTokenWithoutPermission(), HttpStatus.UNAUTHORIZED);
        LoggingHelper.loggerUnignore(LoggingHelper.accessDeniedExceptions);

        // as userWithPermission
        logger.info("fetch entity as userWithPermission");
        entity = getEntity(identifier, AuthenticatedTestHelper.getTokenWithPermission(), HttpStatus.OK);
        assertTrue(containsEntity(entity, expectedCreatedEntity));

        // as admin
        logger.info("fetch entity as admin");
        entity = getEntity(identifier, AuthenticatedTestHelper.getTokenAdmin(), HttpStatus.OK);
        assertTrue(containsEntity(entity, expectedCreatedEntity));

        //// delete created
        logger.info("delete created entity");
        deleteEntity(identifier, AuthenticatedTestHelper.getTokenWithPermission(), HttpStatus.OK);

        // try to fetch not existing entity
        logger.info("fetch not existing entity: should return NOT_FOUND");
        LoggingHelper.loggerIgnore(LoggingHelper.ownedResourceNotFoundException);
        getEntity(notExistingIdentifier, AuthenticatedTestHelper.getTokenAdmin(), HttpStatus.NOT_FOUND);
        LoggingHelper.loggerUnignore(LoggingHelper.ownedResourceNotFoundException);

        // check entity count
        currentCount = getAllEntities(AuthenticatedTestHelper.getTokenAdmin()).size();
        assertEquals(countAsAdmin, currentCount);

        expectedCreatedEntity.setVisibility(visibilityBefore);
    }


    private void checkGETALLonPUBLIC(SimpleEntityRequest request, T expectedCreatedEntity) throws IOException, UnirestException {
        List<T> entities;
        T entity;
        String identifier;
        int countAsAdmin;
        int currentCount;
        SimpleEntityRequest localRequest = new SimpleEntityRequest(request);
        OwnedResource.Visibility visibilityBefore = expectedCreatedEntity.getVisibility();

        logger.info("check GET ALL on PUBLIC entity");

        // count existing entities
        countAsAdmin = getAllEntities(AuthenticatedTestHelper.getTokenAdmin()).size();

        // assert visibility=PUBLIC
        expectedCreatedEntity.setVisibility(OwnedResource.Visibility.PUBLIC);
        localRequest.putParameter(OwnedResourceManagingController.visibilityParameterName, OwnedResource.Visibility.PUBLIC.name());

        //// create entity
        // as userWithPermission: use this in further requests
        logger.info("create PUBLIC entity with request body, parameters and headers");
        entity = createEntity(localRequest, AuthenticatedTestHelper.getTokenWithPermission(),HttpStatus.OK);
        assertTrue(containsEntity(entity, expectedCreatedEntity));
        identifier = entity.getIdentifier();

        //// check GET ALL entities
        // as anonymous user
        logger.info("fetch object as anonymous user");
        entities = getAllEntities(null);
        entity = getFirstEntityWithIdentifier(entities, identifier);
        assertNotNull(entity);
        assertTrue(containsEntity(entity, expectedCreatedEntity));

        // as userWithoutPermission
        logger.info("fetch all entities as userWithoutPermission");
        entities = getAllEntities(AuthenticatedTestHelper.getTokenWithoutPermission());
        entity = getFirstEntityWithIdentifier(entities, identifier);
        assertNotNull(entity);
        assertTrue(containsEntity(entity, expectedCreatedEntity));

        // as userWithPermission
        logger.info("fetch all entities as userWithPermission");
        entities = getAllEntities(AuthenticatedTestHelper.getTokenWithPermission());
        entity = getFirstEntityWithIdentifier(entities, identifier);
        assertNotNull(entity);
        assertTrue(containsEntity(entity, expectedCreatedEntity));

        // as admin
        logger.info("fetch all entities as admin");
        entities = getAllEntities(AuthenticatedTestHelper.getTokenAdmin());
        entity = getFirstEntityWithIdentifier(entities, identifier);
        assertNotNull(entity);
        assertTrue(containsEntity(entity, expectedCreatedEntity));

        //// delete created
        logger.info("delete created entity");
        deleteEntity(identifier, AuthenticatedTestHelper.getTokenWithPermission(), HttpStatus.OK);

        // check entity count
        currentCount = getAllEntities(AuthenticatedTestHelper.getTokenAdmin()).size();
        assertEquals(countAsAdmin, currentCount);

        expectedCreatedEntity.setVisibility(visibilityBefore);
    }

    private void checkGETALLonPRIVATE(SimpleEntityRequest request, T expectedCreatedEntity) throws IOException, UnirestException {
        List<T> entities;
        T entity;
        String identifier;
        int countAsAdmin;
        int currentCount;
        SimpleEntityRequest localRequest = new SimpleEntityRequest(request);
        OwnedResource.Visibility visibilityBefore = expectedCreatedEntity.getVisibility();

        logger.info("check GET ALL on PRIVATE entity");

        // count existing entities
        countAsAdmin = getAllEntities(AuthenticatedTestHelper.getTokenAdmin()).size();

        // assert visibility=PUBLIC
        expectedCreatedEntity.setVisibility(OwnedResource.Visibility.PRIVATE);
        localRequest.putParameter(OwnedResourceManagingController.visibilityParameterName, OwnedResource.Visibility.PRIVATE.name());

        //// create entity
        // as userWithPermission: use this in further requests
        logger.info("create PRIVATE entity with request body, parameters and headers");
        entity = createEntity(localRequest, AuthenticatedTestHelper.getTokenWithPermission(),HttpStatus.OK);
        assertTrue(containsEntity(entity, expectedCreatedEntity));
        identifier = entity.getIdentifier();

        //// check GET ALL entities
        // as anonymous user
        logger.info("fetch object as anonymous user");
        entities = getAllEntities(null);
        entity = getFirstEntityWithIdentifier(entities, identifier);
        assertNull(entity);

        // as userWithoutPermission
        logger.info("fetch all entities as userWithoutPermission");
        entities = getAllEntities(AuthenticatedTestHelper.getTokenWithoutPermission());
        entity = getFirstEntityWithIdentifier(entities, identifier);
        assertNull(entity);

        // as userWithPermission
        logger.info("fetch all entities as userWithPermission");
        entities = getAllEntities(AuthenticatedTestHelper.getTokenWithPermission());
        entity = getFirstEntityWithIdentifier(entities, identifier);
        assertNotNull(entity);
        assertTrue(containsEntity(entity, expectedCreatedEntity));

        // as admin
        logger.info("fetch all entities as admin");
        entities = getAllEntities(AuthenticatedTestHelper.getTokenAdmin());
        entity = getFirstEntityWithIdentifier(entities, identifier);
        assertNotNull(entity);
        assertTrue(containsEntity(entity, expectedCreatedEntity));

        //// delete created
        logger.info("delete created entity");
        deleteEntity(identifier, AuthenticatedTestHelper.getTokenWithPermission(), HttpStatus.OK);

        // check entity count
        currentCount = getAllEntities(AuthenticatedTestHelper.getTokenAdmin()).size();
        assertEquals(countAsAdmin, currentCount);

        expectedCreatedEntity.setVisibility(visibilityBefore);
    }

    private void checkUPDATE(SimpleEntityRequest request, SimpleEntityRequest updateRequest, T expectedCreatedEntity, T expectedUpdatedEntity, String notExistingIdentifier) throws IOException, UnirestException {
        T entity;
        T updatedEntity;
        String identifier;
        int countAsAdmin;
        int currentCount;

        logger.info("check UPDATE");

        // count existing entites
        countAsAdmin = getAllEntities(AuthenticatedTestHelper.getTokenAdmin()).size();

        //// create entity
        // as userWithPermission: use this in further requests
        logger.info("create entity with request body, parameters and headers");
        entity = createEntity(request, AuthenticatedTestHelper.getTokenWithPermission(),HttpStatus.OK);
        assertTrue(containsEntity(entity, expectedCreatedEntity));
        identifier = entity.getIdentifier();

        //// check UPDATE entity
        // as anonymous user
        logger.info("update entity as anonymous user: should return UNAUTHORIZED");
        LoggingHelper.loggerIgnore(LoggingHelper.accessDeniedExceptions);
        updatedEntity = updateEntity(identifier, updateRequest, null, HttpStatus.UNAUTHORIZED);
        LoggingHelper.loggerUnignore(LoggingHelper.accessDeniedExceptions);

        // as userWithoutPermission
        logger.info("update entity as userWithoutPermission: should return UNAUTHORIZED");
        LoggingHelper.loggerIgnore(LoggingHelper.accessDeniedExceptions);
        updatedEntity = updateEntity(identifier, updateRequest, AuthenticatedTestHelper.getTokenWithoutPermission(), HttpStatus.UNAUTHORIZED);
        LoggingHelper.loggerUnignore(LoggingHelper.accessDeniedExceptions);

        // as userWithPermission
        logger.info("update entity as userWithPermission");
        updatedEntity = updateEntity(identifier, updateRequest, AuthenticatedTestHelper.getTokenWithPermission(), HttpStatus.OK);
        assertTrue(containsEntity(updatedEntity, expectedUpdatedEntity));
        // remove updated entity
        logger.info("delete updated entity");
        deleteEntity(identifier, AuthenticatedTestHelper.getTokenWithPermission(), HttpStatus.OK);

        //// create entity
        // as userWithPermission: use this in further requests
        logger.info("create entity with request body, parameters and headers");
        entity = createEntity(request, AuthenticatedTestHelper.getTokenWithPermission(),HttpStatus.OK);
        assertTrue(containsEntity(entity, expectedCreatedEntity));
        identifier = entity.getIdentifier();
        // as admin
        logger.info("update entity as admin");
        updatedEntity = updateEntity(identifier, updateRequest, AuthenticatedTestHelper.getTokenAdmin(), HttpStatus.OK);
        assertTrue(containsEntity(updatedEntity, expectedUpdatedEntity));
        // remove updated entity
        logger.info("delete updated entity");
        deleteEntity(identifier, AuthenticatedTestHelper.getTokenAdmin(), HttpStatus.OK);

        // try to update not existing entity
        logger.info("update not existing entity: should return NOT_FOUND");
        LoggingHelper.loggerIgnore(LoggingHelper.ownedResourceNotFoundException);
        updateEntity(notExistingIdentifier, updateRequest, AuthenticatedTestHelper.getTokenAdmin(), HttpStatus.NOT_FOUND);
        LoggingHelper.loggerUnignore(LoggingHelper.ownedResourceNotFoundException);

        // check entity count
        currentCount = getAllEntities(AuthenticatedTestHelper.getTokenAdmin()).size();
        assertEquals(countAsAdmin, currentCount);
    }

    private void checkDELETE(SimpleEntityRequest request, T expectedCreatedEntity, String notExistingIdentifier) throws IOException, UnirestException {
        T entity;
        int countAsAdmin;
        int currentCount;

        logger.info("check DELETE");

        // count existing entites
        countAsAdmin = getAllEntities(AuthenticatedTestHelper.getTokenAdmin()).size();

        // create entity
        // as userWithPermission
        logger.info("create entity with request body, parameters and headers");
        entity = createEntity(request, AuthenticatedTestHelper.getTokenWithPermission(),HttpStatus.OK);
        assertTrue(containsEntity(entity, expectedCreatedEntity));

        // as anonymous user
        logger.info("delete entity as anonymous user: should return UNAUTHORIZED");
        LoggingHelper.loggerIgnore(LoggingHelper.accessDeniedExceptions);
        deleteEntity(entity.getIdentifier(), null, HttpStatus.UNAUTHORIZED);
        LoggingHelper.loggerUnignore(LoggingHelper.accessDeniedExceptions);

        // as userWithoutPermission user
        logger.info("delete entity as anonymous user: should return UNAUTHORIZED");
        LoggingHelper.loggerIgnore(LoggingHelper.accessDeniedExceptions);
        deleteEntity(entity.getIdentifier(), AuthenticatedTestHelper.getTokenWithoutPermission(), HttpStatus.UNAUTHORIZED);
        LoggingHelper.loggerUnignore(LoggingHelper.accessDeniedExceptions);

        // as userWithPermission
        logger.info("delete entity as userWithPermission");
        deleteEntity(entity.getIdentifier(), AuthenticatedTestHelper.getTokenWithPermission(), HttpStatus.OK);

        // create entity
        // as userWithPermission
        logger.info("create entity with request body, parameters and headers");
        entity = createEntity(request, AuthenticatedTestHelper.getTokenWithPermission(),HttpStatus.OK);
        assertTrue(containsEntity(entity, expectedCreatedEntity));

        // as admin
        logger.info("delete entity as admin");
        deleteEntity(entity.getIdentifier(), AuthenticatedTestHelper.getTokenAdmin(), HttpStatus.OK);

        // try to update not existing entity
        logger.info("delete not existing entity: should return NOT_FOUND");
        LoggingHelper.loggerIgnore(LoggingHelper.ownedResourceNotFoundException);
        deleteEntity(notExistingIdentifier, AuthenticatedTestHelper.getTokenAdmin(), HttpStatus.NOT_FOUND);
        LoggingHelper.loggerUnignore(LoggingHelper.ownedResourceNotFoundException);

        // check entity count
        currentCount = getAllEntities(AuthenticatedTestHelper.getTokenAdmin()).size();
        assertEquals(countAsAdmin, currentCount);
    }


    private T getFirstEntityWithIdentifier(List<T> entities, String identifier){
        for(T entity: entities){
            if(entity.getIdentifier().equals(identifier))
                return entity;
        }
        return null;
    }

    public boolean containsEntity(T entity1, T entity2) throws JsonProcessingException {
        JSONObject jObject1 = new JSONObject(entity1.toJson());
        JSONObject jObject2 = new JSONObject(entity2.toJson());
        for(Iterator iterator = jObject2.keySet().iterator(); iterator.hasNext();) {
            String key = (String) iterator.next();
            Object o1 = jObject1.get(key);
            if(jObject2.isNull(key) || key.equals(creationTimeIdentifier) || key.equals(idIdentifier))// || key.equals(identifierIdentifier))
                continue;
            Object o2 = jObject2.get(key);
            if(!o1.toString().equals(o2.toString())) {
                logger.info("entities are not equal at key=\""+key+"\": \""+o1.toString()+"\" != \""+o2.toString()+"\"");
                return false;
            }
        }
        return true;
    }


    // CREATE
    @SuppressWarnings("unchecked")
    public T createEntity(SimpleEntityRequest request, String token, HttpStatus expectedStatus) throws IOException, UnirestException {
        HttpResponse<String> response;
        String url = ath.getAPIBaseUrl() + service;
        response = ath.addAuthentication(Unirest.post(url), token)
                .headers(request.getHeaders())
                .header("content-type", SerializationFormatMapper.JSON)
                .queryString(request.getParameters())
                .body(request.getBody())
                .asString();
        assertEquals(expectedStatus.value(), response.getStatus());
        if(expectedStatus.equals(HttpStatus.OK)){
            String contentType = response.getHeaders().getFirst("content-type").split(";")[0];
            assertEquals(SerializationFormatMapper.JSON, contentType);
        }
        try {
            return (T) T.fromJson(response.getBody(), clazz);//fromJSON(response.getBody());
        }catch (IOException e){
            logger.info("json response was not valid concerning the entity class");
            return null;
        }
    }


    //UPDATE
    @SuppressWarnings("unchecked")
    public T updateEntity(String identifier, SimpleEntityRequest request, String token, HttpStatus expectedStatus) throws IOException, UnirestException {
        HttpResponse<String> response;
        String url = ath.getAPIBaseUrl() + service;
        response = ath.addAuthentication(Unirest.put(url+"/"+identifier), token)
                .headers(request.getHeaders())
                .header("content-type", SerializationFormatMapper.JSON)
                .queryString(request.getParameters())
                .body(request.getBody())
                .asString();

        assertEquals(expectedStatus.value(), response.getStatus());
        if(expectedStatus.equals(HttpStatus.OK)){
            String contentType = response.getHeaders().getFirst("content-type").split(";")[0];
            assertEquals(SerializationFormatMapper.JSON, contentType);
        }
        try {
            return (T) T.fromJson(response.getBody(), clazz);//fromJSON(response.getBody());
        }catch (IOException e){
            logger.info("json response was not valid concerning the entity class");
            return null;
        }
    }

    //DELETE
    public void deleteEntity(String identifier, String token, HttpStatus expectedStatus ) throws UnirestException {
        HttpResponse<String> response;
        String url = ath.getAPIBaseUrl() + service + "/"+identifier;
        response = ath.addAuthentication(Unirest.delete(url), token)
                .asString();
        assertEquals(expectedStatus.value(), response.getStatus());
    }


    // GET
    @SuppressWarnings("unchecked")
    public T getEntity(String identifier, String token, HttpStatus expectedStatus) throws UnirestException, IOException {
        HttpResponse<String> response;
        String url = ath.getAPIBaseUrl() + service;
        response = ath.addAuthentication(Unirest.get(url+"/"+identifier), token)
                .asString();
        assertEquals(expectedStatus.value(), response.getStatus());
        if(expectedStatus.equals(HttpStatus.OK)){
            String contentType = response.getHeaders().getFirst("content-type").split(";")[0];
            assertEquals(SerializationFormatMapper.JSON, contentType);
        }

        try {
            return (T) T.fromJson(response.getBody(), clazz);//fromJSON(response.getBody());
        }catch (IOException e){
            logger.info("json response was not valid concerning the entity class");
            return null;
        }
    }

    //GETALL
    public List<T> getAllEntities(String token) throws UnirestException, IOException {
        HttpResponse<String> response;
        String url = ath.getAPIBaseUrl() + service;
        response = ath.addAuthentication(Unirest.get(url), token)
                .asString();
        assertEquals(HttpStatus.OK.value(), response.getStatus());

        String contentType = response.getHeaders().getFirst("content-type").split(";")[0];
        assertEquals(SerializationFormatMapper.JSON, contentType);

        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(response.getBody(),
                TypeFactory.defaultInstance().constructCollectionType(List.class, clazz));
    }

}
