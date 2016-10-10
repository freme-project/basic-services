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

import com.hp.hpl.jena.rdf.model.Model;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.HttpRequestWithBody;

import eu.freme.bservices.internationalization.api.InternationalizationAPI;
import eu.freme.bservices.internationalization.okapi.nif.converter.ConversionException;
import eu.freme.common.conversion.SerializationFormatMapper;
import eu.freme.common.conversion.rdf.JenaRDFConversionService;
import eu.freme.common.conversion.rdf.RDFConstants;
import eu.freme.common.conversion.rdf.RDFConstants.RDFSerialization;
import eu.freme.common.exception.InternalServerErrorException;
import eu.freme.common.persistence.model.PipelineRequest;

import org.apache.log4j.Logger;
import org.json.JSONObject;
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
	private final static Logger logger = Logger
			.getLogger(PipelineService.class);

	@Autowired
	private InternationalizationAPI internationalizationApi;

	@Autowired
	SerializationFormatMapper serializationFormatMapper;

	public PipelineService() {
		// set timeouts
		Unirest.setTimeouts(60 * 1000, 600 * 1000);
	}

	/**
	 * Performs a chain of requests to other e-services (pipeline).
	 * 
	 * @param pipelineRequests
	 *            Requests to different services, serialized in JSON.
	 * @return The result of the pipeline.
	 */
	@SuppressWarnings("unused")
	public WrappedPipelineResponse chain(
			final List<PipelineRequest> pipelineRequests, String useI18n)
			throws IOException, UnirestException, ServiceException {
		Map<String, Long> executionTime = new LinkedHashMap<>();

		// determine mime types of first and last pipeline request
		Conversion conversion = null;
		boolean roundtrip = false; // true: convert input to NIF, execute
									// pipeline, convert back to input format at
									// the end.
		String mime1 = null;
		String mime2 = null;
		if (pipelineRequests.size() > 1) {
			mime1 = pipelineRequests.get(0).getInputMime(
					serializationFormatMapper);
			mime2 = pipelineRequests.get(pipelineRequests.size() - 1)
					.getOutputMime(serializationFormatMapper);
			if (!(useI18n != null && useI18n.trim().toLowerCase()
					.equals("false"))
					&& mime1 != null
					&& mime2 != null
					&& mime1.equals(mime2)
					&& internationalizationApi.getRoundtrippingFormats()
							.contains(mime1)) {
				roundtrip = true;
				conversion = new Conversion(internationalizationApi);
				try {
					long startOfRequest = System.currentTimeMillis();
					String nif = conversion.convertToNif(pipelineRequests
							.get(0).getBody(), mime1);
					executionTime.put("e-Internationalization (" + mime1
							+ " -> NIF)",
							(System.currentTimeMillis() - startOfRequest));
					pipelineRequests.get(0).setBody(nif);
				} catch (ConversionException e) {
					logger.warn(
							"Could not convert the "
									+ mime1
									+ " contents to NIF. Tying to proceed without converting... Error: ",
							e);
					roundtrip = false;
				}
			}
		}

		PipelineResponse lastResponse = new PipelineResponse(pipelineRequests
				.get(0).getBody(), null);
		long start = System.currentTimeMillis();
		for (int reqNr = 0; reqNr < pipelineRequests.size(); reqNr++) {
			long startOfRequest = System.currentTimeMillis();
			PipelineRequest pipelineRequest = pipelineRequests.get(reqNr);
			try {
				if (roundtrip) {
					pipelineRequest.setInputMime(RDFConstants.TURTLE);
					pipelineRequest.setOutputMime(RDFConstants.TURTLE);
				}
				lastResponse = execute(pipelineRequest, lastResponse.getBody(),
						lastResponse.getContentType());
			} catch (ServiceException e) {
				JSONObject jo = new JSONObject(e.getResponse().getBody());
				throw new PipelineFailedException(jo,
						"The pipeline has failed in step " + reqNr
								+ ", request to URL '"
								+ pipelineRequest.getEndpoint() + "'",
						e.getStatus());
			} catch (UnirestException e) {
				throw new UnirestException("Request #" + reqNr + " at "
						+ pipelineRequest.getEndpoint() + " failed: "
						+ e.getMessage());
			} catch (IOException e) {
				throw new IOException("Request #" + reqNr + " at "
						+ pipelineRequest.getEndpoint() + " failed: "
						+ e.getMessage());
			} finally {
				long endOfRequest = System.currentTimeMillis();
				executionTime.put(pipelineRequest.getEndpoint(),
						(endOfRequest - startOfRequest));
			}
		}
		if (roundtrip) {

			// guess nif version
			String lastBody = lastResponse.getBody();
			Model model;
			String nifVersion = RDFConstants.nifVersion2_0;
			try {
				String contentType = lastResponse.getContentType();
				if (contentType.contains(";")) {
					contentType = contentType.substring(0,
							contentType.indexOf(";"));
				}
				String rdfFormat = serializationFormatMapper.get(contentType);
				model = new JenaRDFConversionService().unserializeRDF(lastBody,
						rdfFormat);
				nifVersion = new NIFVersionGuesser().guessNifVersion(model);
				logger.info("detected nif version: \"" + nifVersion + "\"");
			} catch (Exception e) {
				logger.error("Exception occured:\n" + e);
				throw new InternalServerErrorException(
						"Could not determine the nif version of the pipeline data");
			}

			long startOfRequest = System.currentTimeMillis();
			String output = conversion.convertBack(lastResponse.getBody(),
					nifVersion);
			lastResponse = new PipelineResponse(output, mime1);
			executionTime.put("e-Internationalization (NIF -> " + mime1 + ")",
					(System.currentTimeMillis() - startOfRequest));
		}
		long end = System.currentTimeMillis();
		return new WrappedPipelineResponse(lastResponse, executionTime,
				(end - start));
	}

	private PipelineResponse execute(final PipelineRequest request,
			final String body, final String lastResponseContentType)
			throws UnirestException, IOException, ServiceException {
		switch (request.getMethod()) {
		case GET:
			throw new UnsupportedOperationException(
					"GET is not supported at this moment.");
		default:
			HttpRequestWithBody req = Unirest.post(request.getEndpoint());
			String inputMime = request.getInputMime(serializationFormatMapper);
			if (lastResponseContentType != null) {
				if (inputMime == null) {
					inputMime = lastResponseContentType;
				} else {
					if (!inputMime.split(";")[0]
							.trim()
							.toLowerCase()
							.equals(lastResponseContentType.split(";")[0]
									.trim().toLowerCase()))
						logger.warn("The content type header of the last response is '"
								+ lastResponseContentType
								+ "', but '"
								+ inputMime
								+ "' will be used for the request to '"
								+ request.getEndpoint()
								+ "', because it is defined in the pipeline. This can cause errors at this pipeline step.");
				}
			}
			// clear and set it
			if (inputMime != null) {
				request.setInputMime(inputMime);
			}
			if (request.getHeaders() != null && !request.getHeaders().isEmpty()) {
				req.headers(request.getHeaders());
			}
			if (request.getParameters() != null
					&& !request.getParameters().isEmpty()) {
				req.queryString(request.getParameters()); // encode as POST
															// parameters
			}

			HttpResponse<String> response;
			if (body != null) {
				response = req.body(body).asString();
			} else {
				response = req.asString();
			}
			if (!HttpStatus.Series.valueOf(response.getStatus()).equals(
					HttpStatus.Series.SUCCESSFUL)) {
				String errorBody = response.getBody();
				HttpStatus status = HttpStatus.valueOf(response.getStatus());
				if (errorBody == null || errorBody.isEmpty()) {
					// throw new ServiceException(new PipelineResponse(
					// "The service failed. No further explanation given by service.",
					// SerializationFormatMapper.PLAINTEXT), status);
					throw new ServiceException(
							new PipelineResponse(
									"The service \""
											+ request.getEndpoint()
											+ "\" reported HTTP status "
											+ status.toString()
											+ ". No further explanation given by service.",
									SerializationFormatMapper.PLAINTEXT),
							status);
				} else {
					throw new ServiceException(new PipelineResponse(errorBody,
							response.getHeaders().getFirst("content-type")),
							status);
				}
			}
			return new PipelineResponse(response.getBody(), response
					.getHeaders().getFirst("content-type"));
		}
	}
}
