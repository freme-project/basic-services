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
