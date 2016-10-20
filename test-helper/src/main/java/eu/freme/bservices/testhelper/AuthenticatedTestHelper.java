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

import javax.annotation.PostConstruct;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.HttpRequest;

import eu.freme.common.persistence.model.User;
import eu.freme.common.rest.BaseRestController;

import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static org.junit.Assert.assertTrue;

/**
 * Created by Arne Binder (arne.b.binder@gmail.com) on 13.01.2016.
 */
@Component
public class AuthenticatedTestHelper {
	
	@Autowired
	TestHelper testHelper;

    Logger logger = Logger.getLogger(AuthenticatedTestHelper.class);

    private static String tokenWithPermission;
    private static String tokenWithOutPermission;
    private static String tokenAdmin;

    private static boolean authenticated = false;

    private final String usernameWithPermission = "userwithpermission";
    private final String passwordWithPermission = "testpassword";
    private final String usernameWithoutPermission = "userwithoutpermission";
    private final String passwordWithoutPermission = "testpassword";

    private static User userWithPermission;
    private static User userWithoutPermission;

    public String getAPIBaseUrl(){
        return testHelper.getAPIBaseUrl();
    }

    /**
     * This method creates and authenticates two users, userwithpermission and userwithoutpermission.
     * Furthermore the admin token is created.
     *
     * @throws UnirestException
     */
    public void authenticateUsers() throws UnirestException, IOException {
        if (!authenticated) {
            //Creates two users, one intended to have permission, the other not
            userWithPermission = createUser(usernameWithPermission, passwordWithPermission);
            tokenWithPermission = authenticateUser(usernameWithPermission, passwordWithPermission);
            userWithoutPermission = createUser(usernameWithoutPermission, passwordWithoutPermission);
            tokenWithOutPermission = authenticateUser(usernameWithoutPermission, passwordWithoutPermission);
            //ConfigurableApplicationContext context = IntegrationTestSetup.getApplicationContext();
            tokenAdmin = authenticateUser(testHelper.getAdminUsername(), testHelper.getAdminPassword());
            authenticated = true;
        }
    }

    /**
     * This method deletes the users created for authentication purposes.
     *
     * @throws UnirestException
     */
    public void removeAuthenticatedUsers() throws UnirestException {
        if (authenticated) {
            deleteUser(usernameWithPermission, tokenWithPermission);
            deleteUser(usernameWithoutPermission, tokenWithOutPermission);
            authenticated = false;
        }
    }

    /**
     * Use this method to add an authentication header to the request.
     * If the given token is null, the request will not be modified.
     *
     * @param request The request to add the authentication
     * @param token   The authentication Token
     * @param <T>
     * @return The modified request
     */
    @SuppressWarnings("unchecked")
    public <T extends HttpRequest> T addAuthentication(T request, String token) {
        if (token == null)
            return request;
        return (T) request.header("X-Auth-Token", token);
    }

    /**
     * Add authentication for the user intended to have permission to the given request.
     *
     * @param request the request to modify
     * @return the modified request
     */
    public <T extends HttpRequest> T addAuthentication(T request) {
        return addAuthentication(request, tokenWithPermission);
    }

    /**
     * Add authentication for the user intended to have no permission to the given request.
     *
     * @param request the request to modify
     * @return the modified request
     */
    public <T extends HttpRequest> T addAuthenticationWithoutPermission(T request) {
        return addAuthentication(request, tokenWithOutPermission);
    }

    /**
     * Add authentication for the admin user to the given request.
     *
     * @param request the request to modify
     * @return the modified request
     */
    public <T extends HttpRequest> T addAuthenticationWithAdmin(T request) {
        return addAuthentication(request, tokenAdmin);
    }


    public User createUser(String username, String password) throws UnirestException, IOException {
        logger.info("create user: " + username);
        HttpResponse<String> response = Unirest.post(testHelper.getAPIBaseUrl() + "/user")
                .queryString("username", username)
                .queryString("password", password).asString();
        assertTrue(response.getStatus() == HttpStatus.OK.value());
        ObjectMapper mapper = new ObjectMapper();
        User user = mapper.readValue(response.getBody(), User.class);
        return user;
    }

    public void deleteUser(String username, String token) throws UnirestException {
        logger.info("delete user: " + username);
        HttpResponse<String> response = addAuthentication(Unirest.delete(testHelper.getAPIBaseUrl() + "/user/" + username), token).asString();
        assertTrue(response.getStatus() == HttpStatus.NO_CONTENT.value());
    }

    public String authenticateUser(String username, String password) throws UnirestException {
        HttpResponse<String> response;

        logger.info("login with new user / create token");
        response = Unirest
                .post(testHelper.getAPIBaseUrl() + BaseRestController.authenticationEndpoint)
                .header("X-Auth-Username", username)
                .header("X-Auth-Password", password).asString();
        assertTrue(response.getStatus() == HttpStatus.OK.value());
        String token = new JSONObject(response.getBody()).getString("token");
        return token;
    }

    public static String getTokenWithPermission() {
        return tokenWithPermission;
    }

    public static String getTokenWithoutPermission() {
        return tokenWithOutPermission;
    }

    public static String getTokenAdmin() {
        return tokenAdmin;
    }

    public static User getUserWithPermission() {
        return userWithPermission;
    }

    public static User getUserWithoutPermission() {
        return userWithoutPermission;
    }
}
