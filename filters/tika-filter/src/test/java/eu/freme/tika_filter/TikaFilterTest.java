package eu.freme.tika_filter;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import eu.freme.bservices.testhelper.TestHelper;
import eu.freme.bservices.testhelper.ValidationHelper;
import eu.freme.bservices.testhelper.api.IntegrationTestSetup;

public class TikaFilterTest {

	TestHelper th;
	ValidationHelper vh;
	Logger logger = Logger.getLogger(TikaFilterTest.class);


	public TikaFilterTest() throws  UnirestException {
		ApplicationContext context = IntegrationTestSetup.getContext("tika-filter-test-package.xml");
		th = context.getBean(TestHelper.class);
		vh = context.getBean(ValidationHelper.class);
	}

	@Test
	public void testPdfContent() throws UnirestException, IOException {

		String filename = "source_pdf.pdf";

		ClassLoader classLoader = getClass().getClassLoader();
		File pdfFile = new File(classLoader.getResource(filename).getFile());
		byte[] bFile = Files.readAllBytes(pdfFile.toPath());
		
		HttpResponse<String> response = Unirest.post(th.getAPIBaseUrl() +  "/e-capitalization")
				.queryString("filename",filename)
				.queryString("informat", "TIKAFile")
				.body(bFile)
				.asString();
		
		
		if(pdfFile.delete()){
			logger.info("uploaded file, " + pdfFile.getName() + ", was deleted by TikaFilterTest.");
		}else{
			logger.warn("Deleting uploaded file, " + pdfFile.getName() + ", by TikaFilterTest failed.");
			pdfFile.deleteOnExit();
		}
		
        assertEquals(HttpStatus.OK.value(), response.getStatus());
        assertTrue(response.getBody().contains("THE FREE ACROBAT READER IS EASY TO DOWNLOAD"));
        
	}
	
	@Test
	public void testOdtContent() throws UnirestException, IOException {

        String filename = "testdoc.odt";
    	
        ClassLoader classLoader = getClass().getClassLoader();
		File odtFile = new File(classLoader.getResource(filename).getFile());
		System.out.println(odtFile.getAbsolutePath());
		byte[] bFile = Files.readAllBytes(odtFile.toPath());
		
		
//        File odtFile = File.createTempFile("src/test/resources/" + filename, null);
		HttpResponse<String> response = Unirest.post(th.getAPIBaseUrl() +  "/e-capitalization")
				.queryString("filename",filename)
				.queryString("informat", "TIKAFile")
				.body(bFile)
				.asString();
		
		if(odtFile.delete()){
			logger.info("uploaded file, " + odtFile.getName() + ", was deleted by TikaFilterTest.");
		}else{
			logger.warn("Deleting uploaded file, " + odtFile.getName() + ", by TikaFilterTest failed.");
			odtFile.deleteOnExit();
		}
		
        assertEquals(HttpStatus.OK.value(), response.getStatus());
        assertTrue(response.getBody().contains("IT WAS CREATED IN BERLIN"));

	}
	
	

}
