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

import static eu.freme.common.conversion.rdf.RDFConstants.nifPrefix;

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

import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.util.StringUtils;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

import eu.freme.bservices.internationalization.okapi.nif.converter.util.NifConverterUtil;
import eu.freme.bservices.internationalization.okapi.nif.its.ItsRdfConstants;
import eu.freme.common.conversion.rdf.JenaRDFConversionService;
import eu.freme.common.conversion.rdf.RDFConstants;

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

	/** The triple model. */
	private Model model;
	
	private final Logger logger = Logger.getLogger(this.getClass());

	/**
	 * Default constructor.
	 */
	public HTMLBackConverter() {

	}

	public InputStream convertBack(String nifVersion,final InputStream skeletonFile,
			final InputStream enrichedFile) {

		return convertBack(nifVersion, skeletonFile, enrichedFile,
				JenaRDFConversionService.JENA_TURTLE,
				JenaRDFConversionService.JENA_TURTLE
				//RDFConstants.RDFSerialization.TURTLE.toRDFLang(),
				//RDFConstants.RDFSerialization.TURTLE.toRDFLang()
		);
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
	public InputStream convertBack(String nifVersion, final InputStream skeletonFile,
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
		String originalFile = convertBack(nifVersion);
		return new ByteArrayInputStream(originalFile.getBytes());
	}
	
	/**
	 * Performs the back conversion.
	 * 
	 * @param nifVersion the nif Version 2.0 or 2.1 .
	 * @return the skeleton context string plus enrichments, when there are
	 */
	private String convertBack(String nifVersion) {
		
		boolean isNif21 = nifVersion != null && nifVersion.equals("2.1");
		String skeletonContext = findSkeletonContextString(nifVersion);
		String tbp = skeletonContext;
		String tbs = "";
		
		if (tbp != null) {
			
			// Handling its-annotators-ref attribute in HTML (only when nifVersion 2.1)
			if(isNif21){
				List<Statement> annotatorsRefStmts = findAnnotatorsRefStmts();
				if(annotatorsRefStmts != null && !annotatorsRefStmts.isEmpty()){
					// whole HTML document
					if(!HTMLBackConverterHelper.isHtmlSnippet(skeletonContext)){
						//the attribute is added to the <html> element
						Statement statement = annotatorsRefStmts.get(0);
						String attributeValue = statement.getObject().asResource().getURI();
						logger.debug("Found Tool Annotation");
						Document doc = Jsoup.parse(skeletonContext);
						String attributeName = HTMLBackConverterHelper.getAttributeNameForItsAnnot(RDFConstants.TA_ANNOTATORS_REF);
						if(doc.select("html").hasAttr(attributeName)) {
							String oldValue = doc.select("html").attr(attributeName);
							if(!HTMLBackConverterHelper.isThisToolAnnotationPresent(oldValue, attributeValue)){
								if(oldValue != null && !oldValue.equals("")) {
									String updatedValue = oldValue + " " + attributeValue;
									doc.select("html").removeAttr(attributeName);
									doc.select("html").attr(attributeName, updatedValue);
								} else {
									doc.select("html").attr(attributeName, attributeValue);
								}
							}
						} else {
							doc.select("html").attr(attributeName, attributeValue);
						}
						tbp = doc.html();
					// Html snippet
					} else {
						tbp = HTMLBackConverterHelper.applyAnnotatorsRef(tbp, annotatorsRefStmts, model);
					}
				}
			}
			
			String unEscapeXML = NifConverterUtil.unescapeXmlInScriptElements(tbp);
			// Handling Terminology
			List<TextUnitResource> tuResources = listTextUnitResources(nifVersion);
			Map<String, List<String>> enrichmentsMap = buildAnnotationsMap(tuResources);
			
			tbs = unEscapeXML;
			for(Map.Entry<String,List<String>> entry: enrichmentsMap.entrySet()){
				String enriched = entry.getKey();
			    String enrichedHtml = HTMLBackTerminologyHelper.getEnrichedHtml(tbs, enriched,entry.getValue(), enrichmentsMap);
			    tbs = enrichedHtml;
			}
			
			logger.debug(tbs);
			
			// Handling Translation (only available with nif 2.1)
			if(isNif21){
				HTMLBackTranslatorHelper backTranslHelper = new HTMLBackTranslatorHelper();
				String backTranslation = backTranslHelper.translateBack(tbs, model);
				return backTranslation;
			} else {
				return tbs;
			}
			
		} else {
			return tbs;
		}
		
	}
	
	/** Builds a map between an enriched text and the list of html target elements
	 * 
	 * @param tuResources the text unit resources list
	 * @return the mapping between enriched text unit resource and the html elements
	 */
	private Map<String,List<String>> buildAnnotationsMap(List<TextUnitResource> tuResources) {
		
		//The mapping between the text of a text unit resource enriched and the list of target html elements
		Map<String,List<String>> enrichments = new HashMap<String,List<String>>();
		
		for(TextUnitResource tur: tuResources){
			
			List<Statement> entityStmts = findEnrichmentStatements(tur);
			
			if(!entityStmts.isEmpty()){
				
				StringBuilder annotatedText = new StringBuilder();
				
				annotatedText.append("<span");
				annotatedText.append(HTMLBackConverterHelper.buildAnnotsAttributesString(entityStmts, null));
				annotatedText.append(">");
				annotatedText.append(tur.getText().replace("\n", ""));
				annotatedText.append("</span>");
				
				String key = tur.getText().replace("\n", "");
				
				if(enrichments.get(key) != null) {
					enrichments.get(key).add(annotatedText.toString());
				} else {
					ArrayList<String> htmlElements = new ArrayList<String>();
					htmlElements.add(annotatedText.toString());
					enrichments.put(key, htmlElements);
				}
			}
		}
		
		logger.debug("\nEnrichments:\n");
		enrichments.forEach((k,v) -> logger.debug(k + "=" + v + ",size=" + v.size()));
		return enrichments;
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
	 * @param resource the resource
	 * @return the list of terminology statements.
	 */
	private List<Statement> findTermStmts(TextUnitResource resource) {

		List<Statement> termStmts = new ArrayList<Statement>();
		List<Statement> termInfoRefStmts = new ArrayList<Statement>();
		
		Property termProp = model.createProperty(RDFConstants.itsrdfPrefix,ItsRdfConstants.TERM_INFO);
		Property termInfoRefProp = model.createProperty(RDFConstants.itsrdfPrefix,ItsRdfConstants.TERM_INFO_REF);
		
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
			String subjectUri = stmt.getSubject().getURI();
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
		Property identRef = model.createProperty(RDFConstants.itsrdfPrefix,ItsRdfConstants.TA_IDENT_REF);
		Property classRef = model.createProperty(RDFConstants.itsrdfPrefix,ItsRdfConstants.TA_CLASS_REF);
		Property taConfidence = model.createProperty(RDFConstants.itsrdfPrefix,ItsRdfConstants.TA_CONFIDENCE);
		
		StmtIterator identRefStmts = model.listStatements(resource.getResource(), identRef, (RDFNode) null);
		
		while (identRefStmts.hasNext()) {
			entityStmts.add(identRefStmts.next());
		}
		
		StmtIterator classRefStmts = model.listStatements(resource.getResource(), classRef, (RDFNode) null);
		
		while (classRefStmts.hasNext()) {
			entityStmts.add(classRefStmts.next());
		}
		
		StmtIterator confidenceRefStmts = model.listStatements(resource.getResource(), taConfidence, (RDFNode) null);
		
		while (confidenceRefStmts.hasNext()) {
			entityStmts.add(confidenceRefStmts.next());
		}

		return entityStmts;
	}
	
	private List<Statement> findAnnotatorsRefStmts() {

		List<Statement> annotatorsRefStmts = new ArrayList<Statement>();
		
		Property annotatorsRefProp = model.createProperty(RDFConstants.itsrdfPrefix,ItsRdfConstants.TA_ANNOTS_REF);
		
		StmtIterator stmtsWithAnnotatorsRefProp = model.listStatements(null,annotatorsRefProp, (RDFNode) null);
		
		while (stmtsWithAnnotatorsRefProp != null && stmtsWithAnnotatorsRefProp.hasNext()) {
			annotatorsRefStmts.add(stmtsWithAnnotatorsRefProp.next());
		}
		
		return annotatorsRefStmts;
	}


	/**
	 * Retrieves the list of text unit resources from the triple model.
	 * 
	 * @return the list of text unit resources.
	 */
	private List<TextUnitResource> listTextUnitResources(String nifVersion) {

		List<TextUnitResource> tuResources = new ArrayList<TextUnitResource>();
		Property anchorOfProp = model.createProperty(nifPrefix + RDFConstants.ANCHOR_OF);
		StmtIterator anchorStmts = model.listStatements(null, anchorOfProp,(RDFNode) null);
		
		Statement anchorStmt = null;
		TextUnitResource unitRes = null;
		
		while (anchorStmts.hasNext()) {
			anchorStmt = anchorStmts.next();
			unitRes = new TextUnitResource(anchorStmt.getSubject(), anchorStmt.getObject().asLiteral().getString(), nifVersion);
			if (!tuResources.contains(unitRes)) {
				tuResources.add(unitRes);
			}
		}
		
		Collections.sort(tuResources, new TextUnitResComparator());
		return tuResources;
	}

	/**
	 * Retrieves the skeleton context string from the triple model.
	 * @param nifVersion the nif Version 2.0 or 2.1 .
	 * @return the skeleton context string.
	 */
	private String findSkeletonContextString(String nifVersion) {
		
		boolean isNif20 = StringUtils.isEmpty(nifVersion) || nifVersion.equals("2.0");
		
		String offsetPrefix = isNif20?RDFConstants.NIF20_OFFSET:RDFConstants.NIF21_OFFSET;

		Property wasConvertedFromProp = model.createProperty(nifPrefix + RDFConstants.WAS_CONVERTED_FROM);
		NodeIterator wasConvFromNodes = model.listObjectsOfProperty(wasConvertedFromProp);
		
		String skeletonContextString = null;
		if (wasConvFromNodes != null && wasConvFromNodes.hasNext()) {
			
			RDFNode node = wasConvFromNodes.next();
			String skeletonCtxtUriPrefix = node.asResource().getURI();
			int offsetIdx = skeletonCtxtUriPrefix.indexOf(offsetPrefix);
			skeletonCtxtUriPrefix = skeletonCtxtUriPrefix.substring(0, offsetIdx);
			
			Property isStringProp = model.createProperty(nifPrefix + RDFConstants.IS_STRING);
			
			StmtIterator isStrStmts = model.listStatements(null, isStringProp,(RDFNode) null);
			
			while (isStrStmts.hasNext() && skeletonContextString == null) {
				Statement stmt = isStrStmts.next();
				if (stmt.getSubject().getURI().startsWith(skeletonCtxtUriPrefix)) {
					skeletonContextString = stmt.getObject().asLiteral().getString();
				}
			}

		}

		return skeletonContextString;
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
	 * @param resource the resource.
	 * @param text the text
	 * @param nifVersion the nif Version 2.0 or 2.1 .
	 */
	public TextUnitResource(Resource resource, String text, String nifVersion) {
		
		boolean isNif20 = StringUtils.isEmpty(nifVersion) || nifVersion.equals("2.0");
		
		String offsetPrefix = isNif20?RDFConstants.NIF20_OFFSET:RDFConstants.NIF21_OFFSET;
		String splitter = isNif20?",":"_";

		this.resource = resource;
		this.text = text;
		int offsetIdx = resource.getURI().indexOf(offsetPrefix);
		String[] offset = resource.getURI().substring(offsetIdx	+ offsetPrefix.length()).split(splitter);
		
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

}}
