/**
 * Copyright (C) 2015 Agro-Know, Deutsches Forschungszentrum für Künstliche Intelligenz, iMinds,
 * Institut für Angewandte Informatik e. V. an der Universität Leipzig,
 * Istituto Superiore Mario Boella, Tilde, Vistatec, WRIPL (http://freme-project.eu)
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
package eu.freme.bservices.internationalization.okapi.nif.filter;

import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.resource.Code;
import net.sf.okapi.common.resource.TextFragment;

import java.nio.charset.CharsetEncoder;
import java.text.Normalizer;
import java.util.List;

/**
 * Helper class for markers management in NIF Writer filter.
 */
public class NifMarkerHelper {

	/** The text enclosed into marker tag. */
	private String codedText;

	/** The list of codes contained in a text container. */
	private List<Code> codes;

	/** The charset encoder. */
	private CharsetEncoder chsEnc;

	/** States if an opened marker has been found. */
	private boolean markerOpened;

	/** Total length of parts analyzed so far. */
	private int totPartsLength;

	/** The current code. */
	private Code currentCode;

	/** The String containing the annotated text. */
	private StringBuilder annotatedText;

	/** The writer filter. */
	private NifWriterFilter writerFilter;

	/** The annotated text start index. */
	private int annotatedTextStartIdx;

	/**
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

	/**
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
	public String toString(TextFragment content, LocaleId locale,
			boolean isTarget) {
		codedText = content.getCodedText();
		codes = content.getCodes();
		annotatedText = new StringBuilder();
		annotatedTextStartIdx = -1;
		StringBuilder tmp = new StringBuilder();
		int index;
//		Code code;

		for (int i = 0; i < codedText.length(); i++) {
			
			switch (codedText.codePointAt(i)) {
			
				case TextFragment.MARKER_OPENING:
					index = TextFragment.toIndex(codedText.charAt(++i));
					currentCode = codes.get(index);
					String codeAsString = currentCode.toString();
					if(checkCode(codeAsString, "<canvas", false) || checkCode(codeAsString, "<iframe", false)){
						tmp.append("\n", 0, 1);
					}
					markerOpened = true;
					break;
				case TextFragment.MARKER_CLOSING:
					index = TextFragment.toIndex(codedText.charAt(++i));
					String stringCode = codes.get(index).toString();
					if(checkCode(stringCode, "</canvas>", true)|| checkCode(stringCode, "</iframe>", true)){
						tmp.append("\n");
					}
					markerOpened = false;
					manageInlineAnnotation(isTarget, locale);
					break;
				case TextFragment.MARKER_ISOLATED:
					index = TextFragment.toIndex(codedText.charAt(++i));
					Code codeFromIndex = codes.get(index);
					String codeString = codeFromIndex.toString();
					if(checkCode(codeString, "<br>", true)){
						tmp.append("\n");
					}
					if(checkCode(codeString, "<hr", false) || checkCode(codeString, "<img", false)){
						tmp.append("\n");
						tmp.append("\n", 0,1);
					}
					markerOpened = false;
					manageInlineAnnotation(isTarget, locale);
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

	/**
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

	/**
	 * Manages the in line annotations for the current code.
	 * 
	 * @param isTarget
	 *            boolean stating if it's content from the target
	 * @param locale
	 *            the locale.
	 */
	private void manageInlineAnnotation(boolean isTarget, LocaleId locale) {

		if (currentCode != null && annotatedText.length() > 0) {
			if (currentCode.getGenericAnnotations() != null) {
				String normalizedString = annotatedText.toString();
				if (!Normalizer.isNormalized(normalizedString,
						Normalizer.Form.NFC)) {
					normalizedString = Normalizer.normalize(normalizedString,
							Normalizer.Form.NFC);
				}
				writerFilter.createResourceForInlineAnnotation(
						normalizedString, annotatedTextStartIdx
								+ totPartsLength, locale, isTarget,
						currentCode.getGenericAnnotations());
//				for (GenericAnnotation annot : currentCode
//						.getGenericAnnotations()) {
//					System.out.println(annot.toString());
//				}
			}
		}
		currentCode = null;
		annotatedText = new StringBuilder();
		annotatedTextStartIdx = -1;
	}
	
	/**
	 * Check if the current code string starts with the specified text if equalityCheck is false,
	 * and if it is equal to the specified text if equalityCheck is true
	 * @param code the html tag code as found in the input html source
	 * @param text text to be matched
	 * @param equalityCheck if true an equality check is done otherwise the initial content of code will be checked
	 * @return
	 */
	private boolean checkCode(String code, String text, boolean equalityCheck) {
		return equalityCheck?code.equalsIgnoreCase(text):code.toLowerCase().startsWith(text);
		
	}
}
