package eu.freme.bservices.internationalization.okapi.nif.converter.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

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
			}
			if(isHtmlMimeType && !hasHtml){
				html = "NO_HTML";
			}
			if(isHtmlMimeType && !hasHead){
				head = "NO_HEAD";
			}
			if(isHtmlMimeType && !hasBody){
				body = "NO_BODY";
			}
			
        } catch (IOException exc) {
			exc.printStackTrace();
			logger.error("Error in input reading:" + exc.getLocalizedMessage());
			
		}
	}
	
	public static String getContentWithNwLineTag(String skel, String content) {
		
		String source = skel.replace("\t", "");
		
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
	
}