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
package eu.freme.bservices.internationalization.okapi.nif.step;

import eu.freme.bservices.internationalization.okapi.nif.filter.NifWriterFilter;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;
import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.MimeTypeMapper;
import net.sf.okapi.common.pipeline.BasePipelineStep;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.StartDocument;

/**
 * This step can be used in the Okapi pipeline. It sends all events received
 * from the pipeline to the NIF writer filter.
 */
public class NifWriterStep extends BasePipelineStep {

	/** The parameters for this step. */
	private NifParameters params;

	/** The NIF filter writer object. */
	private NifWriterFilter writer;
	
	/** The document mime type*/
	private String mimeType;
	
	/**
	 * Constructor.
	 */
	public NifWriterStep() {

		params = new NifParameters();
		// codec = OPCPackageReader.CODEC;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.sf.okapi.common.pipeline.IPipelineStep#getName()
	 */
	@Override
	public String getName() {

		return "NIF writer";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.sf.okapi.common.pipeline.IPipelineStep#getDescription()
	 */
	@Override
	public String getDescription() {
		return "Generate NIF file. Expects: filter events. Sends back: filter events.";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.sf.okapi.common.pipeline.BasePipelineStep#getParameters()
	 */
	@Override
	public IParameters getParameters() {
		return params;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.sf.okapi.common.pipeline.BasePipelineStep#setParameters(net.sf.okapi
	 * .common.IParameters)
	 */
	@Override
	public void setParameters(IParameters params) {

		if (!(params instanceof NifParameters)) {
			throw new IllegalArgumentException("Received params of type "
					+ params.getClass().getName()
					+ ". Only NifParameters accepted.");
		}
		this.params = (NifParameters) params;
		if (writer != null) {
			writer.setParameters(params);
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.sf.okapi.common.pipeline.BasePipelineStep#handleEvent(net.sf.okapi
	 * .common.Event)
	 */
	@Override
	public Event handleEvent(Event event) {
//		System.out.println(event.getResource().getSkeleton().toString());
		switch (event.getEventType()) {
		
		case NO_OP:
			return event;
		case START_DOCUMENT:
			processStartDocument(event.getStartDocument());
			break;

		case TEXT_UNIT:
			ITextUnit tu = event.getTextUnit();
			if (mimeType == MimeTypeMapper.HTML_MIME_TYPE && tu.getType() != null && tu.getType().startsWith("x-")) {
				return event;
			} else {
				Event ev = new Event(EventType.TEXT_UNIT, tu.clone());
				processTextUnit(tu);
				return ev;
			}
		case END_DOCUMENT:
			processEndDocument();
			break;
		case END_GROUP:
		case END_SUBFILTER:
		case DOCUMENT_PART:
		case START_SUBDOCUMENT:
		case END_SUBDOCUMENT:
		case CUSTOM:
		case CANCELED:
		case START_BATCH:
		case END_BATCH:
		case MULTI_EVENT:
		case PIPELINE_PARAMETERS:
		case RAW_DOCUMENT:
		case START_BATCH_ITEM:
		case END_BATCH_ITEM:
		case START_GROUP:
		case START_SUBFILTER:
			break;
		default:
			break;
		}
		return event;
	}

	/**
	 * Processes a single text unit.
	 * 
	 * @param textUnit
	 *            the text unit.
	 */
	private void processTextUnit(ITextUnit textUnit) {

		// clone the text unit, so that it can be handled by following steps in
		// the pipeline.
		textUnit = textUnit.clone();
		// CodecUtil.encodeTextUnit(textUnit, codec);
		// make the writer process the text unit
		writer.processTextUnit(textUnit);
	}

	/**
	 * Processes the end of the document and reset the writer.
	 */
	private void processEndDocument() {
		if (writer != null) {
			writer.processEndDocument();
			writer = null;
		}

	}

	/**
	 * Creates the NIF writer filter object and then processes the start of the
	 * document.
	 * 
	 * @param startDocument
	 *            the start document object
	 */
	private void processStartDocument(StartDocument startDocument) {
		mimeType = startDocument.getMimeType();
		writer = new NifWriterFilter(params, startDocument.getLocale());
		writer.processStartDocument(startDocument);
	}
	

}
