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
package eu.freme.bservices.controllers.nifconverter;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import com.hp.hpl.jena.rdf.model.Model;

import eu.freme.common.exception.BadRequestException;
import eu.freme.common.exception.FREMEHttpException;
import eu.freme.common.rest.BaseRestController;
import eu.freme.common.rest.NIFParameterSet;

/**
 * This implements an anything to NIF converter.
 * You can send plaintext or the input formats
 * that e-Internationalisation supports to the
 * endpoint and it returns NIF.
 *
 * Created by Arne Binder (arne.b.binder@gmail.com) on 09.03.2016.
 */
@RestController
public class NifConverterController extends BaseRestController{

	Logger logger = Logger.getLogger(NifConverterController.class);

	@Autowired HttpServletRequest request;

	@RequestMapping(value = "/toolbox/nif-converter", method = RequestMethod.POST)
	public ResponseEntity<String> convert(
			HttpServletRequest request, 
			@RequestHeader(value = "Accept") String acceptHeader,
			@RequestHeader(value = "Content-Type") String contentTypeHeader,
			@RequestBody(required = false) String postBody,
			@RequestParam Map<String, String> allParams
			) {
		
		if( ( allParams.containsKey("informat") && allParams.get("informat").equals("TIKAFile") ) ||
				(allParams.containsKey("i") && allParams.get("i").equals("TIKAFile"))	){

			InputStream stream = null;
			File inputFile = null;
			File tempDir = new File(System.getProperty("java.io.tmpdir"));
			MultipartFile file1 = null;

			if (request instanceof MultipartHttpServletRequest){
				MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
				file1 = multipartRequest.getFile("inputFile");
				if(file1==null){
					String msg = "There is no input file when TIKAFile informat has been specified";
					logger.error(msg);

					throw new BadRequestException(msg);
				}
				if (!file1.isEmpty()) {
					try {
						File tempFile = new File(tempDir + File.separator + file1.getOriginalFilename());
						tempFile.createNewFile(); 
						FileOutputStream fos = new FileOutputStream(tempFile); 
						fos.write(file1.getBytes());
						fos.close(); 
						inputFile = tempFile;
						
						stream = new FileInputStream(inputFile);
					} catch (Exception e) {
						String msg = "There was a problem copying the inputfile";
						logger.error(msg);
						throw new BadRequestException(msg);
					}
				} else {
					String msg = "The inputfile is empty";
					logger.error(msg);
					throw new BadRequestException(msg);
				}
			}

			// Read from file without MultipartRequest
			else{
//				if(!allParams.containsKey("filename")){
//					String msg = "Please specify filename in the parameter 'filename'";
//					throw new BadRequestException(msg);
//				}
				// read File from Body 		    	
				stream = new ByteArrayInputStream(postBody.getBytes());

			}

			String text="";
			try{
				AutoDetectParser parser = new AutoDetectParser();
				BodyContentHandler handler = new BodyContentHandler();
				Metadata metadata = new Metadata();
				parser.parse(stream, handler, metadata);
				text = handler.toString();
				stream.close();
			}
			catch(Exception e){
				String msg = "There was a problem parsing the input file";
				logger.error(msg);
				logger.error(e);

				throw new BadRequestException(msg);
			}
			postBody = text;
			allParams.put("input", text);
			allParams.put("i", text);
			allParams.put("informat", "text");
			allParams.put("f", "text");
		}

		NIFParameterSet nifParameters =  normalizeNif(postBody,
				acceptHeader, contentTypeHeader, allParams, false);

		try {
			Model model = getRestHelper().convertInputToRDFModel(nifParameters);
			return createSuccessResponse(model, nifParameters.getOutformatString());
		}catch (FREMEHttpException e){
			logger.error("Error", e);
			throw e;
		} catch (Exception e) {
			logger.error("Error", e);
			throw new BadRequestException(e.getMessage());
		}
	}

}
