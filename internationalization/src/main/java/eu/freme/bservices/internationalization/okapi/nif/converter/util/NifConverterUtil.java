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
package eu.freme.bservices.internationalization.okapi.nif.converter.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import eu.freme.bservices.internationalization.api.InternationalizationAPI;
import eu.freme.bservices.internationalization.okapi.nif.converter.util.Definitions.NwLine;
import eu.freme.bservices.internationalization.okapi.nif.step.NifParameters;

public class NifConverterUtil {
	
	static final Logger logger = Logger.getLogger(NifConverterUtil.class);
	
	private static String doctype = "DOCTYPE";
	private static String html = "HTML";
	private static String head = "HEAD";
	private static String body = "BODY";
	
	public static String getDoctype() {
		return doctype;
	}

	public static String getHtml() {
		return html;
	}

	public static String getHead() {
		return head;
	}

	public static String getBody() {
		return body;
	}
	
	public static String getShortenedSkeletonString(String skeletonString, NifParameters params) {
		
	    if(!params.getHtml().equals("HTML")) {
	        skeletonString = skeletonString.replaceAll("<(H|h)(T|t)(M|m)(L|l)>", "");
	        skeletonString = skeletonString.replaceAll("</(H|h)(T|t)(M|m)(L|l)>", "");
	   }
	   if(!params.getHead().equals("HEAD")) {
		    skeletonString = skeletonString.replaceAll("<(H|h)(E|e)(A|a)(D|d)>", "");
		    skeletonString = skeletonString.replaceAll("</(H|h)(E|e)(A|a)(D|d)>", "");
	   }
	   if(!params.getBody().equals("BODY")) {
		    skeletonString = skeletonString.replaceAll("<(B|b)(O|o)(D|d)(Y|y)>", "");
		    skeletonString = skeletonString.replaceAll("</(B|b)(O|o)(D|d)(Y|y)>", "");
	   }

	   return skeletonString;

	}
	
	public static void setHtmlParameters(final InputStream is, String mimeType) {
		
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		StringBuilder sb = new StringBuilder();
        String line;
        String contentAsString = null;
        
        try {
        	
			while ((line = br.readLine()) != null) {
			    sb.append(line);
			}
			logger.info("Writing input content ...");
			logger.info("Input content is:" + sb);   
			logger.info("End of input content writing.\n");
			
			contentAsString = sb.toString();
			boolean isHtmlMimeType = mimeType.equals(InternationalizationAPI.MIME_TYPE_HTML);
			
			Pattern pattern = Pattern.compile("<!DOCTYPE html>", Pattern.CASE_INSENSITIVE);
			Matcher matcher = pattern.matcher(contentAsString);
			boolean hasDocType = matcher.find();
			
			pattern = Pattern.compile("<HTML>|<HTML (.)*>", Pattern.CASE_INSENSITIVE);
			matcher = pattern.matcher(contentAsString);
			boolean hasHtml = matcher.find();
			
			pattern = Pattern.compile("<HEAD>|HEAD (.)*>", Pattern.CASE_INSENSITIVE);
			matcher = pattern.matcher(contentAsString);
			boolean hasHead = matcher.find();
			
			pattern = Pattern.compile("<BODY>|BODY (.)*>", Pattern.CASE_INSENSITIVE);
			matcher = pattern.matcher(contentAsString);
			boolean hasBody = matcher.find();
			
			if(isHtmlMimeType && !hasDocType){
				doctype = "NO_DOCTYPE";
			} else if(isHtmlMimeType){
				doctype = "DOCTYPE";
			}
			if(isHtmlMimeType && !hasHtml){
				html = "NO_HTML";
			} else if(isHtmlMimeType){
				html = "HTML";
			}
			if(isHtmlMimeType && !hasHead){
				head = "NO_HEAD";
			} else if(isHtmlMimeType){
				head = "HEAD";
			}
			if(isHtmlMimeType && !hasBody){
				body = "NO_BODY";
			} else if(isHtmlMimeType) {
				body = "BODY";
			}
			
        } catch (IOException exc) {
			exc.printStackTrace();
			logger.error("Error in input reading:" + exc.getLocalizedMessage());
			
		}
	}
	
	public static String getContentWithNwLineTag(String skel, String content) {
		
		String source = skel.replace("\t", "");
		source = source.replaceAll("\n", "");
		source = source.replaceAll("\r", "");
		source = source.replaceAll("<script(.*)>(.*)</script>", "");
		source = source.replaceAll("<style(.*)>(.*)</style>", "");
		source = source.replaceAll("<link(.*?)>", "");
		source = source.replaceAll("<meta(.*?)>", "");
		source = source.replaceAll("=\"(.*?)\"", "");
		
		for(NwLine nwl: NwLine.values()){
			
			String regex = "<" + nwl.getTagName() +"[\\s|>]";
			Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
			Matcher matcher = pattern.matcher(source);
			
			if(matcher.find()){
				content = "\n" + content;
			}
			
			regex = "([^\t]+)</" + nwl.getTagName() + ">$";
			pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
			matcher = pattern.matcher(source);
			if(matcher.find()){
				content = content + "\n";
			}
			
			regex = "</" + nwl.getTagName() + ">([^\t]+)";
			pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
			matcher = pattern.matcher(source);
			if(matcher.find()){
				content = "\n" + content;
			}
		}
		
		return content;
	}
	
	public static String unescapeXmlInScriptElements(String skel){
		
		String tbc = skel;
		Document doc = Jsoup.parse(skel);
		Elements scriptElements = doc.getElementsByTag("script");
		for (Element element :scriptElements ){   
			String oldHtml = element.html();
			String newHtml = StringEscapeUtils.unescapeXml(oldHtml);
			tbc = tbc.replace(oldHtml, newHtml);
		}
		
		return tbc;
	}
	
	public static String unescapeContentInStyleElements(String skel){
		
		String tbc = skel;
		Document doc = Jsoup.parse(skel);
		Elements styleElements = doc.getElementsByTag("style");
		for (Element element :styleElements ){   
			String oldHtml = element.html();
			String newHtml = StringEscapeUtils.unescapeXml(oldHtml);
			tbc = tbc.replace(oldHtml, newHtml);
		}
		
		return tbc;
	}
	
	public static boolean[] isScriptsAndStyles(String skel) {
		
		boolean[] positions = new boolean[skel.length()];
		// false means allowed; true means not allowed

		String scriptRegex= "<script(.*)>(.*)</script>";
		Pattern pattern = Pattern.compile(scriptRegex);
		Matcher matcher = pattern.matcher(skel);
		while(matcher.find()){
			String group = matcher.group();
			int endIndex = matcher.end();
			int startIndex = endIndex - group.length();
			for(int k = startIndex; k < endIndex; k++){
					positions[k] = true;
			}
		}
		
		String styleRegex = "<style(.*)>(.*)</style>";
		pattern = Pattern.compile(styleRegex);
		matcher = pattern.matcher(skel);
		while(matcher.find()){
			String group = matcher.group();
			int endIndex = matcher.end();
			int startIndex = endIndex - group.length();
			for(int k = startIndex; k < endIndex; k++){
					positions[k] = true;
			}
		}
		
		return positions;
		
	}
	
	public static boolean[] isAttributeContent(String skel) {
		
		boolean[] positions = new boolean[skel.length()];
		// false means allowed; true means not allowed

		String attributeRegex= "=\"(.*?)\"";
		Pattern pattern = Pattern.compile(attributeRegex);
		Matcher matcher = pattern.matcher(skel);
		while(matcher.find()){
			String group = matcher.group();
			int endIndex = matcher.end();
			int startIndex = endIndex - group.length();
			for(int k = startIndex; k < endIndex; k++){
					positions[k] = true;
			}
		}
		
		return positions;
		
	}
	
	public static String getRegex(String source) {
		
		String tbf = source.replace(")", "\\)").replace(".", "\\.").replace("(", "\\(");
		
		//Special cases
		boolean startsWith = tbf.startsWith(",")|| tbf.startsWith("\\.");
		boolean endsWith = tbf.endsWith(",") || tbf.endsWith(".");
		
		String regex = "";
		
		if(startsWith && endsWith){
			regex = tbf;
		} else if(startsWith && !endsWith){
			regex = tbf + "(?![a-zA-Z0-9])";
		} else if (!startsWith && endsWith){
			regex = "\\w*(?<![a-zA-Z0-9])" + tbf ;
		} else {
			regex = "\\w*(?<![a-zA-Z0-9])" + tbf + "(?![a-zA-Z0-9])";
		}
		
		return regex;
	}
	
}
