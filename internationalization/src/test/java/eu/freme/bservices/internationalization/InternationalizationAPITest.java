/**
 * Copyright (C) 2015 Agro-Know, Deutsches Forschungszentrum für Künstliche Intelligenz, iMinds,
 * Institut für Angewandte Informatik e. V. an der Universität Leipzig,
 * Istituto Superiore Mario Boella, Tilde, Vistatec, WRIPL (http://freme-project.eu)
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
package eu.freme.bservices.internationalization;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.mashape.unirest.http.exceptions.UnirestException;
import eu.freme.bservices.testhelper.TestHelper;
import eu.freme.bservices.internationalization.api.InternationalizationAPI;
import eu.freme.bservices.internationalization.okapi.nif.converter.ConversionException;
import eu.freme.bservices.internationalization.okapi.nif.filter.RDFConstants;

import eu.freme.bservices.testhelper.api.IntegrationTestSetup;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

import java.io.*;

import static org.junit.Assert.assertTrue;

public class InternationalizationAPITest {

	TestHelper th;
	ClassLoader classLoader;
	Logger logger = Logger.getLogger(InternationalizationAPITest.class);

    InternationalizationAPI internationalizationAPI;


	public InternationalizationAPITest() throws UnirestException {
		ApplicationContext context = IntegrationTestSetup.getContext("internationalization-test-package.xml");// FREMEStarter.startPackageFromClasspath("ratelimiter-test-package.xml");
		th = context.getBean(TestHelper.class);
        internationalizationAPI = context.getBean(InternationalizationAPI.class);
		classLoader = getClass().getClassLoader();
	}

	
	@Test
	public void testEInternationalizationAPIXliff() {

		InputStream is = getClass().getResourceAsStream(
				"/nifConversion/src1/test1.xlf");
		try {
			Reader nifReader = internationalizationAPI.convertToTurtle(is,
					InternationalizationAPI.MIME_TYPE_XLIFF_1_2);
			Model model = ModelFactory.createDefaultModel();
			model.read(nifReader, null,
					RDFConstants.RDFSerialization.TURTLE.toRDFLang());
			// assertFalse(model.isEmpty());
//			model.write(new OutputStreamWriter(System.out), "TTL");
			Reader expectedReader = new InputStreamReader(getClass()
					.getResourceAsStream(
							"/nifConversion/expected_text1.xlf.ttl"), "UTF-8");
			Model expectedModel = ModelFactory.createDefaultModel();
			expectedModel.read(expectedReader, null,
					RDFConstants.RDFSerialization.TURTLE.toRDFLang());
			assertTrue(model.isIsomorphicWith(expectedModel));
		} catch (ConversionException | UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
	
	 @Test
	public void testEInternationalizationAPIHTML() {

		InputStream is = getClass().getResourceAsStream(
				"/nifConversion/src1/test10.html");
		try {
			Reader nifReader = internationalizationAPI.convertToTurtle(is,
					InternationalizationAPI.MIME_TYPE_HTML);
			Model model = ModelFactory.createDefaultModel();
			model.read(nifReader, null,
					RDFConstants.RDFSerialization.TURTLE.toRDFLang());
			Reader expectedReader = new InputStreamReader(getClass()
					.getResourceAsStream(
							"/nifConversion/expected_text10.html.ttl"), "UTF-8");
			Model expectedModel = ModelFactory.createDefaultModel();
			expectedModel.read(expectedReader, null,
					RDFConstants.RDFSerialization.TURTLE.toRDFLang());
			assertTrue(model.isIsomorphicWith(expectedModel));
		} catch (ConversionException | UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	 }
	
	 @Test
	public void testEInternationalizationAPIXML() {

		InputStream is = getClass().getResourceAsStream(
				"/nifConversion/src1/test1.xml");
		try {
			Reader nifReader = internationalizationAPI.convertToTurtle(is,
					InternationalizationAPI.MIME_TYPE_XML);
			Model model = ModelFactory.createDefaultModel();
			model.read(nifReader, null,
					RDFConstants.RDFSerialization.TURTLE.toRDFLang());
//			 model.write(new OutputStreamWriter(System.out), "TTL");
			// assertFalse(model.isEmpty());
			Reader expectedReader = new InputStreamReader(getClass()
					.getResourceAsStream(
							"/nifConversion/expected_test1.xml.ttl"), "UTF-8");
			Model expectedModel = ModelFactory.createDefaultModel();
			expectedModel.read(expectedReader, null,
					RDFConstants.RDFSerialization.TURTLE.toRDFLang());
			assertTrue(model.isIsomorphicWith(expectedModel));
		} catch (ConversionException | UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	 }

	
	 @Test
	public void testEInternationalizationAPIODT() {

		InputStream is = getClass().getResourceAsStream(
				"/nifConversion/src2/TestDocument02.odt");
		try {
			Reader nifReader = internationalizationAPI.convertToTurtle(is,
					InternationalizationAPI.MIME_TYPE_ODT);
			Model model = ModelFactory.createDefaultModel();
			model.read(nifReader, null,
					RDFConstants.RDFSerialization.TURTLE.toRDFLang());
//			model.write(new OutputStreamWriter(System.out), "TTL");
			// assertFalse(model.isEmpty());
			 Reader expectedReader = new InputStreamReader(getClass()
			 .getResourceAsStream(
			 "/nifConversion/expected_TestDocument02.odt.ttl"), "UTF-8");
			 Model expectedModel = ModelFactory.createDefaultModel();
			 expectedModel.read(expectedReader, null,
			 RDFConstants.RDFSerialization.TURTLE.toRDFLang());
//			 expectedModel.write(new OutputStreamWriter(System.out), "TTL");
//			 assertTrue(model.isIsomorphicWith(expectedModel));
		} catch (ConversionException | UnsupportedEncodingException e) {
			e.printStackTrace();
			 }
	 }
	
	 @Test
	public void testEInternationalizationAPIUnsupportedMimeType() {

		String unsupportedMimeType = "unsupp/mime-type";
		InputStream is = getClass().getResourceAsStream(
				"/nifConversion/src1/test1.xlf");
		ConversionException exception = null;
		try {
			internationalizationAPI.convertToTurtle(is, unsupportedMimeType);
		} catch (ConversionException e) {
			exception = e;
		}
		// Assert.assertNotNull(exception);
		// UnsupportedMimeTypeException unsuppException = new
		// UnsupportedMimeTypeException(
		// unsupportedMimeType, new String[] {
		// InternationalizationAPI.MIME_TYPE_XLIFF_1_2,
		// InternationalizationAPI.MIME_TYPE_HTML });
		// Assert.assertEquals(
		// unsuppException.getMessage(),
		// exception.getMessage());
		// Assert.assertTrue(exception.getCause() instanceof
		// UnsupportedMimeTypeException);
	}

//	@Test
//	public void testRoundtripping() throws IOException {
//
//		try {
//			//STEP 1: creation of the skeleton file: the TTL file with the context including markups.
//			InputStream originalFile = getClass().getResourceAsStream(
//					"/roundtripping/input-html.txt");
//			Reader skeletonReader = internationalizationAPI
//					.convertToTurtleWithMarkups(originalFile,
//							InternationalizationAPI.MIME_TYPE_HTML);
//			
//			//STEP 2: save the skeleton file somewhere on the machine
//			BufferedReader br = new BufferedReader(skeletonReader);
////			File skeletonFile = File.createTempFile("freme-i18n-unittest", "");
//			File skeletonFile = new File(System.getProperty("user.home"), "skeletonApiTest.ttl");
//			FileWriter writer = new FileWriter(skeletonFile);
//			String line;
//			while ((line = br.readLine()) != null) {
//				System.out.println(line);
//				writer.write(line);
//			}
//			br.close();
//			writer.close();
//			
//			//STEP 3: execute the conversion back by submitting the skeleton file and the enriched file
//			InputStream skeletonStream = new FileInputStream(skeletonFile);
//			InputStream turtle = getClass().getResourceAsStream(
//					"/roundtripping/input-turtle.txt");
//			Reader reader = internationalizationAPI.convertBack(skeletonStream,
//					turtle);
//			br = new BufferedReader(reader);
//			while ((line = br.readLine()) != null) {
//				System.out.println(line);
//			}
//			br.close();
////			skeletonFile.delete();
//		} catch (ConversionException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}


	
	public void testRoundTripping(String originalFilePath, String enrichmentPath)
			throws ConversionException, IOException {
		// STEP 1: creation of the skeleton file: the TTL file with the context
		// including markups.
		InputStream originalFile = getClass().getResourceAsStream(
				originalFilePath);
		Reader skeletonReader = internationalizationAPI
				.convertToTurtleWithMarkups(originalFile,
						InternationalizationAPI.MIME_TYPE_HTML);

		// STEP 2: save the skeleton file somewhere on the machine
		BufferedReader br = new BufferedReader(skeletonReader);
		File skeletonFile = File.createTempFile("freme-i18n-unittest", "");
		FileWriter writer = new FileWriter(skeletonFile);
		String line;
		while ((line = br.readLine()) != null) {
//			System.out.println(line);
			writer.write(line);
		}
		br.close();
		writer.close();

		// STEP 3: execute the conversion back by submitting the skeleton file
		// and the enriched file
		InputStream skeletonStream = new FileInputStream(skeletonFile);
		InputStream turtle = getClass().getResourceAsStream(
				enrichmentPath);
		Reader reader = internationalizationAPI.convertBack(skeletonStream,
				turtle);
		br = new BufferedReader(reader);
		while ((line = br.readLine()) != null) {
			logger.info(line);
		}
		br.close();
		skeletonFile.delete();
	}

//	@Test
	public void testSimpleRoundtripping() throws IOException, ConversionException {

		testRoundTripping("/roundtripping/input-html.txt",
				"/roundtripping/input-turtle.txt");
	}
	
//	@Test
//	public void testConvertToTurtle(){
//		
//		InputStream fileToConvert = getClass().getResourceAsStream("/roundtripping/short-html.html");
//		try {
//			Reader nifReader = internationalizationAPI.convertToTurtle(fileToConvert, InternationalizationAPI.MIME_TYPE_HTML);
//			File nifFile = new File(System.getProperty("user.home"), "convertedHtml.ttl");
//			FileWriter writer = new FileWriter(nifFile);
//			BufferedReader br = new BufferedReader(nifReader);
//			String line;
//			while ((line = br.readLine()) != null) {
//				System.out.println(line);
//				writer.write(line);
//			}
//			br.close();
//			writer.close();
//		} catch (ConversionException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		};
//	}
	
	@Test
	public void testLongRoundtripping() throws IOException, ConversionException {

		testRoundTripping("/roundtripping/vt-input-html.txt",
				"/roundtripping/vt-input-turtle.txt");
		
//		testRoundTripping("/roundtripping/long-html.html",
//				"/roundtripping/long-html-enriched.ttl");
		
	}
}