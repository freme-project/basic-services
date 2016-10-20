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

/**
 * @author KatiaI
 * 
 * class representing the segment for translation
 */
public class TranslationUnit {
	
	private String source;
	private String target;
	private int startIndex;
	private int endIndex;
	private int skelIndex;
	private int skelEndIndex;
	
	public TranslationUnit() {}

	public TranslationUnit(String source, String target, int startIndex,
			int endIndex, int skelIndex, int skelEndIndex) {
		
		this.source = source;
		this.target = target;
		this.startIndex = startIndex;
		this.endIndex = endIndex;
		this.skelIndex = skelIndex;
		this.skelEndIndex = skelEndIndex;
	}
	
	public String getSource() {
		return source;
	}
	
	public void setSource(String source) {
		this.source = source;
	}
	
	public String getTarget() {
		return target;
	}
	
	public void setTarget(String target) {
		this.target = target;
	}
	
	public int getStartIndex() {
		return startIndex;
	}
	
	public void setStartIndex(int startIndex) {
		this.startIndex = startIndex;
	}
	
	public int getEndIndex() {
		return endIndex;
	}
	
	public void setEndIndex(int endIndex) {
		this.endIndex = endIndex;
	}
	
	public int getSkelIndex() {
		return skelIndex;
	}

	public void setSkelIndex(int skelIndex) {
		this.skelIndex = skelIndex;
	}
	
	public int getSkelEndIndex() {
		return skelEndIndex;
	}

	public void setSkelEndIndex(int skelEndIndex) {
		this.skelEndIndex = skelEndIndex;
	}

	@Override
	public String toString() {
		return "TranslationUnit [source=" + source + ", target=" + target
				+ ", startIndex=" + startIndex + ", endIndex=" + endIndex
				+ ", skelIndex=" + skelIndex + ", skelEndIndex=" + skelEndIndex
				+ "]";
	}

}
