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
package eu.freme.common.persistence.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.freme.common.conversion.SerializationFormatMapper;
import eu.freme.common.exception.BadRequestException;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class contains the data to form a HTTP request.
 *
 * @author Gerald Haesendonck
 */
public class PipelineRequest {
	public enum HttpMethod {
		GET, POST
	}

	private HttpMethod method;
	private String endpoint;
	private Map<String, Object> parameters;
	private Map<String, String> headers;
	private String body;

	@JsonIgnore
	public static final String parameterMark = "$";

	/**
	 * Creates a single request for usage in the pipelines service.
	 * Use the {@link eu.freme.bservices.controllers.pipelines.requests.RequestFactory} or {@link eu.freme.bservices.controllers.pipelines.requests.RequestBuilder} to create requests.
	 * @param method			The method of the request. Can be {@code GET} or {@code POST}.
	 * @param endpoint	    The URI to send te request to. In other words, the service endpoint.
	 * @param parameters	URL parameters to add to the request.
	 * @param headers		HTTP headers to add to the request.
	 * @param body			HTTP body to add to the request. Makes only sense when method is {@code POST}, but it's possible.
	 */
	@JsonCreator
	public PipelineRequest(
			@JsonProperty("method") HttpMethod method,
			@JsonProperty("endpoint") String endpoint,
			@JsonProperty("parameters") Map<String, Object> parameters,
			@JsonProperty("headers") Map<String, String> headers,
			@JsonProperty("body") String body) {

		this.method = method;
		this.endpoint = endpoint;
		this.body = body;

		if( parameters != null ){
			this.parameters = parameters;
		}else{
			this.parameters = new HashMap<>();
		}
		if( headers != null){
			this.headers = new HashMap<>(headers.size(), 1);
			// convert header names to lowercase (not their values). This is important for further processing...
			for (Map.Entry<String, String> header2value : headers.entrySet()) {
				this.headers.put(header2value.getKey().toLowerCase(), header2value.getValue());
			}
		}else{
			this.headers = new HashMap<>();
		}
	}

	public HttpMethod getMethod() {
		return method;
	}

	public String getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}

	public Map<String, Object> getParameters() {
		return parameters;
	}

	public void addParameters(Map<String, Object> newParameters){
		this.parameters.putAll(newParameters);
	}

	public void addParameter(String key, Object value){
		this.parameters.put(key, value);
	}

	public void removeParameter(String key){
		this.parameters.remove(key);
	}

	public Map<String, String> getHeaders() {
		return headers;
	}

	public void addHeaders(Map<String, String> newHeaders){
		this.headers.putAll(newHeaders);
	}
	public void addHeader(String key, String value){
		this.headers.put(key, value);
	}

	public void removeHeader(String key){
		this.headers.remove(key);
	}


	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public String isValid() {
		if (endpoint == null) {
			return "No endpoint given.";
		}
		if (method == null) {
			return "HTTP Method not supported. Only GET and POST are supported.";
		}
		try {
			new URL(endpoint);
		} catch (MalformedURLException e) {
			return e.getMessage();
		}
		return "";
	}


	public String getInputMime(final SerializationFormatMapper mapper) {
		String informat = (String)parameters.get("informat");
		if (informat == null) {
			informat = (String)parameters.get("f");
		}
		if (informat == null) {
			informat = headers.get("content-type");
		}
		if (informat == null) {
			informat = headers.get("Content-Type");
		}
		if(informat!=null && informat.equals("*/*")){
			informat = null;
		}
		return informat != null ? mapper.get(informat) : null;
	}

	public String getOutputMime(final SerializationFormatMapper mapper) {
		String outformat = (String)parameters.get("outformat");
		if (outformat == null) {
			outformat = (String)parameters.get("o");
		}
		if (outformat == null) {
			outformat = headers.get("accept");
		}
		if (outformat == null) {
			outformat = headers.get("Accept");
		}
		if (outformat!=null && outformat.equals("*/*")){
			outformat = null;
		}
		return outformat != null ? mapper.get(outformat) : null;
	}

	public void setInputMime(String contentTypeHeader) {
		clearInputMime();
		if(contentTypeHeader!=null)
			headers.put("content-type", contentTypeHeader);
	}

	public void setOutputMime(String acceptHeader) {
		clearOutputMime();
		if(acceptHeader!=null)
			headers.put("accept", acceptHeader);
	}

	private void clearInputMime(){
		parameters.remove("informat");
		parameters.remove("f");
		headers.remove("Content-Type");
		headers.remove("content-type");
	}

	private void clearOutputMime(){
		parameters.remove("outformat");
		parameters.remove("o");
		headers.remove("Accept");
		headers.remove("accept");
	}

	public void unParametrize(Map<String, Object> parameters) throws BadRequestException{
		endpoint = unParametrize(endpoint, parameters);
		for(String key: this.parameters.keySet()){
			this.parameters.put(key, unParametrize(this.parameters.get(key).toString(), parameters));
		}
		for(String key: this.headers.keySet()){
			this.headers.put(key, unParametrize(this.headers.get(key), parameters));
		}
	}

	private String unParametrize(String original, Map<String, Object> parameters) throws BadRequestException{

		// check, if parameterMark ("$") is contained at least two times
		int count = original.length() - original.replace(parameterMark, "").length();
		if(count < 2){
			return original;
		}
		String result = original;
		Iterator<Map.Entry<String, Object>> it = parameters.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, Object> pair = it.next();
			if(result.contains(parameterMark+pair.getKey()+parameterMark)){
				result = result.replace(parameterMark+pair.getKey()+parameterMark, pair.getValue().toString());
				//it.remove();
				count-=2;
			}
		}

		// error handling
		if(count>1){
			List<String> allMatches = new ArrayList<>();
			String re = "\\"+parameterMark+"([^\\"+parameterMark+"]*)\\"+parameterMark;
			Matcher m = Pattern.compile(re)
					.matcher(result);
			while (m.find()) {
				allMatches.add(m.group(1));
			}
			throw new BadRequestException("Missing parameter(s) to process the string: '"+result+"'. Please specify value(s) for the following parameter(s): "+String.join(", ", allMatches));
		}

		return result;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		PipelineRequest request = (PipelineRequest) o;

		if (method != request.method) return false;
		if (!endpoint.equals(request.endpoint)) return false;
		if (parameters != null ? !parameters.equals(request.parameters) : request.parameters != null) return false;
		if (headers != null ? !headers.equals(request.headers) : request.headers != null) return false;
		return !(body != null ? !body.equals(request.body) : request.body != null);

	}

	@Override
	public int hashCode() {
		int result = method.hashCode();
		result = 31 * result + endpoint.hashCode();
		result = 31 * result + (parameters != null ? parameters.hashCode() : 0);
		result = 31 * result + (headers != null ? headers.hashCode() : 0);
		result = 31 * result + (body != null ? body.hashCode() : 0);
		return result;
	}
}
