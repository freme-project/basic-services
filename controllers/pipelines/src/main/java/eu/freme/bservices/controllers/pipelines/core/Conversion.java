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

import eu.freme.bservices.internationalization.api.InternationalizationAPI;
import eu.freme.bservices.internationalization.okapi.nif.converter.ConversionException;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

/**
 * <p>Converts HTML to NIF and back using e-Internationalization</p>
 * <p>
 * <p>Copyright 2015 MMLab, UGent</p>
 *
 * @author Gerald Haesendonck
 */
public class Conversion {
	private final InternationalizationAPI internationalizationApi;
	private String skeletonNIF = "";

	public Conversion(InternationalizationAPI internationalizationApi) {
		this.internationalizationApi = internationalizationApi;
	}

	public String convertToNif(final String input, String mimeType) throws IOException, ConversionException {
		try (InputStream in = IOUtils.toInputStream(input, StandardCharsets.UTF_8)) {
			try (Reader reader = internationalizationApi.convertToTurtleWithMarkups(in, mimeType)) {
				skeletonNIF = IOUtils.toString(reader);
			}
		}
		try (InputStream in = IOUtils.toInputStream(input, StandardCharsets.UTF_8)) {
			try (Reader reader = internationalizationApi.convertToTurtle(in, mimeType)) {
				return IOUtils.toString(reader);
			}
		}
	}

	public String convertBack(final String enrichedNIF) throws IOException {
		try (
				InputStream enrichedFile = IOUtils.toInputStream(enrichedNIF, StandardCharsets.UTF_8);
				InputStream skeletonFile = IOUtils.toInputStream(skeletonNIF, StandardCharsets.UTF_8)
		) {
			try (Reader htmlReader = internationalizationApi.convertBack(skeletonFile, enrichedFile)) {
				String output = IOUtils.toString(htmlReader);
				skeletonNIF = "";
				return output;
			}
		}
	}
}
