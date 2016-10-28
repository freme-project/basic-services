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

import eu.freme.common.exception.BadRequestException;
import eu.freme.common.exception.InternalServerErrorException;
import eu.freme.common.persistence.dao.UserDAO;
import eu.freme.common.persistence.model.User;
import eu.freme.common.persistence.tools.AccessLevelHelper;
import eu.freme.common.security.PasswordHasher;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.access.vote.AbstractAccessDecisionManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
public class UserController {

	@Autowired
	AbstractAccessDecisionManager decisionManager;

	@Autowired
	UserDAO userDAO;

	@Autowired
	AccessLevelHelper accessLevelHelper;
	
	private Logger logger = Logger.getLogger(UserController.class);

	@RequestMapping(value = "/user", method = RequestMethod.POST)
	public User createUser(
			@RequestParam(value = "username") String username,
			@RequestParam(value = "password") String password) {

		if (userDAO.getRepository().findOneByName(username) != null) {
			throw new BadRequestException("Username already exists");
		}
		
		// validate that username consists only of charahters
		if( !username.matches("[a-zA-Z]+")){
			throw new BadRequestException("The username can only consist of normal characters from a-z and A-Z");
		}
		
		// passwords need to have at least 8 characters
		if( password.length() < 8 ){
			throw new BadRequestException("The passwords needs to be at least 8 characters long");
		}

		if (username.equals("ROLE_USER") || username.equals("ROLE_ADMIN") || username.equals("ROLE_ANONYMOUS")) {
			throw new BadRequestException("The username can not be" + username + " because this name is reserved for a user role");
		}
		try {
			String hashedPassword = PasswordHasher.getSaltedHash(password);
			User user = new User(username, hashedPassword, User.roleUser);
			user = userDAO.save(user);
			return user;
		} catch (Exception e) {
			logger.error(e);
			throw new InternalServerErrorException();
		}
	}

	@RequestMapping(value = "/user/{username}", method = RequestMethod.PUT)
	@Secured({"ROLE_USER", "ROLE_ADMIN"})
	public User updateUser(
			@PathVariable("username") String username,
			@RequestParam(value = "password") String password) {

		User user = userDAO.getRepository().findOneByName(username);
		if (user == null) {
			throw new BadRequestException("User not found");
		}

		Authentication authentication = SecurityContextHolder.getContext()
				.getAuthentication();
		decisionManager.decide(authentication, user, accessLevelHelper.writeAccess());

		// passwords need to have at least 8 characters
		if( password.length() < 8 ){
			throw new BadRequestException("The passwords needs to be at least 8 characters long");
		}
		try {
			String hashedPassword = PasswordHasher.getSaltedHash(password);
			user.setPassword(hashedPassword);
			user = userDAO.save(user);
			return user;
		} catch (Exception e) {
			logger.error(e);
			throw new InternalServerErrorException();
		}
	}

	@RequestMapping(value = "/user/{username}", method = RequestMethod.GET)
	@PreAuthorize("hasRole('ROLE_USER')")
	public User getUser(@PathVariable("username") String username) {

		User user = userDAO.getRepository().findOneByName(username);
		if (user == null) {
			throw new BadRequestException("User not found");
		}

		Authentication authentication = SecurityContextHolder.getContext()
				.getAuthentication();
		decisionManager.decide(authentication, user, accessLevelHelper.readAccess());
		return user;
	} 

	@RequestMapping(value = "/user/{username}", method = RequestMethod.DELETE)
	@Secured({"ROLE_USER", "ROLE_ADMIN"})
	public ResponseEntity<String> deleteUser(@PathVariable("username") String username) {

		User user = userDAO.getRepository().findOneByName(username);
		if (user == null) {
			throw new BadRequestException("User not found");
		}

		Authentication authentication = SecurityContextHolder.getContext()
				.getAuthentication();
		decisionManager.decide(authentication, user, accessLevelHelper.writeAccess());
		userDAO.delete(user);

		return new ResponseEntity<String>(HttpStatus.NO_CONTENT);
	}
	
	@RequestMapping(value="/user", method= RequestMethod.GET)
	@PreAuthorize("hasRole('ROLE_ADMIN')")
	public Iterable<User> getUsers(){
		return userDAO.findAll();
	}

}
