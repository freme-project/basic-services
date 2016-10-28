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
package eu.freme.bservices.controllers.pipelines.core;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

import eu.freme.common.conversion.rdf.RDFConstants;

/**
 * Guess the NIF version of a NIF document by checking if the http://purl.org/dc/terms/conformsTo triple exists.
 * 
 * @author Jan Nehring
 */
public class NIFVersionGuesser {

	public String guessNifVersion(Model model) {

		String queryStr = "SELECT * WHERE { \n"
				+ "?res a <http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core#ContextCollection> .\n"
				+ "?res  <http://purl.org/dc/terms/conformsTo> <http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core/2.1>\n"
				+ "}";

		Query query = QueryFactory.create(queryStr);
		QueryExecution qexec = null;
		String nifVersion = RDFConstants.nifVersion2_0;
		try {
			qexec = QueryExecutionFactory.create(query, model);
			if (qexec.execSelect().hasNext()) {
				nifVersion = RDFConstants.nifVersion2_1;
			}
		} finally {
			qexec.close();
		}

		return nifVersion;
	}
}
