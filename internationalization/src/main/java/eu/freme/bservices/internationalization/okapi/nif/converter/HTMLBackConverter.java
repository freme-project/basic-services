/**
 * Copyright (C) 2015 Deutsches Forschungszentrum für Künstliche Intelligenz (http://freme-project.eu)
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
package eu.freme.bservices.internationalization.okapi.nif.converter;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

import eu.freme.bservices.internationalization.okapi.nif.filter.RDFConstants;
import eu.freme.bservices.internationalization.okapi.nif.its.ItsRdfConstants;

/**
 * Converts a NIF file back to the original format, when the NIF file was
 * created by converting a HTML file. If the NIF file has been enriched, then
 * the enrichments are added to the original file as well. Note that in order to
 * perform the back conversion, the skeleton NIF file is needed in addition to
 * the enriched NIF file.
 * 
 */
public class HTMLBackConverter {

	/** The URI offset string prefix. */
	public static final String URI_OFFSET_PREFIX = "#char=";

	public static final String HTML_CLASS_REF = "its-ta-class-ref";

	public static final String HTML_DATA_CLASS_REF = "data-its-ta-class-refs";

	/** The triple model. */
	private Model model;
	
	private Map<String,String> annotationsMap;

	/**
	 * Default constructor.
	 */
	public HTMLBackConverter() {

	}

	public InputStream convertBack(final InputStream skeletonFile,
			final InputStream enrichedFile) {

		return convertBack(skeletonFile, enrichedFile,
				RDFConstants.RDFSerialization.TURTLE.toRDFLang(),
				RDFConstants.RDFSerialization.TURTLE.toRDFLang());
	}

	/**
	 * Performs the back conversion.
	 * 
	 * @param skeletonFile
	 *            the skeleton NIF file.
	 * @param enrichedFile
	 *            the enriched NIF file.
	 * @param skeletonFormat
	 *            the skeleton file serialization format.
	 * @param enrichedFormat
	 *            the enriched file serialization format.
	 * @return the input stream being the original HTML file.
	 */
	public InputStream convertBack(final InputStream skeletonFile,
			final InputStream enrichedFile, final String skeletonFormat,
			final String enrichedFormat) {

		Model skeletonModel = ModelFactory.createDefaultModel();
		CharsetDecoder decoder = Charset.forName("UTF-8").newDecoder();
		decoder.onMalformedInput(CodingErrorAction.IGNORE);
		InputStreamReader reader = new InputStreamReader(skeletonFile, decoder);
		skeletonModel.read(reader, null, skeletonFormat);
		Model enrichedModel = ModelFactory.createDefaultModel();
		enrichedModel.read(enrichedFile, null, enrichedFormat);
		model = skeletonModel.union(enrichedModel);
		String originalFile = convertBack();
		return new ByteArrayInputStream(originalFile.getBytes());
	}
	
	/**
	 * @param tuResources the list of text unit resources
	 * @param source the text string to be checked
	 * @return true if source is a text string enriched by the e-service
	 */
	private boolean isEnrichedText(List<TextUnitResource> tuResources, String source){
		
		String tbc = source.replace("\n", "");
		List<String> enrichedTexts = new ArrayList<String>();
		
		for(TextUnitResource tur:tuResources){
			if(!findEnrichmentStatements(tur).isEmpty()){
				enrichedTexts.add(tur.getText().replace("\n", ""));
			}
		}
		
		return enrichedTexts.contains(tbc);
		
	}
	
	/**
	 * @param tuResources the list of test unit resources
	 * @param source a string that may or may not contain enriched text
	 * @return the enriched text contained in source or an empty string otherwise
	 */
	private String getEnrichedText(List<TextUnitResource> tuResources, String source){
		
		String tbc = source.replace("\n", "");
		List<String> enrichedTexts = new ArrayList<String>();
		
		for(TextUnitResource tur:tuResources){
			if(!findEnrichmentStatements(tur).isEmpty()){
				enrichedTexts.add(tur.getText().replace("\n", ""));
			}
		}
		for(String enrichedText:enrichedTexts){
			if (tbc.contains(enrichedText)){
				return enrichedText;
			}
		}
		return "";
		
	}
	
	/**
	 * @param tuResources the list of text unit resources
	 * @return the mapping between text units of enrichment and the resulting enriched html
	 */
	private Map<String,String> buildAnnotationsMapForTextUnitRes(List<TextUnitResource> tuResources) {
		
		//This map contains has text unit text as key and <span ...>text unit text</span> as value
		annotationsMap = new HashMap<String,String>();
		for(TextUnitResource tur: tuResources){
			List<Statement> entityStmts = findEnrichmentStatements(tur);
			if(!entityStmts.isEmpty()){
				StringBuilder annotatedText = new StringBuilder();
				annotatedText.append("<span");
				annotatedText.append(buildAnnotsAttributesString(entityStmts, null));
				annotatedText.append(">");
				annotatedText.append(tur.getText().replace("\n", ""));
				annotatedText.append("</span>");
				String key = tur.getText().replace("\n", "");
				annotationsMap.put(key, annotatedText.toString());
			}
		}
		
		return annotationsMap;
	}

	/**
	 * Performs the back conversion.
	 * 
	 * @return the original file string
	 */
	private String convertBack() {

		StringBuilder originalFileString = new StringBuilder();
		String skeletonContext = findSkeletonContextString();
		
		if (skeletonContext != null) {
			int skeletonLastIdx = 0;
			List<TextUnitResource> tuResources = listTextUnitResources();
			buildAnnotationsMapForTextUnitRes(tuResources);
			for (TextUnitResource tuRes : tuResources) {
				if (checkWasConvertedFromAndSetOffset(tuRes)) {
					List<Statement> enrichmentStmts = findEnrichmentStatements(tuRes);
					if (!enrichmentStmts.isEmpty()) {
						// THE ANNOTATION MATCHES THE TEXT NODE
						String parentNode = skeletonContext
								.substring(skeletonLastIdx,
										tuRes.getWasConvFromStartIdx());
						if (parentNode.isEmpty()) {
							originalFileString.append("<span");
							originalFileString
									.append(buildAnnotsAttributesString(
											enrichmentStmts, parentNode));
							originalFileString.append(">");
							originalFileString.append(tuRes.getText());
							skeletonLastIdx = tuRes.getWasConvFromEndIdx();
							originalFileString.append("</span>");
						} else {
							String annotationAttributes = buildAnnotsAttributesString(
									enrichmentStmts, parentNode);
							int closeTagIdx = parentNode.lastIndexOf(">");
							originalFileString.append(parentNode.substring(0,
									closeTagIdx));
							originalFileString.append(annotationAttributes);
							originalFileString.append(">");
							originalFileString.append(tuRes.getText());
							skeletonLastIdx = tuRes.getWasConvFromEndIdx();

						}

					} else {//enrichmentStmts is empty
						
						if(skeletonLastIdx <= tuRes.getWasConvFromStartIdx()){
							originalFileString
							.append(skeletonContext.substring(skeletonLastIdx,
									tuRes.getWasConvFromStartIdx()));
							skeletonLastIdx = tuRes.getWasConvFromEndIdx();
						} else {
							originalFileString
							.append(skeletonContext.substring(tuRes.getWasConvFromStartIdx(),
										 skeletonLastIdx));
						}

						//Skip writing text of text unit resource when it is enriched text
						//because it should be done differently
						if(!isEnrichedText(tuResources, tuRes.getText())){
							// It is not enriched text but could contain enriched text
							// If the text unit contains enriched text we write without enriched text 
							// but only if there is no <span ...>enrichedText</span>
	                        Pattern pattern = Pattern.compile("<span(.+)</span>");
                            Matcher matcher = pattern.matcher(tuRes.getText());
                            if(matcher.find()){
                            	int fromIndex = tuResources.indexOf(tuRes) + 1;
                            	List<TextUnitResource> fromTuResources = tuResources.subList(fromIndex, tuResources.size());
                            	String enrichedText = getEnrichedText(fromTuResources,tuRes.getText());
                            	if(enrichedText != null && !enrichedText.equals("")){
                            		String key = enrichedText;
                            		String replacement = annotationsMap.get(key);
                            		if(!originalFileString.toString().contains(replacement)){
                            			originalFileString.append(tuRes.getText().replace(enrichedText, replacement));
                            		}
                            	} else {
                            		originalFileString.append(tuRes.getText());
                            	}
                            	
                            } else {
                            	originalFileString.append(tuRes.getText().replace(getEnrichedText(tuResources, tuRes.getText()), ""));
                            }
						} 
						
					}
				} else {
					// no was converted from
					List<Statement> entityStmts = findEnrichmentStatements(tuRes);
					if (!entityStmts.isEmpty()) {
						putAnnotationInTextUnitRes(tuRes, tuResources,
								entityStmts);// Here we build <span ...>enrichedText<span>
					} else {
						String regex = "<span(.+)>";
						String tbc = tuRes.getText().replace("</span>", "").replaceAll(regex, "").replace("\n", "");
						if(isEnrichedText(tuResources,tbc)){
							originalFileString.append(tuRes.getText());
						} else {
							//It is not enriched text but it could contain enriched text and we need to write 
							//only the part with the span element.
							String enrichedText = getEnrichedText(tuResources, tuRes.getText());
							if(enrichedText != null && !enrichedText.equals("")){
								Pattern pattern = Pattern.compile("<span(.+)</span>");
								Matcher matcher = pattern.matcher(tuRes.getText());
								if(matcher.find()){
									String matching = matcher.group();
									if(!originalFileString.toString().contains(matching)){
										originalFileString.append(matching);
									}
									
								}
							}
						}
					}
				}
			}

			
			if (skeletonLastIdx < skeletonContext.length() - 1) {
				originalFileString.append(skeletonContext
						.substring(skeletonLastIdx));
			}
		}
		return originalFileString.toString();
	}

	/**
	 * Puts enrichment annotations into a text unit resource.
	 * 
	 * @param tuResource
	 *            the text unit resource.
	 * @param tuResList
	 *            the list of text unit resources.
	 * @param enrichmentStmts
	 *            the entity statements.
	 */
	private void putAnnotationInTextUnitRes(TextUnitResource tuResource,
			List<TextUnitResource> tuResList, List<Statement> enrichmentStmts) {

		int index = tuResList.indexOf(tuResource) + 1;
		boolean found = false;
		TextUnitResource currRes = null;
		while (index < tuResList.size() && !found) {
			currRes = tuResList.get(index);
			if (currRes.getStartIdx() <= tuResource.getStartIdx()
					&& currRes.getEndIdx() >= tuResource.getEndIdx()
					&& currRes.getText().contains(tuResource.getText())) {
				found = true;
			}
			index++;
		}
		if (found) {
			StringBuilder annotatedText = new StringBuilder();
			annotatedText.append("<span");
			annotatedText.append(buildAnnotsAttributesString(enrichmentStmts,
					null));
			annotatedText.append(">");
			StringBuilder newText = new StringBuilder();
//			newText.append(currRes.getText().substring(
//					0,tuResource.getStartIdx() - currRes.getStartIdx()+ currRes.getAdditionalOffset()));
			
			String annotationText = tuResource.getText();
			int annotationTextPosition = currRes.getText().indexOf(annotationText);
			newText.append(currRes.getText().substring(0,annotationTextPosition));
			
			newText.append(annotatedText);
			newText.append(annotationText);
			newText.append("</span>");
//			newText.append(currRes.getText().substring(
//					tuResource.getEndIdx() - currRes.getStartIdx()+ currRes.getAdditionalOffset()));
			newText.append(currRes.getText().substring(annotationTextPosition + annotationText.length()));
			currRes.setText(newText.toString());
			currRes.setAdditionalOffset(currRes.getAdditionalOffset()
					+ annotatedText.length() + "</span>".length());
		}

	}

	/**
	 * Finds all the enrichment statements associated to a specific resource.
	 * 
	 * @param resource
	 *            the resource
	 * @return the list of enrichment statements.
	 */
	private List<Statement> findEnrichmentStatements(TextUnitResource resource) {

		List<Statement> enrichedStmts = new ArrayList<Statement>();
		enrichedStmts.addAll(findEntityStmts(resource));
		enrichedStmts.addAll(findTermStmts(resource));
		return enrichedStmts;
	}

	/**
	 * Finds all the terminology statements associated to a specific resource
	 * 
	 * @param resource
	 *            the resource
	 * @return the list of terminology statements.
	 */
	private List<Statement> findTermStmts(TextUnitResource resource) {

		List<Statement> termStmts = new ArrayList<Statement>();
		List<Statement> termInfoRefStmts = new ArrayList<Statement>();
		
		Property termProp = model.createProperty(RDFConstants.itsrdfPrefix,
				ItsRdfConstants.TERM_INFO);
		Property termInfoRefProp = model.createProperty(RDFConstants.itsrdfPrefix,
				ItsRdfConstants.TERM_INFO_REF);
		
		StmtIterator stmtsWithTermProp = model.listStatements(resource.getResource(),
				termProp, (RDFNode) null);
		StmtIterator stmtsWithTermInfoRefProp = model.listStatements(resource.getResource(), 
				termInfoRefProp, (RDFNode) null);
		
		
		while (stmtsWithTermProp.hasNext()) {
			termStmts.add(stmtsWithTermProp.next());
		}
		
		while (stmtsWithTermInfoRefProp.hasNext()) {
			termInfoRefStmts.add(stmtsWithTermInfoRefProp.next());
		}
		
		Map<String,Statement> distinctSubjectUris = new HashMap<String,Statement>();
		
		for(Statement stmt:termInfoRefStmts){
			String subjectUri = "<" + stmt.getSubject().getURI() + ">";
			distinctSubjectUris.put(subjectUri,stmt);
		}
		
		for(Map.Entry<String, Statement> entry:distinctSubjectUris.entrySet()){
			termStmts.add(entry.getValue().changeObject(entry.getKey()));
		}
		
		return termStmts;
	}

	/**
	 * Finds all entity statements associated to a specific resource.
	 * 
	 * @param resource
	 *            the resource
	 * @return the list of entity statements.
	 */
	private List<Statement> findEntityStmts(TextUnitResource resource) {

		List<Statement> entityStmts = new ArrayList<Statement>();
		Property identRef = model.createProperty(RDFConstants.itsrdfPrefix,
				ItsRdfConstants.TA_IDENT_REF);
		Property classRef = model.createProperty(RDFConstants.itsrdfPrefix,
				ItsRdfConstants.TA_CLASS_REF);
		Property taConfidence = model.createProperty(RDFConstants.itsrdfPrefix,
				ItsRdfConstants.TA_CONFIDENCE);
		StmtIterator identRefStmts = model.listStatements(
				resource.getResource(), identRef, (RDFNode) null);
		while (identRefStmts.hasNext()) {
			entityStmts.add(identRefStmts.next());
		}
		StmtIterator classRefStmts = model.listStatements(
				resource.getResource(), classRef, (RDFNode) null);
		while (classRefStmts.hasNext()) {
			entityStmts.add(classRefStmts.next());
		}
		StmtIterator confidenceRefStmts = model.listStatements(
				resource.getResource(), taConfidence, (RDFNode) null);
		while (confidenceRefStmts.hasNext()) {
			entityStmts.add(confidenceRefStmts.next());
		}

		return entityStmts;
	}

	/**
	 * Retrieves the list of text unit resources from the triple model.
	 * 
	 * @return the list of text unit resources.
	 */
	private List<TextUnitResource> listTextUnitResources() {

		List<TextUnitResource> tuResources = new ArrayList<TextUnitResource>();
		Property anchorOfProp = model
				.createProperty(RDFConstants.ANCHOR_OF_PROP);
		StmtIterator anchorStmts = model.listStatements(null, anchorOfProp,
				(RDFNode) null);
		// ResIterator tuResIt = model.listResourcesWithProperty(anchorOfProp);
		Statement anchorStmt = null;
		TextUnitResource unitRes = null;
		while (anchorStmts.hasNext()) {
			anchorStmt = anchorStmts.next();
			unitRes = new TextUnitResource(anchorStmt.getSubject(), anchorStmt
					.getObject().asLiteral().getString());
			if (!tuResources.contains(unitRes)) {
				tuResources.add(unitRes);
			}
		}
		Collections.sort(tuResources, new TextUnitResComparator());
		return tuResources;
	}

	/**
	 * Retrieves the skeleton context string from the triple model.
	 * 
	 * @return the skeleton context string.
	 */
	private String findSkeletonContextString() {

		Property wasConvertedFromProp = model
				.createProperty(RDFConstants.WAS_CONVERTED_FROM_PROP);
		NodeIterator wasConvFromNodes = model
				.listObjectsOfProperty(wasConvertedFromProp);
		String skeletonContextString = null;
		if (wasConvFromNodes != null && wasConvFromNodes.hasNext()) {
			RDFNode node = wasConvFromNodes.next();
			String skeletonCtxtUriPrefix = node.asResource().getURI();
			int offsetIdx = skeletonCtxtUriPrefix.indexOf(URI_OFFSET_PREFIX);
			skeletonCtxtUriPrefix = skeletonCtxtUriPrefix.substring(0,
					offsetIdx);
			Property isStringProp = model
					.createProperty(RDFConstants.IS_STRING_PROP);
			StmtIterator isStrStmts = model.listStatements(null, isStringProp,
					(RDFNode) null);
			while (isStrStmts.hasNext() && skeletonContextString == null) {
				Statement stmt = isStrStmts.next();
				if (stmt.getSubject().getURI()
						.startsWith(skeletonCtxtUriPrefix)) {
					skeletonContextString = stmt.getObject().asLiteral()
							.getString();
				}
			}

		}

		return skeletonContextString;
	}

	/**
	 * Checks if the "wasConvertedFrom" property is defined for the resource
	 * passed as parameter. If it is the case, the
	 * <code>wasConvFromStartIdx</code> and the <code>wasConvFromEndIdx</code>
	 * are properly valued.
	 * 
	 * @param resource
	 *            the text unit resource.
	 * @return <code>true</code> if the "wasConvertedFrom" property is defined
	 *         for this resource; <code>false</code> otherwise.
	 */
	private boolean checkWasConvertedFromAndSetOffset(TextUnitResource resource) {

		boolean wasConvertedFromExists = false;
		Property wasConvertedFromProp = model
				.createProperty(RDFConstants.WAS_CONVERTED_FROM_PROP);
		// NodeIterator wasConvNodesIt = model.listObjectsOfProperty(
		// resource.getResource(), wasConvertedFromProp);
		StmtIterator wasConvertedStmts = model.listStatements(
				resource.getResource(), wasConvertedFromProp, (RDFNode) null);
		if (wasConvertedStmts != null && wasConvertedStmts.hasNext()) {
			Statement wasConvStmt = wasConvertedStmts.next();
			wasConvertedFromExists = true;
			String wasConvertedURI = wasConvStmt.getObject().asResource()
					.getURI();
			// String wasConvertedURI = wasConvNodesIt.next().asResource()
			// .getURI();
			String[] wasConvOffset = getOffsetFromURI(wasConvertedURI);
			resource.setWasConvFromStartIdx(Integer.valueOf(wasConvOffset[0]));
			resource.setWasConvFromEndIdx(Integer.valueOf(wasConvOffset[1]));
		}
		return wasConvertedFromExists;
	}

	/**
	 * Gets the offset from the URI string.
	 * 
	 * @param uri
	 *            the URI string.
	 * @return an array containing the start index and the end index of the
	 *         offset.
	 */
	private String[] getOffsetFromURI(String uri) {

		int startOffsetIdx = uri.indexOf(URI_OFFSET_PREFIX);
		return uri.substring(startOffsetIdx + URI_OFFSET_PREFIX.length())
				.split(",");
	}

	// /**
	// * Gets the ITS annotation string (ITS attribute name = ITS attribute
	// value)
	// * derived from the enrichment statement passed as parameter.
	// *
	// * @param enrichmentStmt
	// * the enrichment statement.
	// * @return the ITS annotation string.
	// */
	// private String getItsAnnotationString(Statement enrichmentStmt) {
	// StringBuilder itsAnnotation = new StringBuilder();
	// itsAnnotation.append("its-");
	// String itsPropName = enrichmentStmt.getPredicate().getLocalName();
	// for (int i = 0; i < itsPropName.length(); i++) {
	// if (Character.isUpperCase(itsPropName.charAt(i))) {
	// itsAnnotation.append("-");
	// itsAnnotation.append(Character.toLowerCase(itsPropName
	// .charAt(i)));
	// } else {
	// itsAnnotation.append(itsPropName.charAt(i));
	// }
	// }
	// itsAnnotation.append("=\"");
	// if (enrichmentStmt.getObject().isResource()) {
	// itsAnnotation.append(enrichmentStmt.getObject().asResource()
	// .getURI());
	// } else {
	// itsAnnotation.append(enrichmentStmt.getObject().asLiteral()
	// .getString());
	// }
	// itsAnnotation.append("\"");
	//
	// return itsAnnotation.toString();
	// }

	/**
	 * Builds the ITS annotation strings (ITS attribute name = ITS attribute
	 * value) derived from the list of enrichment statements passed as
	 * parameter.
	 * 
	 * @param stmts
	 *            the list of enrichment statements
	 * @param parentNode
	 *            the parent node if it exists; <code>null</code> otherwise.
	 * @return the string containing the attributes describing ITS annotations.
	 */
	private String buildAnnotsAttributesString(List<Statement> stmts,
			String parentNode) {

		StringBuilder attrString = new StringBuilder();
		/*
		 * Some times it happens that one property occurs more than once in a
		 * list of enrichment statements. For example, FREME NER can return
		 * results like this itsrdf:taClassRef
		 * <http://dbpedia.org/ontology/Place> ,
		 * <http://dbpedia.org/ontology/Settlement> ,
		 * <http://nerd.eurecom.fr/ontology#Location> ,
		 * <http://dbpedia.org/ontology/Location> ,
		 * <http://dbpedia.org/ontology/City> ,
		 * <http://dbpedia.org/ontology/PopulatedPlace> ;
		 * 
		 * In this case, we translate those properties in one attribute
		 * its-ta-class-ref having just one of those values. Instead the list of
		 * multiple values is assigned to the data-its-ta-class-refs attribute
		 * as follows. its-ta-class-ref= "http://dbpedia.org/ontology/Place"
		 * data-its-ta-class-refs =
		 * "http://dbpedia.org/ontology/Place http://dbpedia.org/ontology/Location http://nerd.eurecom.fr/ontology#Location http://dbpedia.org/ontology/Settlement http://dbpedia.org/ontology/City http://dbpedia.org/ontology/PopulatedPlace"
		 * 
		 * The following map is used for grouping multiple attributes values in
		 * the same string.
		 */
		Map<String, StringBuilder> attrsMap = new HashMap<String, StringBuilder>();
		if (stmts != null) {
			String attrName = null;
			for (Statement stmt : stmts) {
				attrName = getAttributeNameForItsAnnot(stmt.getPredicate()
						.getLocalName());
				if (!attrsMap.containsKey(attrName)) {
					attrsMap.put(attrName, new StringBuilder());
				}
				String attrValue = null;
				if (stmt.getObject().isResource()) {
					attrValue = stmt.getObject().asResource().getURI();
				} else {
					attrValue = stmt.getObject().asLiteral().getString();
				}
				if (parentNode == null
						|| !parentNode.contains(attrName + "=\"" + attrValue
								+ "\"")) {
					if (attrsMap.get(attrName).length() > 0) {
						attrsMap.get(attrName).append(" ");
					}
					attrsMap.get(attrName).append(attrValue);
				}
			}
			for (Entry<String, StringBuilder> entry : attrsMap.entrySet()) {
				// if the current attribute is its-ta-class-ref and has more
				// than one value
				if (entry.getKey().equals(HTML_CLASS_REF)
						&& entry.getValue().toString().contains(" ")) {

					// insert the data-its-ta-class-refs attribute
					attrString.append(" ");
					attrString.append(HTML_DATA_CLASS_REF);
					attrString.append("=\"");
					attrString.append(entry.getValue());
					attrString.append("\"");

					// take just the first value from the list and set it as the
					// value of the its-ta-class-ref attribute.
					int firstSpaceIdx = entry.getValue().toString()
							.indexOf(" ");
					entry.setValue(new StringBuilder(entry.getValue()
							.subSequence(0, firstSpaceIdx)));

				}
				attrString.append(" ");
				attrString.append(entry.getKey());
				attrString.append("=\"");
				attrString.append(entry.getValue());
				attrString.append("\"");

			}
		}
		return attrString.toString();
	}

	/**
	 * Gets the attribute name denoting a specific ITS property. For example,
	 * the ITS attribute <code>taClassRef</code> is translated to
	 * <code>its-ta-class-ref</code>.
	 * 
	 * @param itsPropName
	 *            the ITS property.
	 * @return the attribute name.
	 */
	private String getAttributeNameForItsAnnot(String itsPropName) {

		StringBuilder itsAnnotation = new StringBuilder();
		itsAnnotation.append("its-");
		for (int i = 0; i < itsPropName.length(); i++) {
			if (Character.isUpperCase(itsPropName.charAt(i))) {
				itsAnnotation.append("-");
				itsAnnotation.append(Character.toLowerCase(itsPropName
						.charAt(i)));
			} else {
				itsAnnotation.append(itsPropName.charAt(i));
			}
		}
		return itsAnnotation.toString();
	}
}

/**
 * Wrapper class for text unit resources.
 */
class TextUnitResource {

	/** The resource from the triple model. */
	private Resource resource;

	/** The text associated to the resource. */
	private String text;

	/** The start index in the plain text context. */
	private int startIdx;

	/** The end index in the plain text context. */
	private int endIdx;

	/** The start index in the skeleton context. */
	private int wasConvFromStartIdx;

	/** The end index in the skeleton context. */
	private int wasConvFromEndIdx;

	private int additionalOffset;

	/**
	 * Constructor.
	 * 
	 * @param resource
	 *            the resource.
	 * @param text
	 *            the text
	 */
	public TextUnitResource(Resource resource, String text) {

		this.resource = resource;
		this.text = text;
		int offsetIdx = resource.getURI().indexOf(
				HTMLBackConverter.URI_OFFSET_PREFIX);
		String[] offset = resource
				.getURI()
				.substring(
						offsetIdx
								+ HTMLBackConverter.URI_OFFSET_PREFIX.length())
				.split(",");
		startIdx = Integer.valueOf(offset[0]);
		endIdx = Integer.valueOf(offset[1]);
	}

	/**
	 * Gets the resource.
	 * 
	 * @return the resource.
	 */
	public Resource getResource() {
		return resource;
	}

	/**
	 * Gets the start index in the plain text context.
	 * 
	 * @return the start index in the plain text context.
	 */
	public int getStartIdx() {
		return startIdx;
	}

	/**
	 * Gets the end index in the plain text context.
	 * 
	 * @return the end index in the plain text context.
	 */
	public int getEndIdx() {
		return endIdx;
	}

	/**
	 * Gets the text associated to the resource.
	 * 
	 * @return the text associated to the resource.
	 */
	public String getText() {
		return text;
	}

	/**
	 * Sets the text associated to the resource.
	 * 
	 * @param text
	 *            the text associated to the resource.
	 */
	public void setText(String text) {
		this.text = text;
	}

	/**
	 * Gets the start index in the skeleton context.
	 * 
	 * @return the start index in the skeleton context.
	 */
	public int getWasConvFromStartIdx() {
		return wasConvFromStartIdx;
	}

	/**
	 * Sets the start index in the skeleton context.
	 * 
	 * @param wasConvFromStartIdx
	 *            the start index in the skeleton context.
	 */
	public void setWasConvFromStartIdx(int wasConvFromStartIdx) {
		this.wasConvFromStartIdx = wasConvFromStartIdx;
	}

	/**
	 * Gets the end index in the skeleton context.
	 * 
	 * @return the end index in the skeleton context.
	 */
	public int getWasConvFromEndIdx() {
		return wasConvFromEndIdx;
	}

	/**
	 * Sets the end index in the skeleton context.
	 * 
	 * @param wasConvFromEndIdx
	 *            the end index in the skeleton context.
	 */
	public void setWasConvFromEndIdx(int wasConvFromEndIdx) {
		this.wasConvFromEndIdx = wasConvFromEndIdx;
	}

	public int getAdditionalOffset() {
		return additionalOffset;
	}

	public void setAdditionalOffset(int additionalOffset) {
		this.additionalOffset = additionalOffset;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {

		return text + "-" + startIdx + "," + endIdx;
	}

	@Override
	public boolean equals(Object obj) {

		boolean retValue = false;
		if (obj instanceof TextUnitResource) {
			TextUnitResource unit = (TextUnitResource) obj;
			retValue = text.equals(unit.getText())
					&& startIdx == unit.getStartIdx()
					&& endIdx == unit.getEndIdx();
		} else {
			retValue = super.equals(obj);
		}
		return retValue;
	}

	@Override
	public int hashCode() {
		return 31 * (text.hashCode() + startIdx + endIdx);
	}
}

/**
 * Comparator for text unit resources.
 */
class TextUnitResComparator implements Comparator<TextUnitResource> {

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	@Override
	public int compare(TextUnitResource o1, TextUnitResource o2) {

		int retValue = 0;
		if ((o1.getStartIdx() >= o2.getStartIdx() && o1.getEndIdx() <= o2
				.getEndIdx())) {
			retValue = -1;
		} else if ((o2.getStartIdx() >= o1.getStartIdx() && o2.getEndIdx() <= o1
				.getEndIdx())) {
			retValue = 1;
		} else if (o1.getStartIdx() < o2.getStartIdx()) {
			retValue = -1;
		} else if (o2.getStartIdx() < o1.getStartIdx()) {
			retValue = 1;
		}
		return retValue;
	}

}
