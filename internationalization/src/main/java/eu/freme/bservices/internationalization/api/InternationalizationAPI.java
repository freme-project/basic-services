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
package eu.freme.bservices.internationalization.api;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;

import eu.freme.bservices.internationalization.okapi.nif.converter.ConversionException;
import eu.freme.bservices.internationalization.okapi.nif.converter.NifConverter;
//import net.sf.okapi.common.MimeTypeMapper;
import eu.freme.bservices.internationalization.okapi.nif.converter.HTMLBackConverter;
import eu.freme.common.rest.MimeTypeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class InternationalizationAPI {

	@Autowired
	MimeTypeMapper mimeTypeMapper;

	private HashSet<String> supportedMimeTypes;

	public static final String MIME_TYPE_XLIFF_1_2 = net.sf.okapi.common.MimeTypeMapper.XLIFF_MIME_TYPE;

	public static final String MIME_TYPE_HTML = net.sf.okapi.common.MimeTypeMapper.HTML_MIME_TYPE;

	public static final String MIME_TYPE_XML = net.sf.okapi.common.MimeTypeMapper.XML_MIME_TYPE;

	public static final String MIME_TYPE_ODT = net.sf.okapi.common.MimeTypeMapper.OPENOFFICE_MIME_TYPE;

	private static final String FREME_NIF_URI_PREFIX = "http://freme-project.eu/";

	private NifConverter converter;

	private HTMLBackConverter backConverter;

	public InternationalizationAPI() {

		converter = new NifConverter();
		backConverter = new HTMLBackConverter();
		supportedMimeTypes = new HashSet<>();

		supportedMimeTypes.add(MIME_TYPE_XLIFF_1_2);
		supportedMimeTypes.add(MIME_TYPE_HTML);
		supportedMimeTypes.add(MIME_TYPE_XML);
		supportedMimeTypes.add(MIME_TYPE_ODT);
	}

	@PostConstruct
	public void init(){
		mimeTypeMapper.put(MIME_TYPE_XLIFF_1_2, MIME_TYPE_XLIFF_1_2);
		mimeTypeMapper.put(MIME_TYPE_HTML, MIME_TYPE_HTML);
		mimeTypeMapper.put(MIME_TYPE_XML, MIME_TYPE_XML);
		mimeTypeMapper.put("application/xml", MIME_TYPE_XML);
		mimeTypeMapper.put(MIME_TYPE_ODT, MIME_TYPE_ODT);
	}

	public HashSet<String> getSupportedMimeTypes() {
		return supportedMimeTypes;
	}

	public Reader convertToTurtle(InputStream is, String mimeType)
			throws ConversionException {

		Reader reader = null;
		InputStream turtleStream = converter.convert2Nif(is, mimeType, FREME_NIF_URI_PREFIX);
		try {
			reader = new InputStreamReader(turtleStream, "UTF-8");
			
		} catch (UnsupportedEncodingException e) {
			//UTF-8 encoding should always be supported
		}
		return reader;
	}
	
	public Reader convertToTurtleWithMarkups(InputStream is, String mimeType) throws ConversionException{
		
		Reader reader = null;
		InputStream turtleStream = converter.convert2NifWithMarkers(is, mimeType, FREME_NIF_URI_PREFIX);
		try {
			reader = new InputStreamReader(turtleStream, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// UTF-8 encoding should always be supported
		}
		return reader;
	}
	
	public Reader convertBack(InputStream markupsFile, InputStream enrichedFile){
		
		Reader reader = null;
		InputStream originalStream = backConverter.convertBack(markupsFile, enrichedFile);
		try {
			reader = new InputStreamReader(originalStream, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// UTF-8 encoding should always be supported
		}
		return reader;
	}
}
