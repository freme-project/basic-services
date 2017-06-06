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
package eu.freme.bservices.filters.postprocessing;

import com.google.common.base.Strings;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.request.HttpRequestWithBody;
import eu.freme.bservices.controllers.sparqlconverters.SparqlConverterController;
import eu.freme.common.conversion.SerializationFormatMapper;
import eu.freme.common.conversion.rdf.RDFConstants;
import eu.freme.common.exception.BadRequestException;
import eu.freme.common.exception.ExceptionHandlerService;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by Arne Binder (arne.b.binder@gmail.com) on 02.12.2015.
 */
@ControllerAdvice
public class PostprocessingFilter implements ResponseBodyAdvice {

	private Logger logger = Logger.getLogger(PostprocessingFilter.class);

	public final String HEADER_SECURITY_TOKEN = "X-Auth-Token";

	@Autowired
	ExceptionHandlerService exceptionHandlerService;

	// @Autowired
	// RDFSerializationFormats rdfSerializationFormats;

	@Autowired
	SerializationFormatMapper serializationFormatMapper;

	@Override
	public boolean supports(MethodParameter returnType, Class converterType) {
		return true;
	}

	@Override
	public Object beforeBodyWrite(Object originalBody,
			MethodParameter returnType, MediaType selectedContentType,
			Class selectedConverterType, ServerHttpRequest req,
			ServerHttpResponse res) {

		if (!((req instanceof ServletServerHttpRequest) || (res instanceof ServletServerHttpResponse))) {
			return originalBody;
		} else {
			HttpServletRequest httpRequest = ((ServletServerHttpRequest) req)
					.getServletRequest();
			HttpServletResponse httpResponse = ((ServletServerHttpResponse) res)
					.getServletResponse();

			// skip postprocessing filter for OPTIONS requests
			if (httpRequest.getMethod().toLowerCase().equals("options")
					|| httpRequest.getParameter("filter") == null) {
				return originalBody;
			}

			String responseContent = null;
			int responseStatus = HttpStatus.OK.value();
			String responseContentType = SerializationFormatMapper.JSON;

			String filterUrl = "/toolbox/convert/documents/"
					+ httpRequest.getParameter("filter");

			try {
				// get requested format of response
				String outTypeString = httpRequest.getParameter("outformat");
				if (Strings.isNullOrEmpty(outTypeString))
					outTypeString = httpRequest.getParameter("o");
				if (Strings.isNullOrEmpty(outTypeString)
						&& !Strings.isNullOrEmpty(httpRequest
								.getHeader("Accept"))
						&& !httpRequest.getHeader("Accept").equals("*/*"))
					outTypeString = httpRequest.getHeader("Accept").split(";")[0];

				String outType = SparqlConverterController.CSV;
				if (!Strings.isNullOrEmpty(outTypeString)) {
					outType = serializationFormatMapper.get(outTypeString);
					if (outType == null)
						throw new BadRequestException(
								"Can not use filter: "
										+ httpRequest.getParameter("filter")
										+ " with outformat = \""
										+ httpRequest.getParameter("outformat")
										+ "\" / accept-header = \""
										+ httpRequest.getHeader("Accept")
										+ "\". Ensure, that either outformat or accept header contains a valid value!");
				}

				// set Accept header for original request to turtle
				Map<String, String[]> extraParams = new TreeMap<>();
				// delete outformat parameter
				extraParams.put("outformat", new String[] { "turtle" });
				extraParams.put("filter", null);// new String[]{"turtle"});
				Map<String, String[]> extraHeaders = new TreeMap<>();
				extraHeaders
						.put("Accept", new String[] { RDFConstants.TURTLE });

				String baseUrl = String.format("%s://%s:%d",
						httpRequest.getScheme(), httpRequest.getServerName(),
						httpRequest.getServerPort());

				HttpRequestWithBody filterRequest = Unirest
						.post(baseUrl + filterUrl)
						.header("Content-Type", RDFConstants.TURTLE)
						.header("Accept", outType);

				String token = httpRequest.getHeader(HEADER_SECURITY_TOKEN);
				if (!Strings.isNullOrEmpty(token))
					filterRequest = filterRequest.header(HEADER_SECURITY_TOKEN,
							httpRequest.getHeader(HEADER_SECURITY_TOKEN));

				HttpResponse<String> response = filterRequest
						.body((String)originalBody).asString();

				if (response.getStatus() == HttpStatus.OK.value()) {
					responseContentType = response.getHeaders().getFirst(
							"Content-Type");
				}

				responseContent = response.getBody();
				responseStatus = response.getStatus();

			} catch (Exception e) {
				ResponseEntity<String> responseEntity = exceptionHandlerService
						.handleError(httpRequest, e);
				responseStatus = responseEntity.getStatusCode().value();
				responseContent = responseEntity.getBody();
			}

			res.setStatusCode(HttpStatus.valueOf(responseStatus));
			
			List<String> list = new ArrayList<>();
			list.add(new Integer(responseContent.length()).toString());
			res.getHeaders().put("Content-Length", list);
			return responseContent;
		}
	}

}
