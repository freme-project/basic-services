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


public class Definitions {
	
	public enum NwLine {
		
		H1("h1"), H2("h2"), H3("h3"), H4("h4"), H5("h5"), H6("h6"), P("p"), DIV("div"), PRE("pre")
		,TITLE("title"), ARTICLE("article"), SECTION("section"), ADDRESS("address"),FOOTER("footer")
		, TABLE("table"), TR("tr"), TD("td"), THEAD("thead"), TBODY("tbody"), TH("th"), CAPTION("caption") 
		, UL("ul"), LI("li"), OL("ol"), DL ("dl"), DT("dt"), DD("dd");
		 
		private final String tagName;
		
		private NwLine(String tagName){
			this.tagName = tagName;
		}
		
		public String getTagName() {
			return tagName;
		}
		
	}

}
