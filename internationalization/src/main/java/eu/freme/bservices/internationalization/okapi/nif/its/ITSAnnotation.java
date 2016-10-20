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
package eu.freme.bservices.internationalization.okapi.nif.its;

import java.util.List;

/**
 * This class represents an ITS annotation.
 */
public class ITSAnnotation {

	/** The annotation type. */
	private String type;
	
	/** The list of attributes. */
	private List<ITSAnnotAttribute> attributes;

	/**
	 * Constructor.
	 * @param type the annotation type.
	 */
	public ITSAnnotation(String type) {
		
		this.type = type;
	}
	
	/**
	 * Default constructor.
	 */
	public ITSAnnotation() {
	}

	/**
	 * Gets the annotation type.
	 * @return the annotation type.
	 */
	public String getType() {
		return type;
	}

	/**
	 * Sets the annotation type.
	 * @param type the annotation type.
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * Gets the attributes.
	 * @return the attributes
	 */
	public List<ITSAnnotAttribute> getAttributes() {
		return attributes;
	}

	/**
	 * Sets the attributes
	 * @param attributes the attributes
	 */
	public void setAttributes(List<ITSAnnotAttribute> attributes) {
		this.attributes = attributes;
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		
		StringBuilder retString = new StringBuilder(type);
		if(attributes != null){
			retString.append(" - [");
			for(ITSAnnotAttribute attr: attributes){
				retString.append(attr.toString());
				if(attributes.indexOf(attr) < attributes.size() - 1){
					retString.append(", ");
				}
			}
			retString.append("]");
		}
		
		return retString.toString();
	}
	
}
