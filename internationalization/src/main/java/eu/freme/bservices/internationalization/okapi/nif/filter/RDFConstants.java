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
package eu.freme.bservices.internationalization.okapi.nif.filter;

public class RDFConstants {

	public static final String nifPrefix = "http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core#";
	// These two prefixes should be used
	public static final String nifPrefix_2_0 = "http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core/2.0";
	public static final String nifPrefix_2_1 = "http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core/2.1";
	
	public static final String itsrdfPrefix = "http://www.w3.org/2005/11/its/rdf#";
	public static final String xsdPrefix = "http://www.w3.org/2001/XMLSchema#";
	public static final String typePrefix = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
	public static final String dcPrefix = "http://purl.org/dc/elements/1.1/";
	
	// used in a nif 2.1 file, i.e. nif:predLang isolang:eng
	public static final String isolangPrefix = "http://www.lexvo.org/id/iso639-3/";
	
	public static final String nifAnnotationPrefix = "http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-annotation#";
	
	// These should not be used anymore as the nifPrefix can be one of nifPrefix_2_0 and nifPrefix_2_1
	public static final String WAS_CONVERTED_FROM_PROP = nifPrefix + "wasConvertedFrom";
	public static final String IS_STRING_PROP = nifPrefix + "isString";
	public static final String ANCHOR_OF_PROP = nifPrefix + "anchorOf";
	
	// Properties
	public static final String IS_STRING = "isString";
	public static final String ANCHOR_OF = "anchorOf";
	public static final String BEGIN_INDEX = "beginIndex";
	public static final String END_INDEX = "endIndex";
	public static final String IDENTIFIER = "identifier";
	public static final String PRED_LANG = "predLang";
	public static final String ANNOTATION_UNIT = "annotationUnit";
	public static final String REFERENCE_CONTEXT = "referenceContext";
	public static final String WAS_CONVERTED_FROM = "wasConvertedFrom";
	
	public static final String NIF20_OFFSET = "#char=";
	public static final String NIF21_OFFSET = "#offset_";
	
	// Types
	public static final String NIF20_STRINGS_IDENTIFIER = "RFC5147String";
	public static final String NIF21_STRINGS_IDENTIFIER = "OffsetBasedString";
	public static final String NIF_STRING_TYPE = "String";
	public static final String NIF_CONTEXT_TYPE = "Context";
	public static final String NIF_PHRASE_TYPE = "Phrase";
	
	// Prefixes
	public static final String NIF_PREFIX = "nif";
	public static final String ISOLANG_PREFIX = "isolang";
	public static final String XSD_PREFIX = "xsd";
	public static final String ITS_RDF_PREFIX = "itsrdf";
	public static final String DC_PREFIX = "dc";
	
	public static enum RDFSerialization {
		
		TURTLE("TTL"), JSON_LD("JSON-LD"), PLAINTEXT(null);
		
		private String rdfLang;
		
		private RDFSerialization(String rdfLang) {
			this.rdfLang = rdfLang;
		}
		
		public String toRDFLang(){
			return rdfLang;
		}
		
	}
}

