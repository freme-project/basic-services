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
package eu.freme.bservices.internationalization.okapi.nif.its;


import java.util.ArrayList;
import java.util.List;

/**
 * Constants for ITS annotation in the its-rdf ontology.
 */
public abstract class ItsRdfConstants {

	/* ********* Localization Quality Issue ********** */
	public static final String LQI_COMMENT = "locQualityIssueComment";

	public static final String LQI_ENABLED = "locQualityIssueEnabled";

	// TODO not found issues ref in its-rdf ontology
	public static final String LQI_ISSUESREF = "locQualityIssuesRef";
	//
	public static final String LQI_PROFILEREF = "locQualityIssueProfileRef";

	public static final String LQI_SEVERITY = "locQualityIssueSeverity";

	public static final String LQI_TYPE = "locQualityIssueType";

	/* ********* Provenance ********** */
	public static final String PROV_ORG = "org";
	
	public static final String PROV_ORG_REF = "orgRef";

	public static final String PROV_PERSON = "person";
	
	public static final String PROV_PERSON_REF = "personRef";

	public static final String PROV_PROVREF = "provRef";

	// TODO not found provrec ref in its-rdf ontology
	public static final String PROV_RECSREF = "provenanceRecordsRef";
	//
	
	public static final String PROV_REVORG = "revOrg";
	
	public static final String PROV_REVORG_REF = "revOrgRef";

	public static final String PROV_REVPERSON = "revPerson";
	
	public static final String PROV_REVPERSON_REF = "revPersonRef";

	public static final String PROV_REVTOOL = "revTool";
	
	public static final String PROV_REVTOOL_REF = "revToolRef";

	public static final String PROV_TOOL = "tool";
	
	public static final String PROV_TOOL_REF = "toolRef";

	/* ********* MT Confidence ********** */
	public static final String MTCONFIDENCE_VALUE = "mtConfidence";

	/* ********* Allowed Characters ********** */
	public static final String ALLOWEDCHARS_VALUE = "allowedCharacters";

	/* ********* Tools Annotation ********** */
	public static final String MT_CONF_ANNOTS_REF = "mtConfidenceAnnotatorsRef";
	
	public static final String MT_CONF_ANNOTS_REF_OKAPI = "its-mtconfidenceannotatorRefs";

	public static final String TA_ANNOTS_REF = "taAnnotatorsRef";

	public static final String TERM_ANNOTS_REF = "termAnnotatorsRef";

	// TODO not found issues ref in its-rdf ontology
	public static final String ANNOTATORREF = "annotatorsRef";

	//

	public static String getAnnotatorsRef(String annotValue) {

		String annotRef = null;
		if (annotValue != null) {
			if (annotValue.contains("terminology")) {
				annotRef = TERM_ANNOTS_REF;
			} else if (annotValue.contains("text-analysis")) {
				annotRef = TA_ANNOTS_REF;
			} else if (annotValue.contains("mt-confidence")) {
				annotRef = MT_CONF_ANNOTS_REF;
			}
		}
		return annotRef;
	}
	
	public static List<ITSAnnotAttribute> getIstAnnotatorsRef(String annotValue){
		
		List<ITSAnnotAttribute> annotations = new ArrayList<ITSAnnotAttribute>();
		if(annotValue.contains("terminology|")){
			ITSAnnotAttribute attr = new ITSAnnotAttribute();
			attr.setName(TERM_ANNOTS_REF);
			int termIdx = annotValue.indexOf("terminology|");
			int endIndex = annotValue.indexOf(" ", termIdx);
			if(endIndex == -1){
				endIndex = annotValue.length();
			}
			attr.setValue(annotValue.substring(termIdx + "terminology|".length(), endIndex));
			attr.setType(ITSAnnotAttribute.IRI_STRING_TYPE);
			annotations.add(attr);
		}
		if(annotValue.contains("text-analysis|")){
			ITSAnnotAttribute attr = new ITSAnnotAttribute();
			attr.setName(TA_ANNOTS_REF);
			int termIdx = annotValue.indexOf("text-analysis|");
			int endIndex = annotValue.indexOf(" ", termIdx);
			if(endIndex == -1){
				endIndex = annotValue.length();
			}
			attr.setValue(annotValue.substring(termIdx + "text-analysis|".length(), endIndex));
			attr.setType(ITSAnnotAttribute.IRI_STRING_TYPE);
			annotations.add(attr);
		}
		if(annotValue.contains("mt-confidence|")){
			ITSAnnotAttribute attr = new ITSAnnotAttribute();
			attr.setName(MT_CONF_ANNOTS_REF);
			int termIdx = annotValue.indexOf("mt-confidence|");
			int endIndex = annotValue.indexOf(" ", termIdx);
			if(endIndex == -1){
				endIndex = annotValue.length();
			}
			attr.setValue(annotValue.substring(termIdx + "mt-confidence|".length(), endIndex));
			attr.setType(ITSAnnotAttribute.IRI_STRING_TYPE);
			annotations.add(attr);
		}
		
		return annotations;
	}

	/* ********* Domain ********** */
	public static final String DOMAIN_VALUE = "domains";

	/* ********* External Resource ********** */
	public static final String EXTERNALRES_VALUE = "externalResourceRef";

	/* ********* Language Information ********** */
	// TODO not found  in its-rdf ontology
	public static final String LANG_VALUE = "langValue";
	//

	/* ********* Locale Filter ********** */
	public static final String LOC_FILTER_LIST = "localeFilterList";

	public static final String LOC_FILTER_TYPE = "localeFilterType";

	public static ITSAnnotAttribute[] getITSLocFilterAttributes(
			String filterValue) {

		String filterType = "include";
		String filterList = filterValue;
		if (filterValue.startsWith("!")) {
			filterType = "exclude";
			filterList = filterValue.substring(1);
		}
		ITSAnnotAttribute filtListAttr = new ITSAnnotAttribute(LOC_FILTER_LIST,
				filterList, ITSAnnotAttribute.STRING_TYPE);
		ITSAnnotAttribute filtTypeAttr = new ITSAnnotAttribute(LOC_FILTER_TYPE,
				filterType, ITSAnnotAttribute.STRING_TYPE);
		return new ITSAnnotAttribute[] { filtListAttr, filtTypeAttr };
	}
	
	/* ********* Localization Note ********** */
	public static final String LOCNOTE_TYPE = "locNoteType";
	
	public static final String LOCNOTE_VALUE = "locNote";
	
	public static final String LOCNOTE_VALUE_REF = "locNoteRef";
	
	
	/* ********* Localization Quality Rating ********** */
	public static final String LQR_PROFILEREF = "locQualityRatingProfileRef";
	
	public static final String LQR_SCORE= "locQualityRatingScore";
	
	public static final String LQR_SCORETHRESHOLD = "locQualityRatingScoreThreshold";
	
	public static final String LQR_VOTE = "locQualityRatingVote";
	
	public static final String LQR_VOTETHRESHOLD = "locQualityRatingVoteThreshold";
	
	
	/* ********* Preserve Space ********** */
	public static final String PRESERVEWS_INFO = "space";
	
	
	/* ********* Storage Size ********** */
	public static final String STORAGESIZE_ENCODING = "storageEncoding";
	
	public static final String STORAGESIZE_LINEBREAK = "lineBreakType";
	
	public static final String STORAGESIZE_SIZE = "storageSize";
	
	
	/* ********* Text Analysis ********** */
	public static final String TA_CLASS_REF = "taClassRef";
	
	public static final String TA_CONFIDENCE = "taConfidence";
	
	public static final String TA_IDENT = "taIdent";
	
	public static final String TA_IDENT_REF = "taIdentRef";
	
	public static final String TA_SOURCE = "taSource";
	
	
	/* ********* Terminology ********** */
	public static final String TERM_CONFIDENCE = "termConfidence";
	
	public static final String TERM_INFO = "term";
	
	public static final String TERM_INFO_REF = "termInfoRef";
	
	
	/* ********* Translate ********** */
	public static final String TRANSLATE_VALUE = "translate";

	public static final String TARGET = "target";
	
}
