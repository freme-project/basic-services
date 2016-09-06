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

/**
 * <p>Makes creating requests less painful. It returns default requests for services that can be modified afterwards.</p>
 * @author Gerald Haesendonck
 */
public class RequestFactory {

	/**
	 * Creates a default request to the e-Entity Spotlight service.
	 * @param text		The text to enrich (plain text).
	 * @param language	The language the text is in.
	 * @return			A request for e-Entity Spotlight.
	 */
	@SuppressWarnings("unused")
	public static PipelineRequest createEntitySpotlight(final String text, final String language) {
		RequestBuilder builder = new RequestBuilder(ServiceConstants.E_ENTITY_SPOTLIGHT.getUri());
		return builder
				.informat(RDFConstants.RDFSerialization.PLAINTEXT)
				.parameter("language", language)
				.body(text).build();
	}

	/**
	 * Creates a default request to the e-Entity Spotlight service without input. This is used when the input is the output of
	 * another request or for templates.
	 * @param language	The language the text is in.
	 * @return			A request for e-Entity Spotlight.
	 */
	@SuppressWarnings("unused")
	public static PipelineRequest createEntitySpotlight(final String language) {
		RequestBuilder builder = new RequestBuilder(ServiceConstants.E_ENTITY_SPOTLIGHT.getUri());
		return builder
				.informat(RDFConstants.RDFSerialization.PLAINTEXT)
				.parameter("language", language)
				.build();
	}

	/**
	 * Creates a default request to the e-Entity FREME NER service. This is used when the input is the output of
	 * another request or for templates.
	 * @param text		The text to enrich (plain text).
	 * @param language  The language the text is in.
	 * @param dataSet   The data set to use for enrichment.
	 * @return        	A request for e-Entity FREME NER.
	 */
	@SuppressWarnings("unused")
	public static PipelineRequest createEntityFremeNER(final String text, final String language, final String dataSet) {
		RequestBuilder builder = new RequestBuilder(ServiceConstants.E_ENTITY_FREME_NER.getUri());
		return builder
				.informat(RDFConstants.RDFSerialization.PLAINTEXT)
				.parameter("language", language)
				.parameter("dataset", dataSet)
				.body(text)
				.build();
	}

	/**
	 * Creates a default request to the e-Entity FREME NER service without input. This is used when the input is the output of
	 * another request or for templates.
	 * @param language  The language the text is in.
	 * @param dataSet   The data set to use for enrichment.
	 * @return        	A request for e-Entity FREME NER.
	 */
	@SuppressWarnings("unused")
	public static PipelineRequest createEntityFremeNER(final String language, final String dataSet) {
		RequestBuilder builder = new RequestBuilder(ServiceConstants.E_ENTITY_FREME_NER.getUri());
		return builder
				.informat(RDFConstants.RDFSerialization.PLAINTEXT)
				.parameter("language", language)
				.parameter("dataset", dataSet)
				.build();
	}

	/**
	 * Creates a default request to the e-Link service without input. This is used when the input is the output of
	 * another request or for templates.
	 * @param templateID	The template ID to use for linking.
	 * @return				A request for e-Link.
	 */
	@SuppressWarnings("unused")
	public static PipelineRequest createLink(final String templateID) {
		RequestBuilder builder = new RequestBuilder(ServiceConstants.E_LINK.getUri());
		return builder
				.parameter("templateid", templateID)
				.build();
	}

	/**
	 * Creates a default request to the e-Translate service.
	 * @param text			The input text to translate, in plain text.
	 * @param sourceLang 	The source language.
	 * @param targetLang	The target language.
	 * @return				A request for e-Translate.
	 */
	@SuppressWarnings("unused")
	public static PipelineRequest createTranslation(final String text, final String sourceLang, final String targetLang) {
		RequestBuilder builder = new RequestBuilder(ServiceConstants.E_TRANSLATION.getUri());
		return builder
				.parameter("source-lang", sourceLang)
				.parameter("target-lang", targetLang)
				.informat(RDFConstants.RDFSerialization.PLAINTEXT)
				.body(text)
				.build();
	}

	/**
	 * Creates a default request to the e-Translate service without input. This is used when the input is the output of
	 * another request or for templates.
	 * @param sourceLang 	The source language.
	 * @param targetLang	The target language.
	 * @return				A request for e-Translate.
	 */
	@SuppressWarnings("unused")
	public static PipelineRequest createTranslation(final String sourceLang, final String targetLang) {
		RequestBuilder builder = new RequestBuilder(ServiceConstants.E_TRANSLATION.getUri());
		return builder
				.parameter("source-lang", sourceLang)
				.parameter("target-lang", targetLang)
				.build();
	}

	/**
	 * Creates a default request to the e-Terminology service without input. This is used when the input is the output of
	 * another request or for templates.
	 * @param sourceLang 	The source language.
	 * @param targetLang	The target language.
	 * @return				A request for e-Translate.
	 */
	@SuppressWarnings("unused")
	public static PipelineRequest createTerminology(final String sourceLang, final String targetLang) {
		RequestBuilder builder = new RequestBuilder(ServiceConstants.E_TERMINOLOGY.getUri());
		return builder
				.parameter("source-lang", sourceLang)
				.parameter("target-lang", targetLang)
				.build();
	}
}
