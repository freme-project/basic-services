package eu.freme.bservices.testhelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import eu.freme.bservices.testhelper.api.IntegrationTestSetup;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.hp.hpl.jena.shared.AssertionFailureException;
import com.mashape.unirest.http.HttpResponse;

import eu.freme.common.conversion.rdf.RDFConstants;
import eu.freme.common.conversion.rdf.RDFConversionService;

/**
 * @author Jan Nehring - jan.nehring@dfki.de
 */
@Component
public class TestHelper implements ApplicationContextAware{

	protected ApplicationContext context;
	
	//@Autowired
	//RDFConversionService rdfConversionService;

//	public TestHelper(String packageConfigFileName){
//		context = IntegrationTestSetup.getContext(packageConfigFileName);
//	}


	/**
	 * Returns the base url of the API given the spring application context, e.g. http://localhost:8080
	 * @return
	 */
	public String getAPIBaseUrl(){
		String port = context.getEnvironment().getProperty("server.port");
		if( port == null){
			port = "8080";
		}
		return "http://localhost:" + port;
	}
	
	/**
	 * Return the username of the administrator user of the REST API.
	 *
	 * @return
	 */
	public String getAdminUsername(){
		return context.getEnvironment().getProperty("admin.username");
	}
	
	/**
	 * Return the password of the administrator user of the REST API.
	 *
	 * @return
	 */
	public String getAdminPassword(){
		return context.getEnvironment().getProperty("admin.password");
	}

	public String getResourceContent(String resourceLocation) throws IOException {
		ClassLoader classLoader = getClass().getClassLoader();
		File file = new File(classLoader.getResource(resourceLocation).getFile());
		return FileUtils.readFileToString(file);
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		context = applicationContext;
	}
}
