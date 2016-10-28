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

import eu.freme.bservices.internationalization.okapi.nif.filter.NifSkeletonWriterFilter;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.pipeline.BasePipelineStep;
import net.sf.okapi.common.resource.DocumentPart;

/**
 * This step can be used in the Okapi pipeline. It sends all events received
 * from the pipeline to the NIF skeleton writer filter.
 */
public class NifSkeletonWriterStep extends BasePipelineStep {

	/** The parameters. */
	private NifParameters params;

	/** The NIF skeleton writer. */
	private NifSkeletonWriterFilter writer;

	/**
	 * Constructor.
	 */
	public NifSkeletonWriterStep() {
		params = new NifParameters();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.sf.okapi.common.pipeline.IPipelineStep#getName()
	 */
	@Override
	public String getName() {

		return "NIF Skeleton Writer";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.sf.okapi.common.pipeline.IPipelineStep#getDescription()
	 */
	@Override
	public String getDescription() {

		return "Generate a NIF file containing two contexts: one plain text context and one skeleton context. Expects: filter events. Sends back: filter events.";
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
	 * net.sf.okapi.common.pipeline.BasePipelineStep#handleEvent(net.sf.okapi
	 * .common.Event)
	 */
	@Override
	public Event handleEvent(Event event) {
		switch (event.getEventType()) {

		case NO_OP:
			return event;
		case START_DOCUMENT:
			writer = new NifSkeletonWriterFilter(params, event
					.getStartDocument().getLocale());
			writer.processStartDocument(event.getStartDocument());
			break;

		case TEXT_UNIT:
			writer.processTextUnit(event.getTextUnit());
			return event;
		case END_DOCUMENT:
			writer.processEndDocument(event.getEnding());
			break;
		case END_GROUP:
		case END_SUBFILTER:
			break;
		case END_SUBDOCUMENT:
		case START_SUBDOCUMENT:
			break;
		case DOCUMENT_PART:
			if (event.getResource().getSkeleton() != null) {
				writer.processDocumentPart((DocumentPart) event.getResource());
			}
			break;
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

}
