/**
 * Copyright © 2015 Agro-Know, Deutsches Forschungszentrum für Künstliche Intelligenz, iMinds,
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
package eu.freme.bservices.internationalization.okapi.nif.converter;

import java.util.Arrays;

/**
 * Exception raised when a request is submitted for an unsupported MIME type.
 */
public class UnsupportedMimeTypeException extends Exception {

	/** Serial version UID. */
	private static final long serialVersionUID = 1L;

	/**
	 * Constructor.
	 * 
	 * @param unsupportedMimeType
	 *            the unsupported MIME type.
	 * @param supportedTypes
	 *            the list of supported MIME types.
	 */
	public UnsupportedMimeTypeException(String unsupportedMimeType,
			String[] supportedTypes) {

		super(buildMessage(unsupportedMimeType, supportedTypes));
	}

	/**
	 * Builds the exception message.
	 * 
	 * @param unsupportedMimeType
	 *            the unsupported MIME type
	 * @param supportedTypes
	 *            the list of supported MIME types.
	 * @return the exception message.
	 */
	private static String buildMessage(String unsupportedMimeType,
			String[] supportedTypes) {

		return "Unsupported MIME Type: " + unsupportedMimeType
				+ ". Supported types are " + Arrays.toString(supportedTypes);
	}

}
