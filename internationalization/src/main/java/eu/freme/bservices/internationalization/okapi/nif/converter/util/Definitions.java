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
