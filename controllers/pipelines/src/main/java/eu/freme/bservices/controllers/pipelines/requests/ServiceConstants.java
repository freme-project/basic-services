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
package eu.freme.bservices.controllers.pipelines.requests;

/**
 * @author Gerald Haesendonck
 */
public enum ServiceConstants {
	E_ENTITY_SPOTLIGHT("/e-entity/dbpedia-spotlight/documents"),
	E_ENTITY_FREME_NER("/e-entity/freme-ner/documents"),
	E_LINK("/e-link/documents/"),
	E_TRANSLATION("/e-translation/tilde"),
	E_TERMINOLOGY("/e-terminology/tilde");

	private final String uri;

	ServiceConstants(String uri) {
		this.uri = uri;
	}

	public String getUri() {
		return uri;
	}
}
