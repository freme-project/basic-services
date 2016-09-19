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
package eu.freme.bservices.controllers.pipelines.requests;

import eu.freme.common.conversion.rdf.RDFConstants;
import eu.freme.common.persistence.model.PipelineRequest;

import java.util.HashMap;
import java.util.Map;

import static eu.freme.common.conversion.rdf.RDFConstants.TURTLE;


/**
 * <p>To ease the creation of a request.</p>
 *
 * @author Gerald Haesendonck
 */
public class RequestBuilder {
	private PipelineRequest.HttpMethod httpMethod;
	private String service;		// the URI to the specific service, e.g. /e-entity/dbpedia-spotlight/documents
	private String baseEndpoint;
	private String informat;
	private String outformat;
	private String prefix;
	private String body;

	private final Map<String, Object> parameters;	// some extra app-specific parameters that will be added to the URI
	private final Map<String, String> headers;		// some extra app-specific headers.

	/**
	 * Creates a RequestBuilder instance. Use one instance per request.
	 * @param serviceURI	The path to the service, e.g. {@code /e-entity/dbpedia-spotlight/documents}.
	 *                         The current URI's are stored in {@link ServiceConstants}
	 */
	public RequestBuilder(final String serviceURI) {
		service = serviceURI;
		parameters = new HashMap<>(4);
		headers = new HashMap<>(2);

		// the default values:
		httpMethod = PipelineRequest.HttpMethod.POST;
		//baseEndpoint = "http://api.freme-project.eu/0.2";
		baseEndpoint = "http://api.freme-project.eu/current";
		informat = TURTLE;
		outformat = TURTLE;
	}

	/**
	 * Sets the request method.
	 * @param method	The method of HTTP request. Currently only GET and POST are supported; the default is POST.
	 * @return		A builder object with the request method set.
	 */
	public RequestBuilder method(PipelineRequest.HttpMethod method) {
		httpMethod = method;
		return this;
	}

	/**
	 * Sets the base endpoint.
	 * @param baseEndpointURI	The URI to the base URI of the services endpoint. Defaults to http://api.freme-project.eu/0.2
	 * @return	                A builder object with the base endpoint set.
	 */
	public RequestBuilder baseEndpoint(final String baseEndpointURI) {
		this.baseEndpoint = baseEndpoint;
		return this;
	}

	/**
	 * Sets the format of the input.
	 * @param informat	The input format. The default is TURTLE.
	 * @return A builder object with the informat set.
	 */
	public RequestBuilder informat(final String informat) {
		this.informat = informat;
		return this;
	}

	/**
	 * Sets the format of the output.
	 * @param outformat	The output format. The default is TURTLE.
	 * @return A builder object with the outformat set.
	 */
	public RequestBuilder outformat(final String outformat) {
		this.outformat = outformat;
		return this;
	}

	/**
	 * Sets the prefix.
	 * @param prefix     The url of rdf resources generated from plaintext. Has default value "http://freme-project.eu/".
	 * @return           A builder object with the prefix set.
	 */
	public RequestBuilder prefix(final String prefix) {
		this.prefix = prefix;
		return this;
	}

	/**
	 * Sets the body of the request.
	 * @param body 	The body of the request. The default is null, which is treated as an empty body.
	 * @return  	A builder object with the body set.
	 */
	public RequestBuilder body(final String body) {
		this.body = body;
		return this;
	}

	/**
	 * Sets a parameter.
	 * @param name	The name of the parameter
	 * @param value The value of the parameter
	 * @return		A builder with the parameter set.
	 */
	public RequestBuilder parameter(final String name, final String value) {
		parameters.put(name, value);
		return this;
	}

	/**
	 * Sets a header.
	 * @param name	The name of the header
	 * @param value	The value of the header
	 * @return  	A builder with the header set.
	 */
	public RequestBuilder header(final String name, final String value) {
		headers.put(name, value);
		return this;
	}

	/**
	 * Create a PipelineRequest instance with the given values.
	 * @return	A brand new request!
	 */
	public PipelineRequest build() {
		String serviceEndpoint = baseEndpoint + service;
		header("content-type", informat);
		header("accept", outformat);
		if (prefix != null) {
			parameter("prefix", prefix);
		}
		return new PipelineRequest(httpMethod, serviceEndpoint, parameters, headers, body);
	}

}
