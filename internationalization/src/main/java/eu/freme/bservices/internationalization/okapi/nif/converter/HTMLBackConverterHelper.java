package eu.freme.bservices.internationalization.okapi.nif.converter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
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
	
	/** Applies the its-annotators-ref attribute to an html snippet
	 * @param html the html snippet to add the its-annotators-ref attribute
	 * @param annotatorsRefStmts Statements with property itsredf:taAnnotatorsRef
	 * @param model the model associated to the input HTML and the enrichements of the e-service
	 * @return a string representing the html snippet enriched with the its-annotators-ref attribute
	 */
	public static String applyAnnotatorsRef(String html, List<Statement> annotatorsRefStmts, Model model){
		
		Document doc = Jsoup.parse(html);
		
		Property anchorOfProp = model.createProperty(RDFConstants.nifPrefix, RDFConstants.ANCHOR_OF);
		Property annotationUnitProp = model.createProperty(RDFConstants.nifPrefix, RDFConstants.ANNOTATION_UNIT);
		
		for(Statement stmt:annotatorsRefStmts){
			
			String attributeValue = stmt.getObject().asResource().getURI();
			
			// Getting the statement with this annotationUnit
			StmtIterator annotationUnitIter = model.listStatements(null, annotationUnitProp, stmt.getSubject().asResource());
			
			while(annotationUnitIter.hasNext()){
				
				Statement auStmt = annotationUnitIter.next();
				
				// Getting the anchorOf statement of the statement with that annotationUnit
				StmtIterator anchorOfIter = model.listStatements(auStmt.getSubject().asResource(), anchorOfProp, (RDFNode) null);
				
				while(anchorOfIter.hasNext()){
					
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
						
						Element e = selections.get(k);
						String eText = e.text();
						eText = e.html();
						logger.debug(eText);
						
						if (e.tagName().equals("title")){
							continue;
						}
						
						String attrValue = e.attr(HTMLBackConverterHelper.getAttributeNameForItsAnnot("taAnnotatorsRef"));
						
						if(attrValue == null || attrValue.equals("")){
							e.attr(HTMLBackConverterHelper.getAttributeNameForItsAnnot("taAnnotatorsRef"), attributeValue);
						} 
				}
			}
				
				
		}
	}
	return doc.html();

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
}

