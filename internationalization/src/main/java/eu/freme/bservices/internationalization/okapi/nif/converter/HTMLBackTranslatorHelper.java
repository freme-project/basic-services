package eu.freme.bservices.internationalization.okapi.nif.converter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

import eu.freme.bservices.internationalization.okapi.nif.converter.util.NifConverterUtil;
import eu.freme.bservices.internationalization.okapi.nif.filter.RDFConstants;
import eu.freme.bservices.internationalization.okapi.nif.its.ItsRdfConstants;

/**
 * @author KatiaI
 *
 */
public class HTMLBackTranslatorHelper {
	
	private final Logger logger = Logger.getLogger(this.getClass());
	
		private String nifPrefix;
		private String nifOffset;
	
		public HTMLBackTranslatorHelper(){
			
			// Parsing a nif file version 2.1
			nifPrefix = RDFConstants.nifPrefix_2_1;
			nifOffset = RDFConstants.NIF21_OFFSET;
		}
	
		public String translateBack(String skeletonContextString, Model model) {
			
			List<TranslationUnit> translationUnits = new ArrayList<TranslationUnit>();
			
			List<Statement> annotationUnitStmts = new ArrayList<Statement>();
			
			Property annotationUnitProp = model.createProperty(nifPrefix, RDFConstants.ANNOTATION_UNIT);
			StmtIterator stmsWithAnnotationUnitProp = model.listStatements(null,annotationUnitProp, (RDFNode) null);
			
			while (stmsWithAnnotationUnitProp.hasNext()) {
				annotationUnitStmts.add(stmsWithAnnotationUnitProp.next());
			}
			
			Property targetProp = model.createProperty(RDFConstants.itsrdfPrefix, ItsRdfConstants.TARGET);
			
			// loop through the annotationUnit statements
			for( Statement stmt:annotationUnitStmts ){
				
				String target = "";
				// Obtaining target (translation of source segment)
				
				Resource asResource = stmt.getObject().asResource();
			
				StmtIterator stmsWithTargetProp = model.listStatements(asResource, targetProp, (RDFNode) null);
				
				while(stmsWithTargetProp.hasNext()){
					Statement next = stmsWithTargetProp.next();
					target = next.getObject().asLiteral().toString();
				}
				
				int indexOfInTarget = target.indexOf("@");
				if(indexOfInTarget != -1){
					target = target.substring(0,indexOfInTarget);
				}
				
				target = StringEscapeUtils.escapeHtml(target);
				
				String subjectUri = stmt.getSubject().asResource().getURI();
				String[] indexes = subjectUri.substring(subjectUri.indexOf(nifOffset) + nifOffset.length()).split("_");
				Property anchorOfProp = model.createProperty(nifPrefix, RDFConstants.ANCHOR_OF);
				StmtIterator anchorStmtsForSubjectUri = model.listStatements(stmt.getSubject().asResource(), anchorOfProp,
						(RDFNode) null);
				String source = "";
				while(anchorStmtsForSubjectUri.hasNext()){
					Statement next = anchorStmtsForSubjectUri.next();
					source = next.getObject().asLiteral().toString();
				}
				
				String xsdString = "^^xsd:string";
				String xsdStringLong = "^^http://www.w3.org/2001/XMLSchema#string";
				int indexOfInSource = source.indexOf(xsdString);
				if(indexOfInSource == -1){
					indexOfInSource = source.indexOf(xsdStringLong);
				}
				if(indexOfInSource != -1){
					source = source.substring(0,indexOfInSource);
				}
				
				
				TranslationUnit tu = new TranslationUnit();
				tu.setSource(source);
				tu.setTarget(target);
				tu.setStartIndex(Integer.valueOf(indexes[0]));
				tu.setEndIndex(Integer.valueOf(indexes[1]));
				tu.setSkelIndex(-1);
				translationUnits.add(tu);
					
			}
			
			logger.info("Translation Units:");
			translationUnits.forEach((tu) -> logger.info(tu));
			translationUnits.sort(Comparator.comparing((tu) -> tu.getStartIndex()));
			translationUnits.forEach((tu) -> logger.info(tu));
			translationUnits.forEach((tu) -> logger.info(tu));
			
			setSkelPositionsInTranslationUnits(skeletonContextString, translationUnits);
			
			StringBuilder sb = new StringBuilder();
			int startIndex = 0;
			for(TranslationUnit tu:translationUnits){
				if(tu.getSkelIndex() >= startIndex){
					String untranslated = skeletonContextString.substring(startIndex, tu.getSkelIndex());
					sb.append(untranslated);
					sb.append(tu.getTarget());
					startIndex = tu.getSkelEndIndex();
				} 
				
			}
			
			if(startIndex < skeletonContextString.length()){
				sb.append(skeletonContextString.substring(startIndex));
			}
			
			String finalString = sb.toString();
			
			return finalString;
		}
		
		private void setSkelPositionsInTranslationUnits(String html, List<TranslationUnit> translationUnits){
			
			int fromIndex = 0;
			boolean[] scriptsAndStyles = NifConverterUtil.isScriptsAndStyles(html);
			boolean[] attributeContent = NifConverterUtil.isAttributeContent(html);
			for(TranslationUnit tu:translationUnits){
				
				String regex = NifConverterUtil.getRegex(tu.getSource());
				
	            Pattern p = Pattern.compile(regex);
				Matcher m = p.matcher(html);
				boolean find = m.find();
				
				if(!find){
					// find the tag to insert
					StringTokenizer tokenizer = new StringTokenizer(tu.getSource());
					int count = 0;
					List<List<Integer>> indexes = new ArrayList<List<Integer>>();
					while (tokenizer.hasMoreElements()) {
						String element = tokenizer.nextElement().toString();
						logger.debug("\nElement:" + element);
						// Searching for element in html
						
						String elementRegex = NifConverterUtil.getRegex(element);
												
			            Pattern pattern = Pattern.compile(elementRegex);
						Matcher matcher = pattern.matcher(html);
						
						searchElement:
					    while (matcher.find()){
					    	String group = matcher.group();
					    	logger.debug(group);
					    	logger.debug("\ngroup:" + group);
					    	// position of matching
					    	int elementStartIndex = matcher.end() - element.length();
					    	logger.info("\nelementStartIndex:" + elementStartIndex);
					    	if( elementStartIndex >= fromIndex && !scriptsAndStyles[elementStartIndex] && !attributeContent[elementStartIndex] ){
					    		if(count == 0) {
					    			tu.setSkelIndex(elementStartIndex);
					    		}
					    		fromIndex = elementStartIndex + element.length();
					    		indexes.add(Arrays.asList(elementStartIndex,fromIndex));
					    		logger.info("\nfromIndex:" + fromIndex);
					    		break searchElement;
					    	}
					    }
						count++;
					}
					
					tu.setSkelEndIndex(fromIndex);
					
					for(int j = 0; j < indexes.size() - 1; j++) {
						String tbs = html.substring(indexes.get(j).get(1), indexes.get(j + 1).get(0));
						String tagRegex = "<[a-zA-Z](.*?)>";
						Pattern ptrn = Pattern.compile(tagRegex);
						Matcher mtcr = ptrn.matcher(tbs);
						String tags = "";
						while(mtcr.find()){
							String group = mtcr.group();
					    	logger.debug(group);
					    	logger.info("\ngroup:" + group);
					    	logger.info("\ntarget:" + tu.getTarget());
					    	tags = tags + group;
						}
						
						String newTarget = tags + tu.getTarget();
						String closingTagRegex = "</([a-zA-Z]*)>";
						Pattern nrtp = Pattern.compile(closingTagRegex);
						Matcher rctm = nrtp.matcher(tbs);
						String sgat = "";
						while(rctm.find()){
							String group = rctm.group();
					    	logger.debug(group);
					    	logger.info("\ngroup:" + group);
					    	logger.info("\ntarget:" + tu.getTarget());
					    	sgat = sgat + group;
						}
						logger.info("\nsgat:" + sgat);
						newTarget = newTarget + sgat;
				    	tu.setTarget(newTarget);
					}
					
				}
				
				searchSource:
			    while (find){
			    	String group = m.group();
			    	logger.debug(group);
			    	// position of matching
			    	int sourceStartIndex = m.end() - tu.getSource().length();
			    	if( sourceStartIndex >= fromIndex && !scriptsAndStyles[sourceStartIndex] && !attributeContent[sourceStartIndex] ){
			    		tu.setSkelIndex(sourceStartIndex);
			    		fromIndex = sourceStartIndex + tu.getSource().length();
			    		tu.setSkelEndIndex(fromIndex);
			    		find = m.find();
			    		break searchSource;
			    	}
			    	find= m.find();
			    }
			}
			
			
		}

}
