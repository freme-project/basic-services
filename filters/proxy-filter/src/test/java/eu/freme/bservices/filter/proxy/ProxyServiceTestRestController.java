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
package eu.freme.bservices.filter.proxy;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import eu.freme.common.exception.InternalServerErrorException;

@RestController
public class ProxyServiceTestRestController {

	@RequestMapping(value = "/proxy-destination", method = RequestMethod.POST)
	public ResponseEntity<String> proxyTarget(HttpServletRequest req,
			@RequestParam("test") String testQueryString,
			@RequestBody String body) {

		if (testQueryString == null || !testQueryString.equals("value")) {
			throw new InternalServerErrorException("error in query string");
		}

		if (body == null || !body.equals("body\nbody")) {
			throw new InternalServerErrorException("error in body");
		}

		String header = req.getHeader("my-header");
		if (header == null || !header.equals("header-value")) {
			throw new InternalServerErrorException("error in header");
		}

		return new ResponseEntity<String>("response", HttpStatus.OK);
	}
	
	@RequestMapping(value = "/proxy-destination2", method = RequestMethod.GET)
	public ResponseEntity<String> proxyTarget(){
		return new ResponseEntity<String>("response", HttpStatus.OK);
	}
}
