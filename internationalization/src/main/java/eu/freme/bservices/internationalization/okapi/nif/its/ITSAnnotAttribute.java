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

/**
 * This class represents an attribute for an ITS annotation.
 */
public class ITSAnnotAttribute {

	/** String type constant. */
	public static final int STRING_TYPE = 0;
	
	/** The double type constant. */
	public static final int DOUBLE_TYPE = 1;
	
	/** The integer type constant. */
	public static final int INTEGER_TYPE = 2;
	
	/** The unsigned integer type constant. */
	public static final int UNISGNED_INTEGER_TYPE = 3;
	
	/** The boolean type constant. */
	public static final int BOOLEAN_TYPE = 4;
	
	/** The IRI string type constant. */
	public static final int IRI_STRING_TYPE = 5;
	
	/** The attribute name. */
	private String name;
	
	/** The attribute value. */
	private Object value;
	
	/** The attribute type. */
	private int type;

	/**
	 * Default constructor.
	 */
	public ITSAnnotAttribute() {
	
	}
	
	
	/**
	 * Constructor.
	 * @param name the name
	 * @param value the value
	 * @param type the type
	 */
	public ITSAnnotAttribute(String name, Object value, int type) {
		super();
		this.name = name;
		this.value = value;
		this.type = type;
	}


	/**
	 * Gets the attribute name.
	 * @return the attribute name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the attribute name.
	 * @param name the attribute name.
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Gets the attribute value.
	 * @return the attribute value.
	 */
	public Object getValue() {
		return value;
	}

	/**
	 * Sets the attribute value.
	 * @param value the attribute value.
	 */
	public void setValue(Object value) {
		this.value = value;
	}

	/**
	 * Gets the attribute type.
	 * @return the attribute type.
	 */
	public int getType() {
		return type;
	}

	/**
	 * Sets the attribute type.
	 * @param type the attribute type.
	 */
	public void setType(int type) {
		this.type = type;
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		
		return name + "=" + value;
	}
	
	
}
