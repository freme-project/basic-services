/**
 * Copyright (C) 2015 Deutsches Forschungszentrum für Künstliche Intelligenz (http://freme-project.eu)
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

import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;

import org.springframework.util.StringUtils;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.resource.BaseNameable;
import net.sf.okapi.common.resource.DocumentPart;
import net.sf.okapi.common.resource.Ending;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.StartDocument;
import net.sf.okapi.common.resource.TextPart;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

import eu.freme.bservices.internationalization.okapi.nif.converter.util.NifConverterUtil;
import eu.freme.bservices.internationalization.okapi.nif.step.NifParameters;

/**
 * Writer filter for NIF Skeleton files. The NIF skeleton file is a NIF file
 * containing two separated contexts: one context containing the whole original
 * file text (including in line markups); the second context only contains plain
 * text free from any markups. Then a NIF resource is created for each plain
 * text chunk with the following properties:
 * <ul>
 * <li>a referenceContext prop, stating the reference context (the plain text
 * context)</li>
 * <li>a wasConvertedFrom prop, stating the exact offset into the skeleton
 * context (the one containing markups).</li>
 * <ul>
 */
public class NifSkeletonWriterFilter extends AbstractNifWriterFilter {

	/** Suffix for skeleton context URI. */
	private final static String CONTEXT1_URI_DOC = "doc1/";

	// private final static String CONTEXT2_URI_DOC = "doc2/";
	/** Suffix for the plain text context URI. */
	private final static String CONTEXT2_URI_DOC = "";

	/** The marker helper. */
	private NifSkeletonMarkerHelper markerHelper;

	/** The skeleton map. */
	private LinkedHashMap<String, String> skeletonMap;

	/** The list of text units. */
	List<TextUnitInfo> textUnitList;

	/**
	 * Constructor.
	 * 
	 * @param params
	 *            the parameters.
	 * @param sourceLocale
	 *            the source locale.
	 */
	public NifSkeletonWriterFilter(NifParameters params, LocaleId sourceLocale) {
		super(params, sourceLocale);
		markerHelper = new NifSkeletonMarkerHelper();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.sf.okapi.common.filterwriter.IFilterWriter#handleEvent(net.sf.okapi
	 * .common.Event)
	 */
	@Override
	public Event handleEvent(Event event) {
		switch (event.getEventType()) {
		case START_DOCUMENT:
			processStartDocument((StartDocument) event.getResource());
			break;
		case END_DOCUMENT:
			processEndDocument(event.getEnding());
			break;
		case TEXT_UNIT:
			processTextUnit(event.getTextUnit());
			break;
		case START_SUBDOCUMENT:
		case END_SUBDOCUMENT:
			break;
		case DOCUMENT_PART:
			if (event.getResource().getSkeleton() != null) {
				processDocumentPart((DocumentPart) event.getResource());
			}
			break;
		case START_GROUP:
		case START_SUBFILTER:
		case END_GROUP:
		case END_SUBFILTER:
			break;
		default:
			// Do nothing
			break;
		}
		return event;
	}

	/**
	 * Processes the document part.
	 * 
	 * @param docPart
	 *            the document part.
	 */
	public void processDocumentPart(DocumentPart docPart) {
		processSkeleton(docPart);
	}

	/**
	 * Processes the skeleton contained into the resource passed as parameter.
	 * 
	 * @param docResource
	 *            the resource to be processed.
	 */
	private void processSkeleton(BaseNameable docResource) {
		if (docResource.getSkeleton() != null) {
			String skeletonString = docResource.getSkeleton().toString();
			int propNameStartIdx = -1;
			int propNameEndIdx = -1;
			// Sets all the properties into the skeleton string.
			while (skeletonString
					.contains(SkeletonConstants.PROP_STRING_PREFIX)) {
				int propStringStartIdx = skeletonString
						.indexOf(SkeletonConstants.PROP_STRING_PREFIX);
				propNameStartIdx = propStringStartIdx
						+ SkeletonConstants.PROP_STRING_PREFIX.length();
				propNameEndIdx = skeletonString.substring(propNameStartIdx)
						.indexOf("]") + propNameStartIdx;
				String propertyName = skeletonString.substring(
						propNameStartIdx, propNameEndIdx);
				String propertyValue = getPropertyValue(docResource,
						propertyName);
				if (propertyValue != null) {
					skeletonString = skeletonString.substring(0,
							propStringStartIdx)
							+ propertyValue
							+ skeletonString.substring(propNameEndIdx + 1);
				}
			}

			skeletonString = skeletonString.replace(
					SkeletonConstants.STANDOFF_STRING_PREFIX, "");

			skeletonString = NifConverterUtil.getShortenedSkeletonString(skeletonString, params);
			
			
			// check if the HTML snippet has a doctype
			boolean startDocumentWithDoctype = docResource instanceof StartDocument && params.getDoctype().equals("DOCTYPE");
			boolean documentPartWithHtml = docResource instanceof DocumentPart && params.getHtml().equals("HTML");
			boolean documentPartWithHead = docResource instanceof DocumentPart && params.getHead().equals("HEAD");
			// put the skeleton in the skeleton map
			if(startDocumentWithDoctype || documentPartWithHtml || documentPartWithHead) {
				skeletonMap.put(docResource.getId(), skeletonString);
			}
		}
	}

	/**
	 * Gets the value for the resource property having a specific name.
	 * 
	 * @param docResource
	 *            the document resource
	 * @param propName
	 *            the property name
	 * @return the property value if it exists: <code>null</code> otherwise.
	 */
	private String getPropertyValue(BaseNameable docResource, String propName) {

		String propValue = null;
		if (docResource.getPropertyNames() != null
				&& docResource.getPropertyNames().contains(propName)) {
			propValue = docResource.getProperty(propName).getValue();
		} else if (docResource.getSourcePropertyNames() != null
				&& docResource.getSourcePropertyNames().contains(propName)) {
			propValue = docResource.getSourceProperty(propName).getValue();
		}
		return propValue;
	}

	/**
	 * Processes the text unit passed as parameter.
	 * 
	 * @param textUnit
	 *            the text unit.
	 */
	public void processTextUnit(ITextUnit textUnit) {

		String skeletonString = "";
		if (textUnit.getSkeleton() != null) {
			skeletonString = textUnit.getSkeleton().toString();
		}
		
		// For HTML snippet remove tags when not originally contained in the snippet:html,head,body
		skeletonString = NifConverterUtil.getShortenedSkeletonString(skeletonString, params);
				
		for (TextPart part : textUnit.getSource().getParts()) {
			markerHelper.manageCodes(part.getContent(), textUnit.getId(),
					skeletonMap, textUnitList, skeletonString,
					textUnit.getType());
		}

	}

	/**
	 * Once the whole document has been processed and the skeleton map has been
	 * filled, then all the pointers contained in the skeleton strings are
	 * resolved. Indeed a skeleton string could exist containing the string
	 * "[#$ID]" where ID is the ID of a text unit. if a skeleton string like
	 * that is found, then the pointer is replaced with the proper text unit
	 * from the text info list.
	 */
	private void resolvePointers() {
		List<String> keyToDelete = new ArrayList<String>();
		List<String> newMapOrder = new ArrayList<String>();
		for (Entry<String, String> skelEntry : skeletonMap.entrySet()) {
			newMapOrder.add(skelEntry.getKey());
		}
		for (Entry<String, String> skelEntry : skeletonMap.entrySet()) {
			int startPointerIdx = -1;
			int endPointerIdx = -1;
			// int idCounter = 1;
			List<TextUnitInfo> tuInfoList = getTextUnitInfoListForSkel(skelEntry
					.getKey());
			while (skelEntry.getValue().contains(
					SkeletonConstants.POINTER_STRING_PREFIX)) {
				startPointerIdx = skelEntry.getValue().indexOf(
						SkeletonConstants.POINTER_STRING_PREFIX);
				endPointerIdx = skelEntry.getValue().substring(startPointerIdx)
						.indexOf("]") // TODO +1: "]" LENGTH
						+ startPointerIdx;
				String pointedId = skelEntry
						.getValue()
						.substring(
								startPointerIdx
										+ SkeletonConstants.POINTER_STRING_PREFIX
												.length(),
								endPointerIdx);
				TextUnitInfo pointedUnitInfo = findFirstTextUnitById(pointedId);
				if (pointedUnitInfo != null) {
					pointedUnitInfo.setOffset(pointedUnitInfo.getOffset()
							+ startPointerIdx);
					pointedUnitInfo.setTuId(skelEntry.getKey()/*
															 * + "-" +
															 * (idCounter++)
															 */);

					if(pointedUnitInfo.isIncludeInContext()){
						changeMapOrder(newMapOrder, pointedId, skelEntry.getKey());
					}
				}
				String valuePointedFromSkeleton = findSkeletonReplaceValue(
						pointedId, keyToDelete);
				if (tuInfoList != null && !tuInfoList.isEmpty()) {
					int pointerStringLength = endPointerIdx - startPointerIdx
							+ 1;
					for (TextUnitInfo tu : tuInfoList) {
						tu.setOffset(tu.getOffset() - pointerStringLength
								+ valuePointedFromSkeleton.length());
					}
				}

				String newValue = skelEntry.getValue().substring(0,
						startPointerIdx)
						+ valuePointedFromSkeleton
						+ skelEntry.getValue().substring(endPointerIdx + 1);
				skelEntry.setValue(newValue);
			}
		}
		for (String key : keyToDelete) {
			skeletonMap.remove(key);
		}
		LinkedHashMap<String, String> newSkeletonMap = new LinkedHashMap<String, String>();
		for (String skelKey : newMapOrder) {
			if (skeletonMap.containsKey(skelKey)) {
				newSkeletonMap.put(skelKey, skeletonMap.get(skelKey));
			}
		}
		skeletonMap = newSkeletonMap;
	}

	private void changeMapOrder(List<String> newMapOrder, String replacedId,
			String idToMove) {

		if (newMapOrder.contains(replacedId)) {
			newMapOrder.remove(idToMove);
			newMapOrder.add(newMapOrder.indexOf(replacedId), idToMove);
		} else {
			boolean found = false;
			int index = 0;
			while (index < newMapOrder.size() && !found) {
				found = newMapOrder.get(index).startsWith(replacedId + "-");
				index++;
			}
			if (found) {
				String valueToReplace = newMapOrder.get(index - 1);
				newMapOrder.remove(idToMove);
				newMapOrder.add(newMapOrder.indexOf(valueToReplace), idToMove);
			}
		}
	}

	/**
	 * Finds the first text unit associated to a specific ID. Sometimes the text
	 * unit is split in multiple texts. In that case, each text is associated to
	 * the text unit id concatenated to the dash symbol and an incremental index
	 * (example: The ID0 tu1 becomes tu1-1, tu1-2, etc.)
	 * 
	 * @param tuId
	 *            the text unit ID
	 * @return the first text unit associated to that ID.
	 */
	private TextUnitInfo findFirstTextUnitById(final String tuId) {

		TextUnitInfo unitInfo = getTextUnitInfo(tuId);
		if (unitInfo == null) {
			unitInfo = getTextUnitInfo(tuId + "-" + 1);
		}
		return unitInfo;
	}

	/**
	 * Retrieves the skeleton text from the map, corresponding to a specific ID.
	 * If many incremental ID exist for that ID, then all those texts are
	 * concatenated.
	 * 
	 * @param skeletonId
	 *            the skeleton ID
	 * @param keyToDelete
	 *            the list containing all the skeleton keys to be deleted.
	 * @return the string from the skeleton map
	 */
	private String findSkeletonReplaceValue(final String skeletonId,
			final List<String> keyToDelete) {

		StringBuilder replaceString = new StringBuilder();
		if (skeletonMap.containsKey(skeletonId)) {
			replaceString.append(skeletonMap.get(skeletonId));
			keyToDelete.add(skeletonId);
		} else {
			int idCount = 1;
			String newSkeletonId = skeletonId + "-" + idCount;
			while (skeletonMap.containsKey(newSkeletonId)) {
				replaceString.append(skeletonMap.get(newSkeletonId));
				keyToDelete.add(newSkeletonId);
				newSkeletonId = skeletonId + "-" + (++idCount);
			}
		}
		return replaceString.toString();
	}

	/**
	 * Builds the model that will define the NIF file.
	 */
	private void buildNIFFile() {

		try {
			initModel();

			StringBuilder context1 = new StringBuilder();
			for (Entry<String, String> skelEntry : skeletonMap.entrySet()) {
				List<TextUnitInfo> tuInfoList = getTextUnitInfoListForSkel(skelEntry
						.getKey());

				for (TextUnitInfo tuInfo : tuInfoList) {
					tuInfo.setOffset(tuInfo.getOffset() + context1.length());
				}
				// tuinfo = getTextUnitInfo(skelEntry.getKey());
				// if (tuinfo != null) {
				// tuinfo.setOffset(tuinfo.getOffset() + context1.length());
				// }
				context1.append(skelEntry.getValue());
			}
			createContextResource(uriPrefix + CONTEXT1_URI_DOC,
					context1.toString());

			StringBuilder context2 = new StringBuilder();
			int lastTuIdSet = 0;
			for (TextUnitInfo currTextInfo : textUnitList) {
				if (currTextInfo.isIncludeInContext()) {

					if (lastTuIdSet != 0
							&& lastTuIdSet != currTextInfo.getTextUnitSet()) {
						context2.append(" ");
					}
					currTextInfo.setOnlyTextOffset(context2.length());
					context2.append(currTextInfo.getText());
					lastTuIdSet = currTextInfo.getTextUnitSet();
				}
			}

			Resource context2Resource = createContextResource(uriPrefix
					+ CONTEXT2_URI_DOC, context2.toString());

			for (TextUnitInfo currTextInfo : textUnitList) {

				if (currTextInfo.isIncludeInContext()) {
					createUnitResource(uriPrefix + CONTEXT2_URI_DOC,
							context2Resource.getURI(), uriPrefix
									+ CONTEXT1_URI_DOC, currTextInfo);
				}
			}
			model.write(new OutputStreamWriter(System.out), "TTL");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private List<TextUnitInfo> getTextUnitInfoListForSkel(String key) {

		List<TextUnitInfo> tuInfoList = new ArrayList<TextUnitInfo>();
		for (TextUnitInfo tuInfo : textUnitList) {
			if (tuInfo.getTuId().equals(key)) {
				tuInfoList.add(tuInfo);
			}
		}
		Collections.sort(tuInfoList, new TextUnitInfoComparator());
		return tuInfoList;
	}

	/**
	 * Create a text unit resource.
	 * 
	 * @param uriPrefix
	 *            the URI prefix
	 * @param realContextRefUri
	 *            the actual context URI
	 * @param convertedFromUri
	 *            the skeleton context URI
	 * @param tuInfo
	 *            the text info
	 */
	private void createUnitResource(final String uriPrefix,
			final String realContextRefUri, final String convertedFromUri,
			final TextUnitInfo tuInfo) {

		String nifVersion = params.getNifVersion();
		
		boolean isNif20 = StringUtils.isEmpty(nifVersion) || nifVersion.equals("2.0");
		String uriOffset = isNif20?URI_CHAR_OFFSET:URI_OFFSET;
		String splitter = isNif20?",":"_";
		String nifPrefix = isNif20?RDFConstants.nifPrefix_2_0:RDFConstants.nifPrefix_2_1;
		
		Resource tuResource = model.createResource(uriPrefix + uriOffset
				+ tuInfo.getOnlyTextOffset() + splitter
				+ (tuInfo.getOnlyTextOffset() + tuInfo.getText().length()));

		// adds following NIF types: String and RFC5147String/OffsetBasedString depending on the value of nifVersion
		Property type = model.createProperty(RDFConstants.typePrefix);
		tuResource.addProperty(type,
				model.createResource(nifPrefix + "String"));
		String nifStringsIdentifier = isNif20?"RFC5147String":"OffsetBasedString";
		tuResource.addProperty(type,model.createResource(nifPrefix + nifStringsIdentifier));

		// adds the text with the anchorOf property
		Property anchorOf = model.createProperty(nifPrefix,"anchorOf");
		if(isNif20){
			Literal anchorOfTypedLiteral = model.createTypedLiteral(tuInfo.getText(), XSDDatatype.XSDstring);
			tuResource.addProperty(anchorOf, anchorOfTypedLiteral);
		} else {
			if (sourceLocale != null) {
				tuResource.addProperty(anchorOf, tuInfo.getText(),
						sourceLocale.getLanguage());
			} else {
				tuResource.addProperty(anchorOf, tuInfo.getText());
			}
		}
		
		// adds start and end index properties
		Literal beginIndex = model.createTypedLiteral(
				new Integer(tuInfo.getOnlyTextOffset()),
				XSDDatatype.XSDnonNegativeInteger);
		tuResource.addProperty(
				model.createProperty(nifPrefix + "beginIndex"),
				beginIndex);
		Literal endIndex = model.createTypedLiteral(
				new Integer(tuInfo.getOnlyTextOffset()
						+ tuInfo.getText().length()),
				XSDDatatype.XSDnonNegativeInteger);
		tuResource.addProperty(
				model.createProperty(nifPrefix + "endIndex"),
				endIndex);

		// Adds the text unit ID by using the identifier property
		Property identifier = model.createProperty(RDFConstants.dcPrefix
				+ "identifier");
		tuResource.addProperty(identifier,
				model.createLiteral(tuInfo.getTuId()));

		Property convertedFrom = model.createProperty(nifPrefix
				+ "wasConvertedFrom");
		
		tuResource.addProperty(convertedFrom,
				model.createResource(convertedFromUri + uriOffset
						+ tuInfo.getOffset() + splitter
						+ (tuInfo.getOffset() + tuInfo.getText().length())));
		

		Property refContext = model.createProperty(nifPrefix,
				"referenceContext");
		tuResource.addProperty(refContext,
				model.createResource(realContextRefUri));

	}

	/**
	 * Creates a context resource.
	 * 
	 * @param uriPrefix
	 *            the URI prefix
	 * @param text
	 *            the text
	 * @return the created resource
	 */
	private Resource createContextResource(final String uriPrefix,
			final String text) {
		
		String nifVersion = params.getNifVersion();
		boolean isNif20 = StringUtils.isEmpty(nifVersion) || nifVersion.equals("2.0");
		String nifPrefix = isNif20?RDFConstants.nifPrefix_2_0:RDFConstants.nifPrefix_2_1;
		String uriOffset = isNif20?URI_CHAR_OFFSET:URI_OFFSET;
		String splitter = isNif20?",":"_";
		String nifStringsIdentifier =isNif20?"RFC5147String":"OffsetBasedString";

		Resource contextRes = model.createResource(uriPrefix + uriOffset
				+ "0" + splitter + text.length());
		
		// Adds following types: String, Context, RFC5147String/OffsetBasedString depending on nif version 2.0/2.1
		Property type = model.createProperty(RDFConstants.typePrefix);
		contextRes.addProperty(type,
				model.createResource(nifPrefix + "String"));
		contextRes.addProperty(type,
				model.createResource(nifPrefix + "Context"));
		contextRes.addProperty(type,model.createResource(nifPrefix + nifStringsIdentifier));
		// Adds the text with the isString property
		if (text.length() > 0) {
			Property isStringProperty = model.createProperty(RDFConstants.nifPrefix	+ "isString");
			if(isNif20){ 
				if ( sourceLocale != null ) {
					contextRes.addProperty(isStringProperty, text, sourceLocale.getLanguage());
				} else {
					contextRes.addProperty(isStringProperty, text);
				}
			} else { // nif 2.1 version
				Literal anchorOfTypedLiteral = model.createTypedLiteral(text, XSDDatatype.XSDstring);
				contextRes.addProperty(isStringProperty, anchorOfTypedLiteral);
				
				if(sourceLocale != null ){
					Property predLangProperty = model.createProperty(nifPrefix	+ "predLang");
					Locale loc = new Locale(sourceLocale.getLanguage());
					String iso3Language = loc.getISO3Language();
					contextRes.addProperty(predLangProperty, model.createResource(RDFConstants.isolangPrefix + iso3Language));
				}
			}
			
			// Adds begin and end indices
			Literal beginIndex = model.createTypedLiteral(new Integer(0),
					XSDDatatype.XSDnonNegativeInteger);
			contextRes.addProperty(model.createProperty(nifPrefix + "beginIndex"), beginIndex);
			Literal endIndex = model.createTypedLiteral(new Integer(text.length()),	XSDDatatype.XSDnonNegativeInteger);
			contextRes.addProperty(model.createProperty(nifPrefix + "endIndex"), endIndex);
		}
		return contextRes;
	}

	/**
	 * Initializes the model.
	 */
	private void initModel() {
		
		String nifVersion = params.getNifVersion();
		boolean isNif20 = StringUtils.isEmpty(nifVersion) || nifVersion.equals("2.0");
		
		model = ModelFactory.createDefaultModel();
		
		if(isNif20){
			model.setNsPrefix("nif", RDFConstants.nifPrefix_2_0);
		} else {
			model.setNsPrefix("nif", RDFConstants.nifPrefix_2_1);
		}
		
		model.setNsPrefix("xsd", RDFConstants.xsdPrefix);
		model.setNsPrefix("itsrdf", RDFConstants.itsrdfPrefix);
		model.setNsPrefix("dc", RDFConstants.dcPrefix);
	}

	/**
	 * Gets the text info from the text unit list having a specific id.
	 * 
	 * @param id
	 *            the text unit id
	 * @return the text info
	 */
	private TextUnitInfo getTextUnitInfo(String id) {
		TextUnitInfo info = new TextUnitInfo();
		info.setTuId(id);
		TextUnitInfo retInfo = null;
		if (textUnitList.contains(info)) {
			retInfo = textUnitList.get(textUnitList.indexOf(info));
		}
		return retInfo;
	}

	/**
	 * Processes the end of the document.
	 * 
	 * @param endDoc
	 *            the end document.
	 */
	public void processEndDocument(Ending endDoc) {

		if (endDoc.getSkeleton() != null) {
			String skeletonString = endDoc.getSkeleton().toString();
			// For HTML snippet remove tags where not originally contained in the snippet:/html,/head,/body
			skeletonString = NifConverterUtil.getShortenedSkeletonString(skeletonString, params);
			skeletonMap.put(endDoc.getId(), skeletonString);
		}
		resolvePointers();
		buildNIFFile();
		close();
	}

	/**
	 * Processes the start of the document
	 * 
	 * @param resource
	 *            the start document resource
	 */
	public void processStartDocument(StartDocument resource) {

		skeletonMap = new LinkedHashMap<String, String>();
		textUnitList = new ArrayList<TextUnitInfo>();
		String resourceName = resource.getName();
		if (resourceName != null) {
			int lastSepIdx = resourceName.lastIndexOf("/");
			if (lastSepIdx != -1 && (lastSepIdx + 1) < resourceName.length()) {
				originalDocName = resourceName.substring(lastSepIdx + 1)
						+ "-skeleton";
			}
		}
		if (resource.getLocale() != null) {
			sourceLocale = resource.getLocale();
		}
		uriPrefix = params.getNifURIPrefix();
		if (uriPrefix == null || uriPrefix.isEmpty()) {
			uriPrefix = DEF_URI_PREFIX;
		}
		if (resource.getSkeleton() != null) {
			processSkeleton(resource);
		}
	}

}

/**
 * Class containing information about a text unit.
 */
class TextUnitInfo {

	/** The text */
	private String text;

	/** The ID */
	private String tuId;

	/** The offset start index in the skeleton context. */
	private int offset;

	/** The offset start index in the plain text context. */
	private int onlyTextOffset;

	/** */
	private int textUnitSet;

	/**
	 * States if this text should be inserted in the context. Text units not
	 * included are those representing attribute valuse.
	 */
	private boolean includeInContext;

	/**
	 * Gets the offset start index in the skeleton context.
	 * 
	 * @return the offset start index in the skeleton context.
	 */
	public int getOnlyTextOffset() {
		return onlyTextOffset;
	}

	/**
	 * Sets the offset start index in the skeleton context.
	 * 
	 * @param onlyTextOffset
	 *            the offset start index in the skeleton context.
	 */
	public void setOnlyTextOffset(int onlyTextOffset) {
		this.onlyTextOffset = onlyTextOffset;
	}

	/**
	 * Gets the text.
	 * 
	 * @return the text.
	 */
	public String getText() {
		return text;
	}

	/**
	 * Sets the text.
	 * 
	 * @param text
	 *            the text.
	 */
	public void setText(String text) {
		this.text = text;
	}

	/**
	 * Gets the ID
	 * 
	 * @return the ID
	 */
	public String getTuId() {
		return tuId;
	}

	/**
	 * Sets the ID
	 * 
	 * @param tuId
	 *            the ID
	 */
	public void setTuId(String tuId) {
		this.tuId = tuId;
	}

	/**
	 * Gets the offset start index in the skeleton context.
	 * 
	 * @return the offset start index in the skeleton context.
	 */
	public int getOffset() {
		return offset;
	}

	/**
	 * Sets the offset start index in the skeleton context.
	 * 
	 * @param offset
	 *            the offset start index in the skeleton context.
	 */
	public void setOffset(int offset) {
		this.offset = offset;
	}

	public int getTextUnitSet() {
		return textUnitSet;
	}

	public void setTextUnitSet(int textUnitSet) {
		this.textUnitSet = textUnitSet;
	}

	public boolean isIncludeInContext() {
		return includeInContext;
	}

	public void setIncludeInContext(boolean includeInContext) {
		this.includeInContext = includeInContext;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		boolean retValue = false;
		if (obj instanceof TextUnitInfo) {
			retValue = ((TextUnitInfo) obj).getTuId().equals(tuId);
		} else {
			retValue = super.equals(obj);
		}
		return retValue;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return tuId.hashCode();
	}

}

// class TextUnitInfoIdComparator implements Comparator<TextUnitInfo> {
//
// @Override
// public int compare(TextUnitInfo o1, TextUnitInfo o2) {
//
// int idxO1 = o1.getTuId().indexOf("-");
// Integer o1Id = null;
// if (idxO1 != -1) {
// o1Id = Integer.valueOf(o1.getTuId().substring(0, idxO1));
// } else {
// o1Id = Integer.valueOf(o1.getTuId());
// }
// int idxO2 = o2.getTuId().indexOf("-");
// Integer o2Id = null;
// if (idxO2 != -1) {
// o2Id = Integer.valueOf(o2.getTuId().substring(0, idxO2));
// } else {
// o2Id = Integer.valueOf(o2.getTuId());
// }
// int retValue = o1Id.compareTo(o2Id);
// if (retValue == 0 && idxO1 != -1 && idxO2 != -1) {
// retValue = Integer.valueOf(o1.getTuId().substring(idxO1 + 1))
// .compareTo(
// Integer.valueOf(o2.getTuId().substring(idxO2 + 1)));
// }
// return retValue;
// }
//
// }
//
class TextUnitInfoComparator implements Comparator<TextUnitInfo> {

	@Override
	public int compare(TextUnitInfo o1, TextUnitInfo o2) {

		int retValue = 0;
		if (o1.getOffset() < o2.getOffset()) {
			retValue = -1;
		} else if (o1.getOffset() > o2.getOffset()) {
			retValue = 1;
		}
		return retValue;
	}

}
