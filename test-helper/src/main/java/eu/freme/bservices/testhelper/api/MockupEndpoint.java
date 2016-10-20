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
package eu.freme.bservices.testhelper.api;

import eu.freme.common.conversion.SerializationFormatMapper;
import eu.freme.common.conversion.rdf.RDFConstants;
import eu.freme.common.exception.FileNotFoundException;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;

@RestController
public class MockupEndpoint {

	@Autowired
	SerializationFormatMapper serializationFormatMapper;

	public static final String path = "/mockups/file";

	// use regEx to include the file extension
	@RequestMapping(path+"/{filename:.+}")
	public ResponseEntity<String> sendRDFfileContent(
			@RequestHeader( value="outformat", required=false) String outformat,
			@RequestHeader( value="accept", required=false) String accept,
			@PathVariable String filename

	) throws IOException{
		String fileContent;
		File file;
		try {
			ClassLoader classLoader = getClass().getClassLoader();
			file = new File(classLoader.getResource("mockup-endpoint-data/" + filename.split("\\?")[0]).getFile());
			//File file = new File("src/main/resources/mockup-endpoint-data/"+filename);
			fileContent = FileUtils.readFileToString(file);
		}catch (Exception ex){
			throw new FileNotFoundException("could not load file: "+filename);
		}
		HttpHeaders headers = new HttpHeaders();

		String contentType = serializationFormatMapper.get(outformat);
		if(contentType==null && accept!=null && !accept.equals("*/*")){
			contentType = serializationFormatMapper.get(accept.split(";")[0]);
		}
		// default to TURTLE
		// serializationFormatMapper.get() can return null!
		if(contentType == null){
			contentType = RDFConstants.TURTLE;
		}
		headers.add("Content-Type", contentType);
		headers.add("content-length", file.length()+"");

		ResponseEntity<String> response = new ResponseEntity<>(fileContent, headers, HttpStatus.OK);
		return response;
	}
}
