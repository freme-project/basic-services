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
package eu.freme.bservices.internationalization.nif.step;

import eu.freme.bservices.internationalization.okapi.nif.step.NifParameters;
import eu.freme.bservices.internationalization.okapi.nif.step.NifWriterStep;
import eu.freme.common.conversion.rdf.JenaRDFConversionService;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.lib.extra.pipelinebuilder.*;
import net.sf.okapi.steps.common.RawDocumentToFilterEventsStep;
import org.junit.Test;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;

public class NifWriterStepTest {

	private static final LocaleId ENUS = new LocaleId("en", "us");
	private static final LocaleId DEDE = new LocaleId("de", "de");
	private static final LocaleId ITIT = new LocaleId("it", "it");

	@Test
	public void testPackageFormat() throws URISyntaxException,
			MalformedURLException {
		File baseDir = new File(this.getClass().getResource("/nifConversion")
				.toURI());
		String pathBase = baseDir.getAbsolutePath();
		String src1Path = pathBase + "/src2/";
		new XPipeline("Test pipeline for NifWriterStep", new XBatch(
				new XBatchItem(new File(src1Path, "TestDocument02.odt").toURI().toURL(),
						"UTF-8", ENUS, ITIT)//,

//				new XBatchItem(new File(src1Path, "TestPresentation01.odp").toURI()
//						.toURL(), "UTF-8", ENUS, DEDE),
//
//				new XBatchItem(new File(src1Path, "TestSpreadsheet01.ods").toURI().toURL(),
//						"UTF-8", ENUS, DEDE)
				),

		// mandatory step --> starting from the raw document, it sends
		// appropriate events, then handled by next steps in the pipeline
				new RawDocumentToFilterEventsStep(),

				new XPipelineStep(new NifWriterStep(), new XParameter(
						NifParameters.OUTPUT_BASE_PATH, pathBase),
				// Defines the desired serialization. Allowed values:
				// RDFSerialization.TURTLE.toRDFLang(),
				// RDFSerialization.JSON-LD.toRDFLang()
				// If null, the output files are saved in RDF format.
						new XParameter(NifParameters.NIF_LANGUAGE, JenaRDFConversionService.JENA_TURTLE)))
				.execute();
	}

}
