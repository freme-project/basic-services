/**
 * Copyright (C) 2015 Agro-Know, Deutsches Forschungszentrum für Künstliche Intelligenz, iMinds,
 * Institut für Angewandte Informatik e. V. an der Universität Leipzig,
 * Istituto Superiore Mario Boella, Tilde, Vistatec, WRIPL (http://freme-project.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.freme.bservices.controllers.pipelines.core;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.HttpRequestWithBody;
import eu.freme.bservices.internationalization.api.InternationalizationAPI;
import eu.freme.bservices.internationalization.okapi.nif.converter.ConversionException;
import eu.freme.common.conversion.SerializationFormatMapper;
import eu.freme.common.conversion.rdf.RDFConstants;
import eu.freme.common.conversion.rdf.RDFSerializationFormats;
import eu.freme.common.persistence.model.SerializedRequest;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Gerald Haesendonck
 */
@Component
public class PipelineService {
	private final static Logger logger = Logger.getLogger(PipelineService.class);

	@Autowired
	private RDFSerializationFormats serializationFormats;

	@Autowired
	private InternationalizationAPI internationalizationApi;

	@Autowired
	SerializationFormatMapper serializationFormatMapper;

	/**
	 * Performs a chain of requests to other e-services (pipeline).
	 * @param serializedRequests  Requests to different services, serialized in JSON.
	 * @return                    The result of the pipeline.
	 */
	@SuppressWarnings("unused")
	public WrappedPipelineResponse chain(final List<SerializedRequest> serializedRequests, String useI18n) throws IOException, UnirestException, ServiceException {
		Map<String, Long> executionTime = new LinkedHashMap<>();

		// determine mime types of first and last pipeline request
		Conversion conversion = null;
		boolean roundtrip = false; // true: convert HTML input to NIF, execute pipeline, convert back to HTML at the end.
		String mime1 = null;
		String mime2 = null;
		if (serializedRequests.size() > 1) {
			mime1 = serializedRequests.get(0).getInputMime(serializationFormatMapper);
			mime2 = serializedRequests.get(serializedRequests.size() - 1).getOutputMime(serializationFormatMapper);
			if(!(useI18n!=null && useI18n.trim().toLowerCase().equals("false")) && mime1!=null && mime2!=null && mime1.equals(mime2) && internationalizationApi.getRoundtrippingFormats().contains(mime1)){
			//if (mime1.equals(RDFConstants.RDFSerialization.HTML) && mime1.equals(mime2)) {
				roundtrip = true;
				conversion = new Conversion(internationalizationApi);
				try {
					long startOfRequest = System.currentTimeMillis();
					String nif = conversion.convertToNif(serializedRequests.get(0).getBody(), mime1);
					executionTime.put("e-Internationalization (HTML -> NIF)", (System.currentTimeMillis() - startOfRequest));
					serializedRequests.get(0).setBody(nif);
				} catch (ConversionException e) {
					logger.warn("Could not convert the HTLM contents to NIF. Tying to proceed without converting... Error: ", e);
					roundtrip = false;
				}
			}
		}

		PipelineResponse lastResponse = new PipelineResponse(serializedRequests.get(0).getBody(), null);
		long start = System.currentTimeMillis();
		for (int reqNr = 0; reqNr < serializedRequests.size(); reqNr++) {
			long startOfRequest = System.currentTimeMillis();
			SerializedRequest serializedRequest = serializedRequests.get(reqNr);
			try {
				if (roundtrip) {
					serializedRequest.setInputMime(RDFConstants.TURTLE);
					serializedRequest.setOutputMime(RDFConstants.TURTLE);
				}
				lastResponse = execute(serializedRequest, lastResponse.getBody(), lastResponse.getContentType());
			} catch (UnirestException e) {
				throw new UnirestException("Request " + reqNr + ": " + e.getMessage());
			} catch (IOException e) {
				throw new IOException("Request " + reqNr + ": " + e.getMessage());
			} finally {
				long endOfRequest = System.currentTimeMillis();
				executionTime.put(serializedRequest.getEndpoint(), (endOfRequest - startOfRequest));
			}
		}
		if (roundtrip) {
			long startOfRequest = System.currentTimeMillis();
			String output = conversion.convertBack(lastResponse.getBody());
			lastResponse = new PipelineResponse(output, mime1);
			executionTime.put("e-Internationalization (NIF -> HTML)", (System.currentTimeMillis() - startOfRequest));
		}
		long end = System.currentTimeMillis();
		return new WrappedPipelineResponse(lastResponse, executionTime, (end - start));
	}

	private PipelineResponse execute(final SerializedRequest request, final String body, final String lastResponseContentType) throws UnirestException, IOException, ServiceException {
		switch (request.getMethod()) {
			case GET:
				throw new UnsupportedOperationException("GET is not supported at this moment.");
			default:
				HttpRequestWithBody req = Unirest.post(request.getEndpoint());
				String inputMime = request.getInputMime(serializationFormatMapper);
				if(lastResponseContentType !=null){
					if(inputMime == null){
						inputMime = lastResponseContentType;
					}else{
						if(!inputMime.split(";")[0].trim().toLowerCase().equals(lastResponseContentType.split(";")[0].trim().toLowerCase()))
							logger.warn("The content type header of the last response is '"+lastResponseContentType+"', but '"+inputMime+"' will be used for the request to '"+request.getEndpoint()+"', because it is defined in the pipeline. This can cause errors at this pipeline step.");
						}
				}
				// clear and set it
				if(inputMime!=null){
					request.setInputMime(inputMime);
				}
				if (request.getHeaders() != null && !request.getHeaders().isEmpty()) {
					req.headers(request.getHeaders());
				}
				if (request.getParameters() != null && !request.getParameters().isEmpty()) {
					req.queryString(request.getParameters());	// encode as POST parameters
				}

				HttpResponse<String> response;
				if (body != null) {
					response = req.body(body).asString();
				} else {
					response = req.asString();
				}
				if (!HttpStatus.Series.valueOf(response.getStatus()).equals(HttpStatus.Series.SUCCESSFUL)) {
					String errorBody = response.getBody();
					HttpStatus status = HttpStatus.valueOf(response.getStatus());
					if (errorBody == null || errorBody.isEmpty()) {
						throw new ServiceException(new PipelineResponse( "The service \"" + request.getEndpoint() + "\" reported HTTP status " + status.toString() + ". No further explanation given by service.", RDFConstants.RDFSerialization.PLAINTEXT.contentType()), status);
					} else {
						throw new ServiceException(new PipelineResponse(errorBody, response.getHeaders().getFirst("content-type")), status);
					}
				}
				return new PipelineResponse(response.getBody(), response.getHeaders().getFirst("content-type"));
		}
	}
}
