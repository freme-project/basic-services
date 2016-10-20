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
import java.util.Arrays;
import java.util.List;

import net.sf.okapi.common.annotation.GenericAnnotation;
import net.sf.okapi.common.annotation.GenericAnnotationType;

/**
 * Helper class for ITS annotations management.
 */
public class ITSAnnotationsHelper {

	/**
	 * Gets the ITS annotation corresponding to the given generic annotation.
	 * 
	 * @param annotation
	 *            the generic annotation
	 * @return the ITS annotation
	 */
	public static ITSAnnotation getITSAnnotation(GenericAnnotation annotation) {

		ITSAnnotation itsAnn = new ITSAnnotation();

		switch (annotation.getType()) {
		case GenericAnnotationType.LQI:
			itsAnn = createLQIAnnotation(annotation);
			break;
		case GenericAnnotationType.PROV:
			itsAnn = createProvAnnotation(annotation);
			break;
		case GenericAnnotationType.MTCONFIDENCE:
			itsAnn = createMtConfidenceAnnotation(annotation);
			break;
		case GenericAnnotationType.ALLOWEDCHARS:
			itsAnn = createAllowedCharsAnnotation(annotation);
			break;
		case GenericAnnotationType.ANNOT:
			itsAnn = createAnnotatorAnnotation(annotation);
			break;
		case GenericAnnotationType.DOMAIN:
			itsAnn = createDomainAnnotation(annotation);
			break;
		case GenericAnnotationType.EXTERNALRES:
			itsAnn = createExternalResAnnotation(annotation);
			break;
		case GenericAnnotationType.LANG:
			itsAnn = createLangAnnotation(annotation);
			break;
		case GenericAnnotationType.LOCFILTER:
			itsAnn = createLocFilterAnnotation(annotation);
			break;
		case GenericAnnotationType.LOCNOTE:
			itsAnn = createLocNoteAnnotation(annotation);
			break;
		case GenericAnnotationType.LQR:
			itsAnn = createLQRAnnotation(annotation);
			break;
		case GenericAnnotationType.PRESERVEWS:
			itsAnn = createPreserveAnnotation(annotation);
			break;
		case GenericAnnotationType.STORAGESIZE:
			itsAnn = createStorageSizeAnnotatin(annotation);
			break;
		case GenericAnnotationType.TA:
			itsAnn = createTextAnalysisAnnotation(annotation);
			break;
		case GenericAnnotationType.TERM:
			itsAnn = createTermAnnotation(annotation);
			break;
		case GenericAnnotationType.TRANSLATE:
			itsAnn = createTranslateAnnotation(annotation);
			break;
		default:
			break;
		}

		return itsAnn;
	}

	/**
	 * Creates an annotation from the "Translate" ITS category.
	 * 
	 * @param annotation
	 *            the generic annotation
	 * @return the translate annotation
	 */
	private static ITSAnnotation createTranslateAnnotation(
			GenericAnnotation annotation) {
		ITSAnnotation transAnn = new ITSAnnotation(
				GenericAnnotationType.TRANSLATE);
		List<ITSAnnotAttribute> attrs = new ArrayList<ITSAnnotAttribute>();
		if (annotation.getBoolean(GenericAnnotationType.TRANSLATE_VALUE) != null) {
			ITSAnnotAttribute attr = new ITSAnnotAttribute(
					ItsRdfConstants.TRANSLATE_VALUE,
					annotation
							.getBoolean(GenericAnnotationType.TRANSLATE_VALUE),
					ITSAnnotAttribute.BOOLEAN_TYPE);
			attrs.add(attr);
		}
		transAnn.setAttributes(attrs);
		return transAnn;
	}

	/**
	 * Creates the annotation from the "Terminology" ITS category.
	 * 
	 * @param annotation
	 *            the generic annotation.
	 * @return the terminology annotation
	 */
	private static ITSAnnotation createTermAnnotation(
			GenericAnnotation annotation) {

		ITSAnnotation termAnn = new ITSAnnotation(GenericAnnotationType.TERM);
		List<ITSAnnotAttribute> attrs = new ArrayList<ITSAnnotAttribute>();
		ITSAnnotAttribute attr = null;
		if (annotation.getDouble(GenericAnnotationType.TERM_CONFIDENCE) != null) {
			attr = new ITSAnnotAttribute(
					ItsRdfConstants.TERM_CONFIDENCE,
					annotation.getDouble(GenericAnnotationType.TERM_CONFIDENCE),
					ITSAnnotAttribute.DOUBLE_TYPE);
			attrs.add(attr);
		}
		if (annotation.getString(GenericAnnotationType.TERM_INFO) != null) {
			String value = annotation
					.getString(GenericAnnotationType.TERM_INFO);
			if (value.startsWith(GenericAnnotationType.REF_PREFIX)) {
				attr = new ITSAnnotAttribute(ItsRdfConstants.TERM_INFO_REF,
						getRefString(value), ITSAnnotAttribute.IRI_STRING_TYPE);
			} else {
				attr = new ITSAnnotAttribute(ItsRdfConstants.TERM_INFO, value,
						ITSAnnotAttribute.STRING_TYPE);
			}
			attrs.add(attr);
		} else {
			// TODO check if it's correct: if no term info are set, then set
			// "yes"
			attr = new ITSAnnotAttribute(ItsRdfConstants.TERM_INFO, "yes",
					ITSAnnotAttribute.STRING_TYPE);
			attrs.add(attr);
		}
		termAnn.setAttributes(attrs);
		return termAnn;
	}

	/**
	 * Creates the annotation from the "Text Analysis" ITS category.
	 * 
	 * @param annotation
	 *            the generic annotation
	 * @return the text analysis annotation
	 */
	private static ITSAnnotation createTextAnalysisAnnotation(
			GenericAnnotation annotation) {

		ITSAnnotation taAnn = new ITSAnnotation(GenericAnnotationType.TA);
		List<ITSAnnotAttribute> attrs = new ArrayList<ITSAnnotAttribute>();
		ITSAnnotAttribute attr = null;
		if (annotation.getString(GenericAnnotationType.TA_CLASS) != null) {
			// the annotation in Okapi has the following form:
			// taClass=REF:http://nerd.eurecom.fr/ontology#Place
			// while in xliff it is
			// its:taClassRef="http://nerd.eurecom.fr/ontology#Place"
			String value = annotation.getString(GenericAnnotationType.TA_CLASS);
			attr = new ITSAnnotAttribute(ItsRdfConstants.TA_CLASS_REF,
					getRefString(value), ITSAnnotAttribute.IRI_STRING_TYPE);
			attrs.add(attr);
		}
		if (annotation.getDouble(GenericAnnotationType.TA_CONFIDENCE) != null) {
			attr = new ITSAnnotAttribute(ItsRdfConstants.TA_CONFIDENCE,
					annotation.getDouble(GenericAnnotationType.TA_CONFIDENCE),
					ITSAnnotAttribute.DOUBLE_TYPE);
			attrs.add(attr);
		}
		if (annotation.getString(GenericAnnotationType.TA_IDENT) != null) {
			// the annotation in Okapi has the following form
			// taIdent=REF:http://dbpedia.org/resource/Arizona
			// while in xliff it is
			// its:taIdentRef="http://dbpedia.org/resource/Arizona"
			String value = annotation.getString(GenericAnnotationType.TA_IDENT);
			if (value.startsWith(GenericAnnotationType.REF_PREFIX)) {
				attr = new ITSAnnotAttribute(ItsRdfConstants.TA_IDENT_REF,
						getRefString(value), ITSAnnotAttribute.IRI_STRING_TYPE);
			} else {
				attr = new ITSAnnotAttribute(ItsRdfConstants.TA_IDENT, value,
						ITSAnnotAttribute.STRING_TYPE);
			}
			attrs.add(attr);
		}
		if (annotation.getString(GenericAnnotationType.TA_SOURCE) != null) {
			attr = new ITSAnnotAttribute(ItsRdfConstants.TA_SOURCE,
					annotation.getString(GenericAnnotationType.TA_SOURCE),
					ITSAnnotAttribute.STRING_TYPE);
			attrs.add(attr);
		}
		taAnn.setAttributes(attrs);
		return taAnn;
	}

	/**
	 * Creates the annotation from "Storage Size" ITS category.
	 * 
	 * @param annotation
	 *            the generic annotation
	 * @return the storage size annotation
	 */
	private static ITSAnnotation createStorageSizeAnnotatin(
			GenericAnnotation annotation) {

		ITSAnnotation storageAnn = new ITSAnnotation(
				GenericAnnotationType.STORAGESIZE);
		List<ITSAnnotAttribute> attrs = new ArrayList<ITSAnnotAttribute>();
		ITSAnnotAttribute attr = null;
		if (annotation.getString(GenericAnnotationType.STORAGESIZE_ENCODING) != null) {
			attr = new ITSAnnotAttribute(
					ItsRdfConstants.STORAGESIZE_ENCODING,
					annotation
							.getString(GenericAnnotationType.STORAGESIZE_ENCODING),
					ITSAnnotAttribute.STRING_TYPE);
			attrs.add(attr);
		}
		if (annotation.getString(GenericAnnotationType.STORAGESIZE_LINEBREAK) != null) {
			attr = new ITSAnnotAttribute(
					ItsRdfConstants.STORAGESIZE_LINEBREAK,
					annotation
							.getString(GenericAnnotationType.STORAGESIZE_LINEBREAK),
					ITSAnnotAttribute.STRING_TYPE);
			attrs.add(attr);
		}
		if (annotation.getInteger(GenericAnnotationType.STORAGESIZE_SIZE) != null) {
			attr = new ITSAnnotAttribute(
					ItsRdfConstants.STORAGESIZE_SIZE,
					annotation
							.getInteger(GenericAnnotationType.STORAGESIZE_SIZE),
					ITSAnnotAttribute.UNISGNED_INTEGER_TYPE);
			attrs.add(attr);
		}
		storageAnn.setAttributes(attrs);
		return storageAnn;
	}

	/**
	 * Creates the annotation from the "Preserve" ITS category.
	 * 
	 * @param annotation
	 *            the generic annotation
	 * @return the preserve annotation
	 */
	private static ITSAnnotation createPreserveAnnotation(
			GenericAnnotation annotation) {

		ITSAnnotation presAnn = new ITSAnnotation(
				GenericAnnotationType.PRESERVEWS);
		List<ITSAnnotAttribute> attrs = new ArrayList<ITSAnnotAttribute>();
		if (annotation.getString(GenericAnnotationType.PRESERVEWS_INFO) != null) {
			ITSAnnotAttribute attr = new ITSAnnotAttribute(
					ItsRdfConstants.PRESERVEWS_INFO,
					annotation.getString(GenericAnnotationType.PRESERVEWS_INFO),
					ITSAnnotAttribute.STRING_TYPE);
			attrs.add(attr);
		}
		presAnn.setAttributes(attrs);
		return presAnn;
	}

	/**
	 * Creates the annotation from the "Localization Quality Rating" ITS
	 * category.
	 * 
	 * @param annotation
	 *            the generic annotation
	 * @return the LQR annotation
	 */
	private static ITSAnnotation createLQRAnnotation(
			GenericAnnotation annotation) {

		ITSAnnotation lqrAnn = new ITSAnnotation(GenericAnnotationType.LQR);
		List<ITSAnnotAttribute> attrs = new ArrayList<ITSAnnotAttribute>();
		ITSAnnotAttribute attr = null;
		if (annotation.getString(GenericAnnotationType.LQR_PROFILEREF) != null) {

			attr = new ITSAnnotAttribute(ItsRdfConstants.LQR_PROFILEREF,
					annotation.getString(GenericAnnotationType.LQR_PROFILEREF),
					ITSAnnotAttribute.IRI_STRING_TYPE);
			attrs.add(attr);
		}
		if (annotation.getDouble(GenericAnnotationType.LQR_SCORE) != null) {

			attr = new ITSAnnotAttribute(ItsRdfConstants.LQR_SCORE,
					annotation.getDouble(GenericAnnotationType.LQR_SCORE),
					ITSAnnotAttribute.DOUBLE_TYPE);
			attrs.add(attr);
		}
		if (annotation.getDouble(GenericAnnotationType.LQR_SCORETHRESHOLD) != null) {

			attr = new ITSAnnotAttribute(
					ItsRdfConstants.LQR_SCORETHRESHOLD,
					annotation
							.getDouble(GenericAnnotationType.LQR_SCORETHRESHOLD),
					ITSAnnotAttribute.DOUBLE_TYPE);
			attrs.add(attr);
		}
		if (annotation.getInteger(GenericAnnotationType.LQR_VOTE) != null) {

			attr = new ITSAnnotAttribute(ItsRdfConstants.LQR_VOTE,
					annotation.getInteger(GenericAnnotationType.LQR_VOTE),
					ITSAnnotAttribute.INTEGER_TYPE);
			attrs.add(attr);
		}
		if (annotation.getInteger(GenericAnnotationType.LQR_VOTETHRESHOLD) != null) {

			attr = new ITSAnnotAttribute(
					ItsRdfConstants.LQR_VOTETHRESHOLD,
					annotation
							.getInteger(GenericAnnotationType.LQR_VOTETHRESHOLD),
					ITSAnnotAttribute.INTEGER_TYPE);
			attrs.add(attr);
		}
		lqrAnn.setAttributes(attrs);
		return lqrAnn;
	}

	/**
	 * Creates the annotation from the "Localization Note" ITS category.
	 * 
	 * @param annotation
	 *            the generic annotation
	 * @return the localization note annotation
	 */
	private static ITSAnnotation createLocNoteAnnotation(
			GenericAnnotation annotation) {

		ITSAnnotation locNoteAnn = new ITSAnnotation(
				GenericAnnotationType.LOCNOTE);
		List<ITSAnnotAttribute> attrs = new ArrayList<ITSAnnotAttribute>();
		ITSAnnotAttribute attr = null;
		if (annotation.getString(GenericAnnotationType.LOCNOTE_TYPE) != null) {
			attr = new ITSAnnotAttribute(ItsRdfConstants.LOCNOTE_TYPE,
					annotation.getString(GenericAnnotationType.LOCNOTE_TYPE),
					ITSAnnotAttribute.STRING_TYPE);
			attrs.add(attr);
		}
		if (annotation.getString(GenericAnnotationType.LOCNOTE_VALUE) != null) {
			String value = annotation
					.getString(GenericAnnotationType.LOCNOTE_VALUE);
			if (value.startsWith(GenericAnnotationType.REF_PREFIX)) {

				attr = new ITSAnnotAttribute(ItsRdfConstants.LOCNOTE_VALUE_REF,
						getRefString(value), ITSAnnotAttribute.IRI_STRING_TYPE);
			} else {
				attr = new ITSAnnotAttribute(ItsRdfConstants.LOCNOTE_VALUE,
						value, ITSAnnotAttribute.STRING_TYPE);
			}
			attrs.add(attr);
		}
		locNoteAnn.setAttributes(attrs);
		return locNoteAnn;
	}

	/**
	 * Creates the annotation from the "Locale Filter" ITS category.
	 * 
	 * @param annotation
	 *            the generic annotation
	 * @return the Locale Filter annotation
	 */
	private static ITSAnnotation createLocFilterAnnotation(
			GenericAnnotation annotation) {

		ITSAnnotation locFilterAnn = new ITSAnnotation(
				GenericAnnotationType.LOCFILTER);
		List<ITSAnnotAttribute> attrs = new ArrayList<ITSAnnotAttribute>();
		if (annotation.getString(GenericAnnotationType.LOCFILTER_VALUE) != null) {
			attrs.addAll(Arrays.asList(ItsRdfConstants
					.getITSLocFilterAttributes(annotation
							.getString(GenericAnnotationType.LOCFILTER_VALUE))));
			// ITSAnnotAttribute attr = new ITSAnnotAttribute(
			// GenericAnnotationType.LOCFILTER_VALUE,
			// annotation.getString(GenericAnnotationType.LOCFILTER_VALUE),
			// ITSAnnotAttribute.STRING_TYPE);
			// attrs.add(attr);
		}
		locFilterAnn.setAttributes(attrs);
		return locFilterAnn;
	}

	/**
	 * Creates the annotation from the "Language Information" ITS category.
	 * 
	 * @param annotation
	 *            the generic annotation
	 * @return the language annotation
	 */
	private static ITSAnnotation createLangAnnotation(
			GenericAnnotation annotation) {

		ITSAnnotation langAnn = new ITSAnnotation(GenericAnnotationType.LANG);
		List<ITSAnnotAttribute> attrs = new ArrayList<ITSAnnotAttribute>();
		// TODO not found issues ref in its-rdf ontology
		if (annotation.getString(GenericAnnotationType.LANG_VALUE) != null) {
			ITSAnnotAttribute attr = new ITSAnnotAttribute(
					ItsRdfConstants.LANG_VALUE,
					annotation.getString(GenericAnnotationType.LANG_VALUE),
					ITSAnnotAttribute.STRING_TYPE);
			attrs.add(attr);
		}
		//
		langAnn.setAttributes(attrs);
		return langAnn;

	}

	/**
	 * Creates the annotation from the "External Resource" ITS category.
	 * 
	 * @param annotation
	 *            the generic annotation.
	 * @return the external resource annotation
	 */
	private static ITSAnnotation createExternalResAnnotation(
			GenericAnnotation annotation) {

		ITSAnnotation extResAnn = new ITSAnnotation(
				GenericAnnotationType.EXTERNALRES);
		List<ITSAnnotAttribute> attrs = new ArrayList<ITSAnnotAttribute>();
		if (annotation.getString(GenericAnnotationType.EXTERNALRES_VALUE) != null) {
			ITSAnnotAttribute attr = new ITSAnnotAttribute(
					ItsRdfConstants.EXTERNALRES_VALUE,
					annotation
							.getString(GenericAnnotationType.EXTERNALRES_VALUE),
					ITSAnnotAttribute.IRI_STRING_TYPE);
			attrs.add(attr);
		}
		extResAnn.setAttributes(attrs);
		return extResAnn;

	}

	/**
	 * Creates the annotation from the "Domain" ITS category.
	 * 
	 * @param annotation
	 *            the generic annotation
	 * @return the domain annotation
	 */
	private static ITSAnnotation createDomainAnnotation(
			GenericAnnotation annotation) {

		ITSAnnotation domainAnn = new ITSAnnotation(
				GenericAnnotationType.DOMAIN);
		List<ITSAnnotAttribute> attrs = new ArrayList<ITSAnnotAttribute>();
		if (annotation.getString(GenericAnnotationType.DOMAIN_VALUE) != null) {
			ITSAnnotAttribute attr = new ITSAnnotAttribute(
					ItsRdfConstants.DOMAIN_VALUE,
					annotation.getString(GenericAnnotationType.DOMAIN_VALUE),
					ITSAnnotAttribute.STRING_TYPE);
			attrs.add(attr);
		}
		domainAnn.setAttributes(attrs);
		return domainAnn;
	}

	/**
	 * Creates the annotator reference implementing the ITS Tools Annotation
	 * mechanism.
	 * 
	 * @param annotation
	 *            the generic annotation
	 * @return the annotator reference.
	 */
	private static ITSAnnotation createAnnotatorAnnotation(
			GenericAnnotation annotation) {

		ITSAnnotation annotAnn = new ITSAnnotation(GenericAnnotationType.ANNOT);
		List<ITSAnnotAttribute> attrs = new ArrayList<ITSAnnotAttribute>();
		ITSAnnotAttribute attr = null;

		if (annotation.getString(GenericAnnotationType.ANNOT_VALUE) != null) {

			// attr = new ITSAnnotAttribute(
			// ItsRdfConstants.getAnnotatorsRef(annotation
			// .getString(GenericAnnotationType.ANNOT_VALUE)),
			// annotation.getString(GenericAnnotationType.ANNOT_VALUE),
			// ITSAnnotAttribute.STRING_TYPE);
			// attrs.add(attr);
			attrs.addAll(ItsRdfConstants.getIstAnnotatorsRef(annotation
					.getString(GenericAnnotationType.ANNOT_VALUE)));
		}
		// TODO not found issues ref in its-rdf ontology
		if (annotation.getString(GenericAnnotationType.ANNOTATORREF) != null) {
			attr = new ITSAnnotAttribute(ItsRdfConstants.ANNOTATORREF,
					annotation.getString(GenericAnnotationType.ANNOTATORREF),
					ITSAnnotAttribute.IRI_STRING_TYPE);
			attrs.add(attr);
		}
		//
		annotAnn.setAttributes(attrs);
		return annotAnn;
	}

	/**
	 * Creates the annotation from the "Allowed Characters" ITS category.
	 * 
	 * @param annotation
	 *            the generic annotation
	 * @return the allowed chars annotation.
	 */
	private static ITSAnnotation createAllowedCharsAnnotation(
			GenericAnnotation annotation) {

		ITSAnnotation allowedCharsAnn = new ITSAnnotation(
				GenericAnnotationType.ALLOWEDCHARS);
		List<ITSAnnotAttribute> attrs = new ArrayList<ITSAnnotAttribute>();
		if (annotation.getString(GenericAnnotationType.ALLOWEDCHARS_VALUE) != null) {
			ITSAnnotAttribute attr = new ITSAnnotAttribute(
					ItsRdfConstants.ALLOWEDCHARS_VALUE,
					annotation
							.getString(GenericAnnotationType.ALLOWEDCHARS_VALUE),
					ITSAnnotAttribute.STRING_TYPE);
			attrs.add(attr);
		}
		allowedCharsAnn.setAttributes(attrs);
		return allowedCharsAnn;
	}

	/**
	 * Creates the annotation from the "MT Confidence" ITS category.
	 * 
	 * @param annotation
	 *            the generic annotation
	 * @return the MT confidence annotation
	 */
	private static ITSAnnotation createMtConfidenceAnnotation(
			GenericAnnotation annotation) {

		ITSAnnotation mtAnnot = new ITSAnnotation(
				GenericAnnotationType.MTCONFIDENCE);
		List<ITSAnnotAttribute> attrs = new ArrayList<ITSAnnotAttribute>();
		if (annotation.getDouble(GenericAnnotationType.MTCONFIDENCE_VALUE) != null) {
			ITSAnnotAttribute attr = new ITSAnnotAttribute(
					ItsRdfConstants.MTCONFIDENCE_VALUE,
					annotation
							.getDouble(GenericAnnotationType.MTCONFIDENCE_VALUE),
					ITSAnnotAttribute.DOUBLE_TYPE);
			attrs.add(attr);
		}
		if (annotation.getString(GenericAnnotationType.ANNOTATORREF) != null) {
			ITSAnnotAttribute attr = new ITSAnnotAttribute(
					ItsRdfConstants.MT_CONF_ANNOTS_REF,
					annotation.getString(GenericAnnotationType.ANNOTATORREF),
					ITSAnnotAttribute.IRI_STRING_TYPE);
			attrs.add(attr);
		}
		mtAnnot.setAttributes(attrs);
		return mtAnnot;
	}

	/**
	 * Creates the annotation from the "Provenance" ITS category
	 * 
	 * @param annotation
	 *            the generic annotation
	 * @return the provenance annotation
	 */
	private static ITSAnnotation createProvAnnotation(
			GenericAnnotation annotation) {

		ITSAnnotation lqiAnn = new ITSAnnotation(GenericAnnotationType.PROV);
		List<ITSAnnotAttribute> attributes = new ArrayList<ITSAnnotAttribute>();
		ITSAnnotAttribute attr = null;
		if (annotation.getString(GenericAnnotationType.PROV_ORG) != null) {
			String value = annotation.getString(GenericAnnotationType.PROV_ORG);
			if (value.startsWith(GenericAnnotationType.REF_PREFIX)) {
				attr = new ITSAnnotAttribute(ItsRdfConstants.PROV_ORG_REF,
						getRefString(value), ITSAnnotAttribute.IRI_STRING_TYPE);
			} else {
				attr = new ITSAnnotAttribute(ItsRdfConstants.PROV_ORG, value,
						ITSAnnotAttribute.STRING_TYPE);
			}
			attributes.add(attr);
		}
		if (annotation.getString(GenericAnnotationType.PROV_PERSON) != null) {
			String value = annotation
					.getString(GenericAnnotationType.PROV_PERSON);
			if (value.startsWith(GenericAnnotationType.REF_PREFIX)) {
				attr = new ITSAnnotAttribute(ItsRdfConstants.PROV_PERSON_REF,
						getRefString(value), ITSAnnotAttribute.IRI_STRING_TYPE);
			} else {
				attr = new ITSAnnotAttribute(ItsRdfConstants.PROV_PERSON,
						value, ITSAnnotAttribute.STRING_TYPE);
			}
			attributes.add(attr);
		}
		if (annotation.getString(GenericAnnotationType.PROV_PROVREF) != null) {
			attr = new ITSAnnotAttribute(ItsRdfConstants.PROV_PROVREF,
					annotation.getString(GenericAnnotationType.PROV_PROVREF),
					ITSAnnotAttribute.IRI_STRING_TYPE);
			attributes.add(attr);
		}

		// TODO not found issues ref in its-rdf ontology
		if (annotation.getString(GenericAnnotationType.PROV_RECSREF) != null) {
			attr = new ITSAnnotAttribute(ItsRdfConstants.PROV_RECSREF,
					annotation.getString(GenericAnnotationType.PROV_RECSREF),
					ITSAnnotAttribute.IRI_STRING_TYPE);
			attributes.add(attr);
		}
		//
		if (annotation.getString(GenericAnnotationType.PROV_REVORG) != null) {
			String value = annotation
					.getString(GenericAnnotationType.PROV_REVORG);
			if (value.startsWith(GenericAnnotationType.REF_PREFIX)) {
				attr = new ITSAnnotAttribute(ItsRdfConstants.PROV_REVORG_REF,
						getRefString(value), ITSAnnotAttribute.IRI_STRING_TYPE);
			} else {
				attr = new ITSAnnotAttribute(ItsRdfConstants.PROV_REVORG,
						value, ITSAnnotAttribute.STRING_TYPE);
			}
			attributes.add(attr);
		}
		if (annotation.getString(GenericAnnotationType.PROV_REVPERSON) != null) {

			String value = annotation
					.getString(GenericAnnotationType.PROV_REVPERSON);
			if (value.startsWith(GenericAnnotationType.REF_PREFIX)) {
				attr = new ITSAnnotAttribute(
						ItsRdfConstants.PROV_REVPERSON_REF,
						getRefString(value), ITSAnnotAttribute.IRI_STRING_TYPE);
			} else {
				attr = new ITSAnnotAttribute(ItsRdfConstants.PROV_REVPERSON,
						value, ITSAnnotAttribute.STRING_TYPE);
			}
			attributes.add(attr);
		}

		if (annotation.getString(GenericAnnotationType.PROV_REVTOOL) != null) {
			String revToolValue = annotation
					.getString(GenericAnnotationType.PROV_REVTOOL);
			if (revToolValue.startsWith(GenericAnnotationType.REF_PREFIX)) {
				attr = new ITSAnnotAttribute(ItsRdfConstants.PROV_REVTOOL_REF,
						getRefString(revToolValue),
						ITSAnnotAttribute.IRI_STRING_TYPE);
			} else {
				attr = new ITSAnnotAttribute(ItsRdfConstants.PROV_REVTOOL,
						revToolValue, ITSAnnotAttribute.STRING_TYPE);
			}
			attributes.add(attr);
		}
		if (annotation.getString(GenericAnnotationType.PROV_TOOL) != null) {
			String value = annotation
					.getString(GenericAnnotationType.PROV_TOOL);
			if (value.startsWith(GenericAnnotationType.REF_PREFIX)) {
				attr = new ITSAnnotAttribute(ItsRdfConstants.PROV_TOOL_REF,
						getRefString(value), ITSAnnotAttribute.IRI_STRING_TYPE);
			} else {
				attr = new ITSAnnotAttribute(ItsRdfConstants.PROV_TOOL, value,
						ITSAnnotAttribute.STRING_TYPE);
			}
			attributes.add(attr);
		}
		lqiAnn.setAttributes(attributes);
		return lqiAnn;
	}

	/**
	 * Creates the annotation from the "Localization Quality Issue" ITS
	 * category.
	 * 
	 * @param annotation
	 *            the generic annotation
	 * @return the LQI annotation
	 */
	private static ITSAnnotation createLQIAnnotation(
			GenericAnnotation annotation) {

		ITSAnnotation lqiAnn = new ITSAnnotation(GenericAnnotationType.LQI);
		List<ITSAnnotAttribute> attributes = new ArrayList<ITSAnnotAttribute>();
		ITSAnnotAttribute attr = null;
		if (annotation.getString(GenericAnnotationType.LQI_COMMENT) != null) {
			attr = new ITSAnnotAttribute(ItsRdfConstants.LQI_COMMENT,
					annotation.getString(GenericAnnotationType.LQI_COMMENT),
					ITSAnnotAttribute.STRING_TYPE);
			attributes.add(attr);
		}
		if (annotation.getBoolean(GenericAnnotationType.LQI_ENABLED) != null) {
			attr = new ITSAnnotAttribute(ItsRdfConstants.LQI_ENABLED,
					annotation.getBoolean(GenericAnnotationType.LQI_ENABLED),
					ITSAnnotAttribute.BOOLEAN_TYPE);
			attributes.add(attr);
		}
		// TODO not found issues ref in its-rdf ontology
		if (annotation.getString(GenericAnnotationType.LQI_ISSUESREF) != null) {
			attr = new ITSAnnotAttribute(ItsRdfConstants.LQI_ISSUESREF,
					annotation.getString(GenericAnnotationType.LQI_ISSUESREF),
					ITSAnnotAttribute.IRI_STRING_TYPE);
			attributes.add(attr);
		}
		//
		if (annotation.getString(GenericAnnotationType.LQI_PROFILEREF) != null) {
			attr = new ITSAnnotAttribute(ItsRdfConstants.LQI_PROFILEREF,
					annotation.getString(GenericAnnotationType.LQI_PROFILEREF),
					ITSAnnotAttribute.IRI_STRING_TYPE);
			attributes.add(attr);
		}
		if (annotation.getDouble(GenericAnnotationType.LQI_SEVERITY) != null) {
			attr = new ITSAnnotAttribute(ItsRdfConstants.LQI_SEVERITY,
					annotation.getDouble(GenericAnnotationType.LQI_SEVERITY),
					ITSAnnotAttribute.DOUBLE_TYPE);
			attributes.add(attr);
		}
		if (annotation.getString(GenericAnnotationType.LQI_TYPE) != null) {
			attr = new ITSAnnotAttribute(ItsRdfConstants.LQI_TYPE,
					annotation.getString(GenericAnnotationType.LQI_TYPE),
					ITSAnnotAttribute.STRING_TYPE);
			attributes.add(attr);
		}
		lqiAnn.setAttributes(attributes);
		return lqiAnn;
	}

	/**
	 * Gets the reference string depending on the reference value passed as
	 * parameter.
	 * 
	 * @param refValue
	 *            the reference value
	 * @return the reference string
	 */
	private static String getRefString(String refValue) {

		String retString = refValue;
		int refIdx = refValue.indexOf(GenericAnnotationType.REF_PREFIX);
		if (refIdx != -1) {
			retString = refValue.substring(refIdx
					+ GenericAnnotationType.REF_PREFIX.length());
		}
		return retString;
	}
}
