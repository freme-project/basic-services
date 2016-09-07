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

import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
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
	
	private final Logger logger = Logger.getLogger(this.getClass());

	/**
	 * Default constructor.
	 */
	public HTMLBackConverter() {

	}

	public InputStream convertBack(String nifVersion,final InputStream skeletonFile,
			final InputStream enrichedFile) {

		return convertBack(nifVersion, skeletonFile, enrichedFile,
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
	
	/** Builds a map between an enriched text and the list of target html elements
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
				annotatedText.append(buildAnnotsAttributesString(entityStmts, null));
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
	 * Performs the back conversion.
	 * 
	 * @param nifVersion the nif Version 2.0 or 2.1 .
	 * @return the skeleton context string plus enrichments, when there are
	 */
	private String convertBack(String nifVersion) {

		String skeletonContext = findSkeletonContextString(nifVersion);
		String unEscapeXML = NifConverterUtil.unescapeXmlInScriptElements(skeletonContext);
		
		String tbs = "";
		
		if (skeletonContext != null) {
			
			List<TextUnitResource> tuResources = listTextUnitResources(nifVersion);
			Map<String, List<String>> enrichments = buildAnnotationsMap(tuResources);
			tbs = unEscapeXML;
			for(Map.Entry<String,List<String>> entry: enrichments.entrySet()){
				
				String enriched = entry.getKey();
			    String enrichedHtml = getEnrichedHtml(tbs, enriched,entry.getValue(), enrichments);
			    tbs = enrichedHtml;
			}
			
		}
		
		logger.debug(tbs);
		
		// Translation only available with nif 2.1
		if(nifVersion != null && nifVersion.equals("2.1")){
			
			HTMLBackTranslatorHelper backTranslHelper = new HTMLBackTranslatorHelper();
			String backTranslation = backTranslHelper.translateBack(tbs, model);
			return backTranslation;
			
		} else {
			
			return tbs;
		}
		
	}
	
	/**
	 * @param html
	 * @param enriched
	 * @param htmlEnrichments
	 * @param enrichments
	 * @return
	 */
	private String getEnrichedHtml(String html, String enriched, List<String> htmlEnrichments, Map<String, List<String>> enrichments){
		
		Map<String, Integer> containingKeys = getContainingKeys(enriched, enrichments);
		
		String eText = "";
		try{
			Document doc = Jsoup.parse(html);
			Elements selections = doc.select(":matchesOwn(\\w*(?<![-a-zA-Z0-9])" + enriched + "(?![-a-zA-Z0-9]))");
			//int k = 0;// selection index
			int j = 0;
			// looping through the selections
			for(int k = 0; k < selections.size(); k++){
				
				StringBuilder sb = new StringBuilder();
				Element e = selections.get(k);
				eText = e.text();
				eText = e.html();
				if (e.tagName().equals("title")){
					continue;
				}
				
				String regex = "\\w*(?<![-a-zA-Z0-9])" + enriched + "(?![-a-zA-Z0-9])";
				
                if( containingKeys.isEmpty() ){
                	// There are no keys including the key with value "enriched"
                	Pattern p = Pattern.compile(regex);
    			    Matcher m = p.matcher(eText);
    			    int delta = 0;
    			    String eText0 = eText;
    			    while (m.find()){
    			    	// selection verifies regex
    			    	String group = m.group();
    			    	logger.debug(group);
    			    	// position of enriched
    			    	int indexOf = m.end() - delta - enriched.length();
    			    	if(NifConverterUtil.isAttributeContent(eText)[indexOf]){
    			    		continue;
    			    	}
    			    	sb.append(eText.substring(0,indexOf));
    			    	sb.append(htmlEnrichments.get(j));
    			    	j += 1;
                        //The new string to analyze (the remaining part)
    			    	eText = eText.substring(indexOf + enriched.length());
    			    	delta = eText0.length() - eText.length();
    			    	
    			    }
    			    // Add the remaining part
    			    sb.append(eText);
    			    eText = sb.toString();
    			    e.html(eText);   
    			    
                }else {
                	// There are keys including the key with value "enriched"
                	Pattern p = Pattern.compile(regex);
            	    Matcher m = p.matcher(eText);
            	    int delta = 0;
    			    String eText0 = eText;
            	    while (m.find()){
            	    	// selection verifies regex
            	    	String group = m.group();
            	    	logger.debug(group);
            	    	// position of enriched
            	    	int indexOf = m.end() - delta - enriched.length();
            	    	// verify we can accept the matching
            	    	if(hasWiderMatching(containingKeys, eText, indexOf) || NifConverterUtil.isAttributeContent(eText)[indexOf]){
            	    		//Discard the matching
            	    		continue;
            	    	} else {
            	    		//Accept the matching
            	    		sb.append(eText.substring(0,indexOf));
        			    	sb.append(htmlEnrichments.get(j));
        			    	j += 1;
                            //The new string to analyze (the remaining part)
        			    	eText = eText.substring(indexOf + enriched.length());
        			    	delta = eText0.length() - eText.length();
            	    	}
            	    }
    			    
            	    // Add the remaining part
    			    sb.append(eText);
    			    eText = sb.toString();
    			    e.html(eText);  
                }
				
			}// End of looping through the selections
			
			return doc.html();
		}catch(Exception e){
			
			logger.error("EXCEPTION OCCURRED." + e.getMessage());
			logger.error("When processing enrichment of text:" + enriched);
			return html;
		}
		
	}
	
	/**
	 * @param containingKeys the enriched texts containing the enriched text
	 * @param text the text to be checked
	 * @param index the position of this text in the matching
	 * @return true if a wider match exists 
	 */
	public static boolean hasWiderMatching(Map<String, Integer> containingKeys, String text, int index ){
		
		for(Map.Entry<String, Integer> entry:containingKeys.entrySet()){
			String containerRegex = "\\w*(?<![a-zA-Z0-9])" + entry.getKey() + "(?![a-zA-Z0-9])";
	    	Pattern p = Pattern.compile(containerRegex);
		    Matcher m = p.matcher(text);
		    while(m.find()){
		    	int indexOf = m.end() - entry.getKey().length() + entry.getValue();
		    	if(NifConverterUtil.isAttributeContent(text)[indexOf]){
		    		continue;
		    	}
			    if(indexOf == index){
			    	return true;
			    }
		    }
		    
		}
		return false;
	}
	
    /**
     * @param key The map key representing service-enriched text
     * @param enrichments The mappings between the service-enriched text and the target html 
     * @return a mapping between the containing keys and the position of key within the keys
     */
    private Map<String, Integer> getContainingKeys(String key, Map<String, List<String>> enrichments){
        
	    Map<String, Integer> containingKeys = new HashMap<String, Integer>();
	    
	    for(Map.Entry<String, List<String>> entry: enrichments.entrySet()){
	    	
		    String mapKey = entry.getKey();
		    boolean isLonger = mapKey.length() > key.length();
		    if(!mapKey.equals(key) && mapKey.contains(key) && isLonger){
		    	String regex = "\\w*(?<![a-zA-Z0-9])" + key + "(?![a-zA-Z0-9])";
		    	Pattern p = Pattern.compile(regex);
		    	Matcher m = p.matcher(mapKey);
		        if(m.find()){
		        	int position = m.end() - key.length();
		        	containingKeys.put(mapKey, position);
		        }
		    }
	    }
     
	    return containingKeys;
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
	private List<TextUnitResource> listTextUnitResources(String nifVersion) {

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
		String nifPrefix = isNif20?RDFConstants.nifPrefix_2_0:RDFConstants.nifPrefix_2_1;

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

}
