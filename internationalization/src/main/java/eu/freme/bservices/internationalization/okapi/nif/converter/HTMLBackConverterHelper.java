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
package eu.freme.bservices.internationalization.okapi.nif.converter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

import eu.freme.common.conversion.rdf.RDFConstants;

public class HTMLBackConverterHelper {
	
	private static final Logger logger = Logger.getLogger(HTMLBackConverterHelper.class);
	
	public static final String HTML_CLASS_REF = "its-ta-class-ref";
	public static final String HTML_DATA_CLASS_REF = "data-its-ta-class-refs";
	
	private static final String XSD_STRING = "^^xsd:string";
	private static final String XSD_STRING_LF = "^^http://www.w3.org/2001/XMLSchema#string";
	
	private static final String DOCTYPE_DECLARATION = "<!DOCTYPE html>";
	
	/** Applies the its-annotators-ref attribute to an html snippet
	 * @param html the html snippet to add the its-annotators-ref attribute
	 * @param annotatorsRefStmts Statements with property itsredf:taAnnotatorsRef
	 * @param model the model associated to the input HTML and the enrichements of the e-service
	 * @return a string representing the html snippet enriched with the its-annotators-ref attribute
	 */
	public static String applyAnnotatorsRef(String html, List<Statement> annotatorsRefStmts, Model model){
		
		Document doc = Jsoup.parse(html, "", Parser.xmlParser());
		
		Property anchorOfProp = model.createProperty(RDFConstants.nifPrefix, RDFConstants.ANCHOR_OF);
		Property annotationUnitProp = model.createProperty(RDFConstants.nifPrefix, RDFConstants.ANNOTATION_UNIT);
		
		outerLoop:
		for(Statement stmt:annotatorsRefStmts){
			
			String attributeValue = stmt.getObject().asResource().getURI();
			
			// Getting the statement with the annotationUnit as object
			StmtIterator annotationUnitIter = model.listStatements(null, annotationUnitProp, stmt.getSubject().asResource());
			
			while(annotationUnitIter != null && annotationUnitIter.hasNext()){
				
				Statement auStmt = annotationUnitIter.next();
				
				// Getting the statement with the anchorOf property for the auStmt statement subject
				StmtIterator anchorOfIter = model.listStatements(auStmt.getSubject().asResource(), anchorOfProp, (RDFNode) null);
				
				while(anchorOfIter != null && anchorOfIter.hasNext()){
					
					Statement anchorOfStmt = anchorOfIter.next();
					String textOf = anchorOfStmt.getObject().asLiteral().toString();
					
					int indexOfInText = textOf.indexOf(XSD_STRING);
					if(indexOfInText == -1){
						indexOfInText = textOf.indexOf(XSD_STRING_LF);
					}
					if(indexOfInText != -1){
						textOf = textOf.substring(0,indexOfInText);
					}
					
					Elements selections = doc.select(":matchesOwn(\\w*(?<![-a-zA-Z0-9])" + textOf + "(?![-a-zA-Z0-9]))");
					
					for(int k = 0; k < selections.size(); k++){
						
						Element selection = selections.get(k);
						String eText = selection.text();
						eText = selection.html();
						logger.debug(eText);
						
						if (selection.tagName().equals("title")){
							continue;
						}
						
						Node currentNode = selection;
						while(currentNode.parentNode() != null){
							currentNode = currentNode.parentNode();
						}
						
						addAttributeValueToAnnotatorsRef((Element)currentNode, attributeValue);
						logger.info(currentNode);
						break outerLoop;
				}
					
			}
				
		}
	}
	return doc.html();

	}
	
	public static boolean isThisToolAnnotationPresent(String previousValue, String currentValue){
		
		if(previousValue == null || previousValue.equals("")) {
			return false;
		}
		if(previousValue.equals(currentValue)){
			return true;
		}
		
		if(previousValue.contains(" ")){
			String[] values = previousValue.split(" ");
			for(String value:values) {
				if(currentValue.equals(value)){
					return true;
				}
			}
			return false;
		} else {
			return false;
		}
	}
	
	/**
	 * Builds the ITS annotation strings (ITS attribute name = ITS attribute
	 * value) derived from the list of enrichment statements passed as
	 * parameter.
	 * 
	 * @param stmts the list of enrichment statements
	 * @param parentNode the parent node if it exists; <code>null</code> otherwise.
	 * @return the string containing the attributes describing ITS annotations.
	 */
	public static String buildAnnotsAttributesString(List<Statement> stmts, String parentNode) {

		StringBuilder attrString = new StringBuilder();
		
		/*
		 * Sometimes it happens that one property occurs more than once in a
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
		 * "http://dbpedia.org/ontology/Place http://dbpedia.org/ontology/Location 
		 * http://nerd.eurecom.fr/ontology#Location http://dbpedia.org/ontology/Settlement 
		 * http://dbpedia.org/ontology/City http://dbpedia.org/ontology/PopulatedPlace"
		 * 
		 * The following map is used for grouping multiple attributes values in
		 * the same string.
		 */
		Map<String, StringBuilder> attrsMap = new HashMap<String, StringBuilder>();
		
		if (stmts != null) {
			
			String attrName = null;
			for (Statement stmt : stmts) {
				attrName = getAttributeNameForItsAnnot(stmt.getPredicate().getLocalName());
				if (!attrsMap.containsKey(attrName)) {
					attrsMap.put(attrName, new StringBuilder());
				}
				String attrValue = null;
				if (stmt.getObject().isResource()) {
					attrValue = stmt.getObject().asResource().getURI();
				} else {
					attrValue = stmt.getObject().asLiteral().getString();
				}
				if (parentNode == null || !parentNode.contains(attrName + "=\"" + attrValue + "\"")) {
					
					if (attrsMap.get(attrName).length() > 0) {
						attrsMap.get(attrName).append(" ");
					}
					attrsMap.get(attrName).append(attrValue);
				}
			}
			
			for (Entry<String, StringBuilder> entry : attrsMap.entrySet()) {
				// if the current attribute is its-ta-class-ref and has more than one value
				if (entry.getKey().equals(HTML_CLASS_REF) && entry.getValue().toString().contains(" ")) {

					// insert the data-its-ta-class-refs attribute
					attrString.append(" ");
					attrString.append(HTML_DATA_CLASS_REF);
					attrString.append("=\"");
					attrString.append(entry.getValue());
					attrString.append("\"");

					// take just the first value from the list and set it as the value of the its-ta-class-ref attribute.
					int firstSpaceIdx = entry.getValue().toString().indexOf(" ");
					entry.setValue(new StringBuilder(entry.getValue().subSequence(0, firstSpaceIdx)));

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
	 * @param itsPropName the ITS property.
	 * @return the attribute name.
	 */
	public static String getAttributeNameForItsAnnot(String itsPropName) {

		StringBuilder itsAnnotation = new StringBuilder();
		itsAnnotation.append("its-");
		for (int i = 0; i < itsPropName.length(); i++) {
			if (Character.isUpperCase(itsPropName.charAt(i))) {
				itsAnnotation.append("-");
				itsAnnotation.append(Character.toLowerCase(itsPropName.charAt(i)));
			} else {
				itsAnnotation.append(itsPropName.charAt(i));
			}
		}
		return itsAnnotation.toString();
	}
	
	public static boolean isHtmlSnippet(String html){
		
		if(!html.startsWith(DOCTYPE_DECLARATION)){
			return true;
		}
		return false;
	}
	
	private static void addAttributeValueToAnnotatorsRef(Element node, String attributeValue) {
		
		String attributeName = HTMLBackConverterHelper.getAttributeNameForItsAnnot(RDFConstants.TA_ANNOTATORS_REF);
		
		Elements children = node.children();
		for(Element child:children){
			String oldValue = child.attr(attributeName);
			if(!isThisToolAnnotationPresent(oldValue, attributeValue)) {
				if(oldValue != null && !oldValue.equals("") ) {
					String updatedValue = oldValue + " " + attributeValue;
					child.removeAttr(attributeName);
					child.attr(attributeName, updatedValue);
				} else  {
					child.attr(attributeName, attributeValue);
				}
			}
			
		}
		
		logger.info(node);
		
	}
	
}

