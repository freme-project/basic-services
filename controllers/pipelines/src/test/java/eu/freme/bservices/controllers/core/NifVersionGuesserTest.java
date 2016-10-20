/**
 * Copyright © 2016 Agro-Know, Deutsches Forschungszentrum für Künstliche Intelligenz, iMinds,
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
package eu.freme.bservices.controllers.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

import eu.freme.bservices.controllers.pipelines.core.NIFVersionGuesser;
import eu.freme.common.conversion.rdf.JenaRDFConversionService;
import eu.freme.common.conversion.rdf.RDFConstants;
import eu.freme.common.conversion.rdf.RDFConstants.RDFSerialization;
import static org.junit.Assert.assertTrue;

public class NifVersionGuesserTest {

	@Test
	public void test() throws Exception {
		NIFVersionGuesser nvg = new NIFVersionGuesser();

		InputStream is = getClass().getClassLoader().getResourceAsStream(
				"nif-version-guesser/nif-20.ttl");
		String ttl = IOUtils.toString(is, "utf-8");
		is.close();
		Model model = new JenaRDFConversionService().unserializeRDF(ttl,
				RDFSerialization.TURTLE);
		String nifVersion = nvg.guessNifVersion(model);
		assertTrue(nifVersion.equals(RDFConstants.nifVersion2_0));

		is = getClass().getClassLoader().getResourceAsStream(
				"nif-version-guesser/nif-21.ttl");
		ttl = IOUtils.toString(is, "utf-8");
		is.close();
		model = new JenaRDFConversionService().unserializeRDF(ttl,
				RDFSerialization.TURTLE);
		nifVersion = nvg.guessNifVersion(model);
		assertTrue(nifVersion.equals(RDFConstants.nifVersion2_1));
	}
}
