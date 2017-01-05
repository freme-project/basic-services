//
//package eu.freme.bservices.filters.urlsupport;
//import static org.junit.Assert.assertEquals;
//import static org.junit.Assert.assertTrue;
//
//import java.io.File;
//import java.io.IOException;
//
//import org.apache.commons.io.FileUtils;
//import org.apache.log4j.Logger;
//import org.junit.Test;
//import org.springframework.context.ApplicationContext;
//import org.springframework.http.HttpStatus;
//
//import com.mashape.unirest.http.HttpResponse;
//import com.mashape.unirest.http.Unirest;
//import com.mashape.unirest.http.exceptions.UnirestException;
//import com.mashape.unirest.request.HttpRequestWithBody;
//
//import eu.freme.bservices.testhelper.TestHelper;
//import eu.freme.bservices.testhelper.ValidationHelper;
//import eu.freme.bservices.testhelper.api.IntegrationTestSetup;
//
//public class UrlSupportFilterTest {
//
//	TestHelper th;
//	ValidationHelper vh;
//	Logger logger = Logger.getLogger(UrlSupportFilterTest.class);
//
//	String dataset = "dbpedia";
//	//		FremeNer fremeNer;
//
//	public UrlSupportFilterTest() throws  UnirestException {
//		ApplicationContext context = IntegrationTestSetup.getContext("urlsupport-filter-test-package.xml");
//		th = context.getBean(TestHelper.class);
//		vh = context.getBean(ValidationHelper.class);
//	}
//
//	@Test
//	public void testRetrieveHtmlWithInformat() throws UnirestException, IOException {
//
//        String filename = "testSentence.html";
//        String url = th.getAPIBaseUrl() + "/mockups/file/" + filename;
//		HttpResponse<String> response = Unirest.post(th.getAPIBaseUrl() +  "/e-capitalization")
//				.queryString("intype","url")
//				.queryString("input", url)
//				.queryString("informat","text/html")
//				.asString();
//		
//        assertEquals(HttpStatus.OK.value(), response.getStatus());
//        assertTrue(response.getBody().contains("HELLO, WORLD."));
//
//	}
//	
//	@Test
//	public void testRetrieveHtmlWithContentType() throws UnirestException, IOException {
//
//        String filename = "testSentence.html";
//        String url = th.getAPIBaseUrl() + "/mockups/file/" + filename;
//		HttpResponse<String> response = Unirest.post(th.getAPIBaseUrl() +  "/e-capitalization")
//				.queryString("intype","url")
//				.queryString("input", url)
//				.header("Content-Type", "text/html")
//				.asString();
//		
//        assertEquals(HttpStatus.OK.value(), response.getStatus());
//        assertTrue(response.getBody().contains("HELLO, WORLD."));
//
//	}
//	
//	
//	@Test
//	public void testRetrievePlainTextWithInformat() throws UnirestException, IOException {
//
//        String filename = "testSentence.txt";
//        String url = th.getAPIBaseUrl() + "/mockups/file/" + filename;
//		HttpResponse<String> response = Unirest.post(th.getAPIBaseUrl() +  "/e-capitalization")
//				.queryString("intype","url")
//				.queryString("input", url)
//				.queryString("informat","text/plain")
//				.asString();
//		
//        assertEquals(HttpStatus.OK.value(), response.getStatus());
//        assertTrue(response.getBody().contains("HELLO, WORLD."));
//
//	}
//	
//	@Test
//	public void testRetrievePlainTextWithContentType() throws UnirestException, IOException {
//
//        String filename = "testSentence.txt";
//        String url = th.getAPIBaseUrl() + "/mockups/file/" + filename;
//		HttpResponse<String> response = Unirest.post(th.getAPIBaseUrl() +  "/e-capitalization")
//				.queryString("intype","url")
//				.queryString("input", url)
//				.header("Content-Type", "text/html")
//				.asString();
//		
//        assertEquals(HttpStatus.OK.value(), response.getStatus());
//        assertTrue(response.getBody().contains("HELLO, WORLD."));
//
//	}
//	
//	
//	
//
//}
