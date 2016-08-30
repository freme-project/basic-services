/**
 * Copyright (C) 2015 Agro-Know, Deutsches Forschungszentrum fÃ¼r KÃ¼nstliche Intelligenz, iMinds,
 * Institut fÃ¼r Angewandte Informatik e. V. an der UniversitÃ¤t Leipzig,
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
package eu.freme.bservices.filters.internationalizationfilter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.base.Strings;

import eu.freme.bservices.internationalization.api.InternationalizationAPI;
import eu.freme.common.conversion.SerializationFormatMapper;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.ReaderInputStream;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import eu.freme.common.exception.BadRequestException;
import eu.freme.common.exception.InternalServerErrorException;
import eu.freme.common.exception.ExceptionHandlerService;
import eu.freme.bservices.internationalization.okapi.nif.converter.ConversionException;

import org.springframework.web.filter.GenericFilterBean;

/**
 * Filter that converts HTML and XLIFF input to NIF and changes the informat
 * parameter. It also performs roundtripping, e.g. to convert HTML to NIF and
 * then back to HTML.
 *
 * @author Jan Nehring - jan.nehring@dfki.de
 */

@Component
@Order(90)
public class InternationalizationFilter extends GenericFilterBean {

	/**
	 * Disable e-Internationalization filter for a set of endpoints with the
	 * configuration option freme.einternationalization.endpoint-blacklist. You
	 * can configure a comma separated list of regular expressions. For example
	 * the regular expression like /toolbox/.* will match the endpoint
	 * http://localhost:8080/toolbox/test
	 */
	@Value("#{'${freme.einternationalization.endpoint-blacklist:/toolbox/convert/.*}'.split(',')}")
	Set<String> endpointBlacklist;

	Set<Pattern> endpointBlacklistRegex;

	@Autowired
	ExceptionHandlerService exceptionHandlerService;

	@Autowired
	SerializationFormatMapper serializationFormatMapper;

	private Logger logger = Logger.getLogger(InternationalizationFilter.class);

	@Autowired
	InternationalizationAPI internationalizationApi;

	@PostConstruct
	public void doInit() {
		endpointBlacklistRegex = new HashSet<Pattern>();
		for (String str : endpointBlacklist) {
			endpointBlacklistRegex.add(Pattern.compile(str));
		}
	}

	/**
	 * Determines format of request. Returns null if the format is not suitable
	 * for eInternationalization Filter.
	 *
	 * @param req
	 * @return
	 */
	public String getInformat(HttpServletRequest req)
			throws BadRequestException {
		String informat = req.getParameter("informat");
		if (informat == null && req.getContentType() != null) {
			informat = req.getContentType();
			String[] parts = informat.split(";");
			if (parts.length > 1) {
				informat = parts[0].trim();
			}
			if (informat.equals("*/*"))
				informat = null;
		}
		if (informat == null) {
			return null;
		}

		if (serializationFormatMapper.get(informat.toLowerCase()) == null) {
			logger.warn("Unsupported informat='"
					+ informat
					+ "'. The following serialization format values are acceptable: "
					+ serializationFormatMapper.keySet().stream()
							.collect(Collectors.joining(", ")));
			return null;
		}
		informat = serializationFormatMapper.get(informat.toLowerCase());
		if (internationalizationApi.getSupportedMimeTypes().contains(informat)) {
			return informat;
		} else {
			return null;
		}
	}

	/**
	 * Determines format of request. Returns null if the format is not suitable
	 * for eInternationalization Filter.
	 *
	 * @param req
	 * @return
	 */
	public String getOutformat(HttpServletRequest req)
			throws BadRequestException {
		String outformat = req.getParameter("outformat");
		if (outformat == null && req.getHeader("Accept") != null) {
			outformat = req.getHeader("Accept");
			String[] parts = outformat.split(";");
			if (parts.length > 1) {
				outformat = parts[0].trim();
			}
			if (outformat.equals("*/*"))
				outformat = null;
		}
		if (outformat == null) {
			return null;
		}

		if (serializationFormatMapper.get(outformat.toLowerCase()) == null) {
			logger.warn("Unsupported outformat='"
					+ outformat
					+ "'. The following serialization format values are acceptable: "
					+ serializationFormatMapper.keySet().stream()
							.collect(Collectors.joining(", ")));
			return null;
		}
		outformat = serializationFormatMapper.get(outformat.toLowerCase());
		if (internationalizationApi.getRoundtrippingFormats().contains(
				outformat)) {
			return outformat;
		} else {
			return null;
		}
	}

	public void doFilter(ServletRequest req, ServletResponse res,
			FilterChain chain) throws IOException, ServletException {
		if (!(req instanceof HttpServletRequest)
				|| !(res instanceof HttpServletResponse)) {
			chain.doFilter(req, res);
			return;
		}

		HttpServletRequest httpRequest = (HttpServletRequest) req;
		HttpServletResponse httpResponse = (HttpServletResponse) res;

		if (httpRequest.getMethod().equals("OPTIONS")) {
			chain.doFilter(req, res);
			return;
		}

		// process e-Internalization switch parameter
		String enable = req
				.getParameter(InternationalizationAPI.switchParameterName);
		if (!Strings.isNullOrEmpty(enable)) {
			enable = enable.trim().toLowerCase();

			if (!enable.equals("false") && !enable.equals("true")
					&& !enable.equals("undefined")) {
				Exception exception = new BadRequestException("The parameter "
						+ InternationalizationAPI.switchParameterName
						+ " has the unknown value = '" + enable
						+ "'. Use either 'true', 'false' or 'undefined'.");
				exceptionHandlerService.writeExceptionToResponse(httpRequest,
						httpResponse, exception);
				return;
			}

			if (enable.equals("false")) {
				chain.doFilter(req, res);
				return;
			}
		}

		if (Strings.isNullOrEmpty(enable)
				|| !enable.trim().toLowerCase().equals("true")) {
			String uri = httpRequest.getRequestURI();
			for (Pattern pattern : endpointBlacklistRegex) {
				if (pattern.matcher(uri).matches()) {
					chain.doFilter(req, res);
					return;
				}
			}
		}

		String informat;
		String outformat = null;
		try {
			informat = getInformat(httpRequest);
			// check if post-processing filter is set
			if (httpRequest.getParameter("filter") == null)
				outformat = getOutformat(httpRequest);
		} catch (BadRequestException exception) {
			exceptionHandlerService.writeExceptionToResponse(httpRequest,
					httpResponse, exception);
			return;
		}

		if (outformat != null
				&& (informat == null || !outformat.equals(informat))) {
			Exception exception = new BadRequestException(
					"Can only convert to outformat \"" + outformat
							+ "\" when informat is also \"" + outformat + "\"");
			exceptionHandlerService.writeExceptionToResponse(httpRequest,
					httpResponse, exception);
			return;
		}

		if (outformat != null
				&& !internationalizationApi.getRoundtrippingFormats().contains(
						outformat)) {
			Exception exception = new BadRequestException("\"" + outformat
					+ "\" is not a valid output format");
			exceptionHandlerService.writeExceptionToResponse(httpRequest,
					httpResponse, exception);
			return;
		}

		if (informat == null) {
			chain.doFilter(req, res);
			return;
		}

		boolean roundtripping = false;
		if (outformat != null) {
			roundtripping = true;
			logger.debug("convert from " + informat + " to " + outformat);
		} else {
			logger.debug("convert input from " + informat + " to nif");
		}

		// do conversion of informat to nif
		// create BodySwappingServletRequest

		String inputQueryString = req.getParameter("input");
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		try (InputStream requestInputStream = inputQueryString == null ? /*
																		 * read
																		 * data
																		 * from
																		 * request
																		 * body
																		 */req
				.getInputStream() : /*
									 * read data from query string input
									 * parameter
									 */new ReaderInputStream(new StringReader(
				inputQueryString), "UTF-8")) {
			// copy request content to buffer
			IOUtils.copy(requestInputStream, baos);
			requestInputStream.close();
		}

		// create request wrapper that converts the body of the request from the
		// original format to turtle
		Reader nif;

		byte[] baosData = baos.toByteArray();
		if (baosData.length == 0) {
			Exception exception = new BadRequestException(
					"No input data found in request.");
			exceptionHandlerService.writeExceptionToResponse(httpRequest,
					httpResponse, exception);
			return;
		}

		String nifVersion = httpRequest.getParameter("nif-version");
		if (nifVersion != null
				&& !(nifVersion.equals("2.0") || nifVersion.equals("2.1"))) {
			throw new BadRequestException("\"" + nifVersion
					+ "\" is not a valid value for nif-version.");
		}

		ByteArrayInputStream bais = new ByteArrayInputStream(baosData);
		try {
			nif = internationalizationApi.convertToTurtle(bais,
					informat.toLowerCase(), nifVersion);
		} catch (ConversionException e) {
			logger.error("Error", e);
			throw new InternalServerErrorException("Conversion from \""
					+ informat + "\" to NIF failed");
		} finally {
			bais.close();
		}

		BodySwappingServletRequest bssr = new BodySwappingServletRequest(
				(HttpServletRequest) req, nif, roundtripping);

		if (!roundtripping) {
			chain.doFilter(bssr, res);
			nif.close();
			return;
		}

		ConversionHttpServletResponseWrapper dummyResponse;

		try {
			dummyResponse = new ConversionHttpServletResponseWrapper(
					httpResponse, internationalizationApi,
					new ByteArrayInputStream(baosData), informat, outformat,
					nifVersion);

			chain.doFilter(bssr, dummyResponse);
			nif.close();

			ServletOutputStream sos = httpResponse.getOutputStream();

			byte[] data;
			if (HttpStatus.Series.valueOf(dummyResponse.getStatus()).equals(
					HttpStatus.Series.SUCCESSFUL)) {
				data = dummyResponse.writeBackToClient(true);
				httpResponse.setContentType(outformat);
			} else {
				data = dummyResponse.writeBackToClient(false);
				httpResponse.setContentType(dummyResponse.getContentType());
			}

			httpResponse.setContentLength(data.length);
			sos.write(data);
			sos.flush();
			sos.close();

		} catch (ConversionException e) {
			e.printStackTrace();
			// exceptionHandlerService.writeExceptionToResponse((HttpServletResponse)res,new
			// InternalServerErrorException());
		}
	}

	// public void init(FilterConfig filterConfig) {
	// }

	public void destroy() {
	}
}
