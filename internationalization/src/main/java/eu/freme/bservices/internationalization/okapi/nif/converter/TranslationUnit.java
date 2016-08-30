package eu.freme.bservices.internationalization.okapi.nif.converter;

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
