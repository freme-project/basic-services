package eu.freme.bservices.internationalization.okapi.nif.converter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import eu.freme.bservices.internationalization.okapi.nif.converter.util.NifConverterUtil;

public class HTMLBackTerminologyHelper {
	
	private static final Logger logger = Logger.getLogger(HTMLBackTerminologyHelper.class);
	
	/**
	 * @param html
	 * @param enriched
	 * @param htmlEnrichments
	 * @param enrichments
	 * @return
	 */
	public static String getEnrichedHtml(String html, String enriched, List<String> htmlEnrichments, Map<String, List<String>> enrichments){
		
		Map<String, Integer> containingKeys = getContainingKeys(enriched, enrichments);
		
		String eText = "";
		try{
			Document doc = null;
			if(HTMLBackConverterHelper.isHtmlSnippet(html)){
				doc = Jsoup.parse(html, "", Parser.xmlParser());
			} else {
				doc = Jsoup.parse(html);
			}
			
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
     * @param key The map key representing service-enriched text
     * @param enrichments The mappings between the service-enriched text and the target html 
     * @return a mapping between the containing keys and the position of key within the keys
     */
    private static Map<String, Integer> getContainingKeys(String key, Map<String, List<String>> enrichments){
        
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
	 * @param containingKeys the enriched texts containing the enriched text
	 * @param text the text to be checked
	 * @param index the position of this text in the matching
	 * @return true if a wider match exists 
	 */
	private static boolean hasWiderMatching(Map<String, Integer> containingKeys, String text, int index ){
		
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

}
