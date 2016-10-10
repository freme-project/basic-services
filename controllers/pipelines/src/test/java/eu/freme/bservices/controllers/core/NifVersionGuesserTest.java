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
