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
package eu.freme.bservices.controllers.users;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import eu.freme.common.exception.AccessDeniedException;
import eu.freme.common.exception.BadRequestException;
import eu.freme.common.rest.BaseRestController;

import org.apache.log4j.Appender;
import org.apache.log4j.Logger;
import org.apache.log4j.filter.ExpressionFilter;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpStatus;

import static org.junit.Assert.assertTrue;
import eu.freme.common.starter.FREMEStarter;

import java.util.Enumeration;

@Ignore
public class UserControllerTest{

	ConfigurableApplicationContext context;
	private static boolean started = false;

	private static String baseUrl = null;
	Logger logger = Logger.getLogger(UserControllerTest.class);


	private static String adminUsername;
	private static String adminPassword;

	@Before
	public void setup() {
		if(!started) {

			context =  FREMEStarter.startPackageFromClasspath("users-test-package.xml");

			String port = context.getEnvironment().getProperty("server.port");
			if (port == null) {
				port = "8080";
			}
			baseUrl = "http://localhost:" + port;

			adminUsername = context.getEnvironment().getProperty("admin.username");
			adminPassword = context.getEnvironment().getProperty("admin.password");

			started = true;
		}
	}

	@Test
	public void testUserSecurity() throws UnirestException {

		String username = "myuser";
		String password = "mypassword";

		logger.info("create user");
		logger.info(baseUrl + "/user");
		HttpResponse<String> response = Unirest.post(baseUrl + "/user")
				.queryString("username", username)
				.queryString("password", password).asString();
		assertTrue(response.getStatus() == HttpStatus.OK.value());
		String responseUsername = new JSONObject(response.getBody()).getString("name");
		assertTrue(username.equals(responseUsername));

		logger.info("create user with dublicate username - should not work, exception is ok");
		loggerIgnore(badRequestExceptions);
		response = Unirest.post(baseUrl + "/user")
				.queryString("username", username)
				.queryString("password", password).asString();
		assertTrue(response.getStatus() == HttpStatus.BAD_REQUEST.value());
		loggerUnignore(badRequestExceptions);


		logger.info("create users with invalid usernames - should not work");
		loggerIgnore(badRequestExceptions);
		response = Unirest.post(baseUrl + "/user")
				.queryString("username", "123")
				.queryString("password", password).asString();
		assertTrue(response.getStatus() == HttpStatus.BAD_REQUEST.value());

		response = Unirest.post(baseUrl + "/user")
				.queryString("username", "*abc")
				.queryString("password", password).asString();
		assertTrue(response.getStatus() == HttpStatus.BAD_REQUEST.value());

		response = Unirest.post(baseUrl + "/user")
				.queryString("username", adminUsername)
				.queryString("password", password).asString();
		assertTrue(response.getStatus() == HttpStatus.BAD_REQUEST.value());

		response = Unirest.post(baseUrl + "/user")
				.queryString("username", "")
				.queryString("password", password).asString();
		assertTrue(response.getStatus() == HttpStatus.BAD_REQUEST.value());
		loggerUnignore(badRequestExceptions);


		logger.info("login with bad password should fail");
		loggerIgnore(accessDeniedExceptions);
		response = Unirest
				.post(baseUrl + BaseRestController.authenticationEndpoint)
				.header("X-Auth-Username", username)
				.header("X-Auth-Password", password + "xyz").asString();
		assertTrue(response.getStatus() == HttpStatus.UNAUTHORIZED.value());
		loggerUnignore(accessDeniedExceptions);

		logger.info("login with new user / create token");
		response = Unirest
				.post(baseUrl + BaseRestController.authenticationEndpoint)
				.header("X-Auth-Username", username)
				.header("X-Auth-Password", password).asString();
		assertTrue(response.getStatus() == HttpStatus.OK.value());
		String token = new JSONObject(response.getBody()).getString("token");
		assertTrue(token != null);
		assertTrue(token.length() > 0);

		logger.info("delete user without providing credentials - should fail, exception is ok");
		loggerIgnore(accessDeniedExceptions);
		response = Unirest.delete(baseUrl + "/user/" + username).asString();
		assertTrue(response.getStatus() == HttpStatus.UNAUTHORIZED.value());
		loggerUnignore(accessDeniedExceptions);

		logger.info("create a 2nd user");
		String otherUsername = "otheruser";
		response = Unirest.post(baseUrl + "/user").queryString("username", otherUsername)
				.queryString("password", password).asString();
		assertTrue(response.getStatus() == HttpStatus.OK.value());
		String responseOtherUsername = new JSONObject(response.getBody()).getString("name");
		assertTrue( otherUsername.equals(responseOtherUsername));

		logger.info( "delete other user should fail");
		loggerIgnore(accessDeniedExceptions);
		response = Unirest
				.delete(baseUrl + "/user/" + otherUsername)
				.header("X-Auth-Token", token).asString();
		assertTrue(response.getStatus() == HttpStatus.UNAUTHORIZED.value());
		loggerUnignore(accessDeniedExceptions);

		logger.info("cannot do authenticated call with username / password, only token should work");
		loggerIgnore(accessDeniedExceptions);
		response = Unirest
				.delete(baseUrl + "/user/" + username)
				.header("X-Auth-Username", username)
				.header("X-Auth-Password", password).asString();
		assertTrue(response.getStatus() == HttpStatus.UNAUTHORIZED.value());
		loggerUnignore(accessDeniedExceptions);


		logger.info("get user information");
		response = Unirest
				.get(baseUrl + "/user/" + username)
				.header("X-Auth-Token", token).asString();
		assertTrue(response.getStatus() == HttpStatus.OK.value());
		responseUsername = new JSONObject(response.getBody()).getString("name");
		assertTrue(responseUsername.equals(username));

		String newpass = "newpassword";
		logger.info("try to change password without authentication / should fail");
		loggerIgnore(accessDeniedExceptions);
		response = Unirest
				.put(baseUrl + "/user/" + username)
				.queryString("password", newpass)
				.asString();
		assertTrue(response.getStatus() == HttpStatus.UNAUTHORIZED.value());
		loggerUnignore(accessDeniedExceptions);

		logger.info("try to change password with wrong authentication / should fail");
		loggerIgnore(accessDeniedExceptions);
		response = Unirest
				.put(baseUrl + "/user/" + username)
				.header("X-Auth-Token", token+"abc")
				.queryString("password", newpass)
				.asString();
		assertTrue(response.getStatus() == HttpStatus.UNAUTHORIZED.value());
		loggerUnignore(accessDeniedExceptions);

		logger.info("try to change password with correct authentication / should work");
		response = Unirest
				.put(baseUrl + "/user/" + username)
				.queryString("password", newpass)
				.header("X-Auth-Token", token).asString();
		assertTrue(response.getStatus() == HttpStatus.OK.value());

		logger.info("login with old password should fail");
		loggerIgnore(accessDeniedExceptions);
		response = Unirest
				.post(baseUrl + BaseRestController.authenticationEndpoint)
				.header("X-Auth-Username", username)
				.header("X-Auth-Password", password).asString();
		assertTrue(response.getStatus() == HttpStatus.UNAUTHORIZED.value());
		loggerUnignore(accessDeniedExceptions);

		logger.info("login with updated user / update token");
		response = Unirest
				.post(baseUrl + BaseRestController.authenticationEndpoint)
				.header("X-Auth-Username", username)
				.header("X-Auth-Password", newpass).asString();
		assertTrue(response.getStatus() == HttpStatus.OK.value());
		token = new JSONObject(response.getBody()).getString("token");
		assertTrue(token != null);
		assertTrue(token.length() > 0);

		logger.info("delete own user - should work");
		response = Unirest
				.delete(baseUrl + "/user/" + username)
				.header("X-Auth-Token", token).asString();
		assertTrue(response.getStatus() == HttpStatus.NO_CONTENT.value());

	}

	@Test
	public void testAdmin() throws UnirestException{

		String username = "carlos";
		String password = "carlosss";
		logger.info("create user \"" + username + "\" and get token");

		HttpResponse<String> response = Unirest.post(baseUrl + "/user")
				.queryString("username", username)
				.queryString("password", password).asString();

		response = Unirest
				.post(baseUrl + BaseRestController.authenticationEndpoint)
				.header("X-Auth-Username", username)
				.header("X-Auth-Password", password).asString();
		String token = new JSONObject(response.getBody()).getString("token");

		logger.info("try to access /user endpoint from user account - should not work");
		//loggerIgnore(accessDeniedExceptions);
		response = Unirest
				.get(baseUrl + "/user")
				.header("X-Auth-Token", token).asString();

		assertTrue(response.getStatus() == HttpStatus.UNAUTHORIZED.value());
		//loggerUnignore(accessDeniedExceptions);

		logger.info("access /user endpoint with admin credentials");
		response = Unirest
				.post(baseUrl + BaseRestController.authenticationEndpoint)
				.header("X-Auth-Username", adminUsername)
				.header("X-Auth-Password", adminPassword).asString();
		token = new JSONObject(response.getBody()).getString("token");

		response = Unirest
				.get(baseUrl + "/user")
				.header("X-Auth-Token", token).asString();
		assertTrue(response.getStatus() == HttpStatus.OK.value());

		logger.info("access user through access token passed via query string");
		response = Unirest
				.get(baseUrl + "/user")
				.queryString("token", token).asString();
		assertTrue(response.getStatus() == HttpStatus.OK.value());


		logger.info("admin can update the password of carlos");
		response = Unirest
				.put(baseUrl + "/user/" + username)
				.queryString("password", "newpassword")
				.header("X-Auth-Token", token).asString();
		assertTrue(response.getStatus() == HttpStatus.OK.value());

		logger.info("admin can delete carlos");
		response = Unirest
				.delete(baseUrl + "/user/" + username)
				.header("X-Auth-Token", token).asString();
		assertTrue(response.getStatus() == HttpStatus.NO_CONTENT.value());

		response = Unirest
				.get(baseUrl + "/user")
				.header("X-Auth-Token", token).asString();
		assertTrue(response.getStatus() == HttpStatus.OK.value());

	}
	
	//@After
	//public void after(){
	//	context.stop();
	//}

	public static final String accessDeniedExceptions = AccessDeniedException.class.getName() + " || EXCEPTION ~=" + org.springframework.security.access.AccessDeniedException.class.getName();
	public static final String badRequestExceptions = BadRequestException.class.getName();

	/**
	 * Sets specific LoggingFilters and initiates suppression of specified Exceptions in Log4j.
	 * @param x Class of Exception
	 **/
	public static void loggerIgnore(Class<Throwable> x){
		loggerIgnore(x.getName());
	}

	/**
	 * Sets specific LoggingFilters and initiates suppression of specified Exceptions in Log4j.
	 * @param x String name of Exception
	 **/
	public static void loggerIgnore(String x) {

		String newExpression="EXCEPTION ~="+x;
		Enumeration<Appender> allAppenders= Logger.getRootLogger().getAllAppenders();
		Appender appender;

		while (allAppenders.hasMoreElements()) {
			appender=allAppenders.nextElement();
			String oldExpression;
			ExpressionFilter exp;
			try {
				exp = ((ExpressionFilter) appender.getFilter());
				oldExpression = exp.getExpression();
				if (!oldExpression.contains(newExpression)) {
					exp.setExpression(oldExpression + " || " + newExpression);
					exp.activateOptions();
				}
			} catch (NullPointerException e) {
				exp = new ExpressionFilter();
				exp.setExpression(newExpression);
				exp.setAcceptOnMatch(false);
				exp.activateOptions();
				appender.clearFilters();
				appender.addFilter(exp);
			}
		}
	}

	/**
	 * Clears specific LoggingFilters and stops their suppression of Exceptions in Log4j.
	 * @param x Class of Exception
	 **/
	public static void loggerUnignore(Class<Throwable> x) {
		loggerUnignore(x.getName());
	}

	/**
	 * Clears specific LoggingFilters and stops their suppression of Exceptions in Log4j.
	 * @param x String name of Exception
	 **/
	public static void loggerUnignore(String x) {
		Enumeration<Appender> allAppenders= Logger.getRootLogger().getAllAppenders();
		Appender appender;

		while (allAppenders.hasMoreElements()) {
			appender=allAppenders.nextElement();
			ExpressionFilter exp = ((ExpressionFilter) appender.getFilter());
			exp.setExpression(exp.getExpression().replace("|| EXCEPTION ~=" + x, "").replace("EXCEPTION ~=" + x + "||", ""));
			exp.activateOptions();
			appender.clearFilters();
			appender.addFilter(exp);
		}
	}

	/**
	 * Clears all LoggingFilters for all Appenders. Stops suppression of Exceptions in Log4j.
	 **/
	public static void loggerClearFilters() {
		Enumeration<Appender> allAppenders = Logger.getRootLogger().getAllAppenders();
		Appender appender;

		while (allAppenders.hasMoreElements()) {
			appender = allAppenders.nextElement();
			appender.clearFilters();
		}
	}
}
