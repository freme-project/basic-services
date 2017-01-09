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
package eu.freme.bservices.internationalization.okapi.nif.filter;

import java.nio.charset.CharsetEncoder;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.annotation.GenericAnnotation;
import net.sf.okapi.common.annotation.GenericAnnotationType;
import net.sf.okapi.common.annotation.GenericAnnotations;
import net.sf.okapi.common.resource.Code;
import net.sf.okapi.common.resource.TextFragment;

/*
 * Helper class for markers management in NIF Writer filter.
 */
public class NifMarkerHelper {

	/* The text enclosed into marker tag. */
	private String codedText;

	/* The list of codes contained in a text container. */
	private List<Code> codes;

	/* The charset encoder. */
	private CharsetEncoder chsEnc;

	/* States if an opened marker has been found. */
	private boolean markerOpened;

	/* Total length of parts analyzed so far. */
	private int totPartsLength;

	/* The current code. */
	private Code currentCode;

	/* The String containing the annotated text. */
	private StringBuilder annotatedText;

	/* The writer filter. */
	private NifWriterFilter writerFilter;

	/* The annotated text start index. */
	private int annotatedTextStartIdx;
	
	/* The list of the current codes. */
	List<Code> currentCodes;

	/*
	 * Constructor.
	 * 
	 * @param writerFilter
	 *            the writer filter.
	 */
	public NifMarkerHelper(final NifWriterFilter writerFilter) {

		this.writerFilter = writerFilter;
		// chsEnc = new DummyEncoder();
	}

	public void clear() {

		totPartsLength = 0;
	}

	/*
	 * Retrieves text from a text fragment. At the moment all the markers are
	 * discarded... TO BE CONTINUED
	 * 
	 * @param content
	 *            the text fragment.
	 * @param locale
	 *            the locale
	 * @param a
	 *            boolean stating if this content is from the target.
	 * @return the text contained into this text fragment.
	 */
	/*
	 * @param content
	 * @param locale
	 * @param isTarget
	 * @return
	 */
	/*
	 * @param content
	 * @param locale
	 * @param isTarget
	 * @return
	 */
	public String toString(TextFragment content, LocaleId locale,
			boolean isTarget) {
		
		codedText = content.getCodedText();
		codes = content.getCodes();
		currentCodes = new ArrayList<Code>();
		
		annotatedText = new StringBuilder();
		annotatedTextStartIdx = -1;
		
		StringBuilder tmp = new StringBuilder();
		
		int index;
		
		for (int i = 0; i < codedText.length(); i++) {
			int codePointAt = codedText.codePointAt(i);
			switch (codePointAt) {
			
				case TextFragment.MARKER_OPENING:
					
					index = TextFragment.toIndex(codedText.charAt(++i));
					currentCode = codes.get(index);
					currentCodes.add(currentCode);
					
					String codeAsString = currentCode.toString();
					handleNewLines(TextFragment.MARKER_OPENING, codeAsString, tmp);
					markerOpened = true;
					break;
				case TextFragment.MARKER_CLOSING:
					
					index = TextFragment.toIndex(codedText.charAt(++i));
					String stringCode = codes.get(index).toString();
					handleNewLines(TextFragment.MARKER_CLOSING, stringCode, tmp);
					markerOpened = false;
					manageInlineAnnotation(isTarget, locale);
					break;
				case TextFragment.MARKER_ISOLATED:
					
					index = TextFragment.toIndex(codedText.charAt(++i));
					Code codeFromIndex = codes.get(index);
					String codeString = codeFromIndex.toString();
					handleNewLines(TextFragment.MARKER_ISOLATED, codeString, tmp);
					
					if(codeFromIndex.getTagType().equals(TextFragment.TagType.PLACEHOLDER) 
							&& codeFromIndex.getType().equals("null") && hasTranslateAttribute(currentCodes)){
						
						if (annotatedText.length() == 0) {
							annotatedTextStartIdx = tmp.length();
						}
						annotatedText.append(codeString);
						tmp.append(codeString);
						codeFromIndex.setType("translate-attribute-content");
					} else {
						if(codeFromIndex.getTagType().equals(TextFragment.TagType.PLACEHOLDER) 
								&& codeFromIndex.getType().equals("null")){
							tmp.append(codeString);
						}
						markerOpened = false;
						manageInlineAnnotation(isTarget, locale);
					}
					break;
				case '>':
					tmp.append(codedText.charAt(i));
					break;
				case '\r': // Not a line-break in the XML context, but a literal
					tmp.append("&#13;");
					break;
				case '<':
					tmp.append("&lt;");
					break;
				case '&':
					tmp.append("&amp;");
					break;
				case '"':
					break;
				case '\'':
					tmp.append(codedText.charAt(i));
					break;
				default:
					if (codedText.charAt(i) > 127) { // Extended chars
						if (Character.isHighSurrogate(codedText.charAt(i))) {
							int cp = codedText.codePointAt(i++);
							String buf = new String(Character.toChars(cp));
							if ((chsEnc != null) && !chsEnc.canEncode(buf)) {
								tmp.append(String.format("&#x%X;", cp));
							} else {
								tmp.append(buf);
							}
						} else {
							if ((chsEnc != null)
									&& !chsEnc.canEncode(codedText.charAt(i))) {
								tmp.append(String.format("&#x%04X;",
										codedText.codePointAt(i)));
							} else { // No encoder or char is supported
								tmp.append(codedText.charAt(i));
							}
						}
					} else { // ASCII chars
						if (markerOpened) {
							if (annotatedText.length() == 0) {
								annotatedTextStartIdx = tmp.length();
							}
							annotatedText.append(codedText.charAt(i));
						}
						tmp.append(codedText.charAt(i));
					}
					break;
				}
		}
		saveTotLength(tmp.toString());
		return tmp.toString();
	}

	/*
	 * Stores the total length of the parts examined so far.
	 * 
	 * @param text
	 *            the text
	 */
	private void saveTotLength(String text) {

		if (!Normalizer.isNormalized(text, Normalizer.Form.NFC)) {
			text = Normalizer.normalize(text, Normalizer.Form.NFC);
		}
		totPartsLength = text.length();
	}

	/*
	 * Manages the in line annotations for the current code.
	 * 
	 * @param isTarget
	 *            boolean stating if it's content from the target
	 * @param locale
	 *            the locale.
	 */
	private void manageInlineAnnotation(boolean isTarget, LocaleId locale) {
		
		GenericAnnotation genericAnnotation = null;
		
		if (currentCode != null && annotatedText.length() > 0) {
			
			if(hasTranslateAttribute(currentCodes)){
				
				genericAnnotation = 
						new GenericAnnotation(GenericAnnotationType.TRANSLATE);
				if(hasTranslateAttributeWithValueNo(currentCodes)){
					genericAnnotation.setBoolean(GenericAnnotationType.TRANSLATE_VALUE, Boolean.FALSE);
				} else {
					genericAnnotation.setBoolean(GenericAnnotationType.TRANSLATE_VALUE, Boolean.TRUE);
				}
				
			}
			
			if ( currentCode.getGenericAnnotations()!= null || genericAnnotation != null ){
				String normalizedString = annotatedText.toString();
				if (!Normalizer.isNormalized(normalizedString,
						Normalizer.Form.NFC)) {
					normalizedString = Normalizer.normalize(normalizedString,
							Normalizer.Form.NFC);
				}
				
				GenericAnnotations genericAnnotations = currentCode.getGenericAnnotations();
				if(genericAnnotations != null){
					genericAnnotations.add(genericAnnotation);
				} else {
					genericAnnotations = new GenericAnnotations();
					genericAnnotations.add(genericAnnotation);
				}
				
				
				writerFilter.createResourceForInlineAnnotation(
						normalizedString, annotatedTextStartIdx
								+ totPartsLength, locale, isTarget,
								genericAnnotations);
			}
		}
		currentCode = null;
		annotatedText = new StringBuilder();
		annotatedTextStartIdx = -1;
	}
	
	/*
	 * @param marker Could be one between TextFragment.MARKER_OPENING,TextFragment.MARKER_CLOSING,
	 * TextFragment.MARKER_ISOLATED
	 * @param codeAsString The Code object converted into a string
	 * @param sb The StringBuilder object to append or prepend the newline to
	 */
	private void handleNewLines(int marker, String codeAsString, StringBuilder sb){
		
		switch(marker){
			
			case TextFragment.MARKER_OPENING:
				
				if( codeAsString.toLowerCase().startsWith( "<canvas") 
						|| codeAsString.toLowerCase().startsWith("<iframe") ){
					sb.append("\n", 0, 1);
				}
				break;
			case TextFragment.MARKER_CLOSING:
				if( codeAsString.equalsIgnoreCase("</canvas>")
						|| codeAsString.equalsIgnoreCase("</iframe>")){
					sb.append("\n");
				}
				break;
			case TextFragment.MARKER_ISOLATED:
				if( codeAsString.equalsIgnoreCase("<br>") ){
					sb.append("\n");
				}
				if( codeAsString.toLowerCase().startsWith("<hr") 
						|| codeAsString.toLowerCase().startsWith("<img") ){
					sb.append("\n");
					sb.append("\n", 0,1);
				}
				break;
			default:
					
		}
	}
	
	/*
	 * Check if one of the opened tags has the "translate" attribute 
	 * @param currentCodes the codes associated to the opened tags
	 * @return true if one of the opened tags has the translate attribute
	 */
	private boolean hasTranslateAttribute(List<Code> currentCodes){
		
		for(Code codej:currentCodes){
			boolean hasTranslateAttribute = codej != null && (codej.toString().toLowerCase().contains("translate=\"no\"")
					|| codej.toString().toLowerCase().contains("translate=\"yes\""));
			if(hasTranslateAttribute){
				return hasTranslateAttribute;
			}
		}
		return false;
		
	}
	
	/*
	 * @param currentCodes the codes corresponding to the opened tags 
	 * @return true if one of the opened tags has the translate="no" attribute
	 */
	private boolean hasTranslateAttributeWithValueNo(List<Code> currentCodes){
		
		for(Code codej:currentCodes){
			boolean hasTranslateAttribute = codej != null 
					&& codej.toString().toLowerCase().contains("translate=\"no\"");
			if(hasTranslateAttribute){
				return hasTranslateAttribute;
			}
		}
		return false;
		
	}
	
}
