package eu.freme.bservices.filters.tika;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.DatatypeConverter;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;

import com.google.common.base.Strings;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import eu.freme.bservices.controllers.nifconverter.NifConverterController;
import eu.freme.common.exception.BadRequestException;
import eu.freme.common.exception.ExceptionHandlerService;

@Component
@Order(80)
public class TikaFilter extends GenericFilterBean {

	private Logger logger = Logger.getLogger(TikaFilter.class);

	@Autowired
	private NifConverterController nifController;

	@Autowired
	ExceptionHandlerService exceptionHandlerService;

	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {


		String informat = request.getParameter("informat");
		if(Strings.isNullOrEmpty(informat)){
			informat = request.getParameter("i");
		}


		if(Strings.isNullOrEmpty(informat) || !informat.equals("TIKAFile") || !(request instanceof HttpServletRequest) ||
				!(response instanceof HttpServletResponse) ){
			chain.doFilter(request, response);
		} else if(((HttpServletRequest)request).getRequestURI().contains("/toolbox/nif-converter")){
			chain.doFilter(request, response);
		}else{

			if(!request.getParameterMap().containsKey("filename")){
				String msg = "For using TIKA, please specify the parameter 'filename' containing the name of the file that is to be processed.";
				throw new BadRequestException(msg);
			}
			//call NIFConverterController
			String nifConverterUrl = "/toolbox/nif-converter";
			String baseUrl = String.format("%s://%s:%d", request.getScheme(),  request.getServerName(), request.getServerPort());

			InputStream requestInputStream = request.getInputStream(); //final ? 
			byte[] baosData = IOUtils.toByteArray(requestInputStream);
						
			requestInputStream.close();

			String filename = request.getParameter("filename");
			File tempFile = File.createTempFile(filename, null);
			FileUtils.writeByteArrayToFile(tempFile, baosData);
			
			HttpResponse<String> nifResponse;
			String nifcontent = "";
			try {
				nifResponse = Unirest
						.post(baseUrl + nifConverterUrl)
						.queryString("informat", "TIKAFile")
						.header("Accept", "text/turtle")
						.field("inputFile", tempFile)
						.asString();

				nifcontent =  nifResponse.getBody();
				System.out.println(nifcontent);

				if(tempFile.delete()){
					logger.info("uploaded file, " + tempFile.getName() + ", was deleted by Tika-Filter.");
				}else{
					logger.warn("Deleting uploaded file, " + tempFile.getName() + ", by Tika-Filter failed.");
					tempFile.deleteOnExit();
				}

			} catch (UnirestException e) {
				e.printStackTrace();
				logger.error(e);
				exceptionHandlerService.handleError((HttpServletRequest)request,(Throwable) e);
				throw new IOException(e);
			}

			final Map<String,String[]> paramMap = new HashMap<String, String[]>();

			String[] intypeArr = {"file"}; 
			String[] informatArr = {"turtle"}; 
			String[] iArr = {"turtle"}; 
			String[] inputArr = {nifcontent}; 

			paramMap.put("intype", intypeArr);
			paramMap.put("informat", informatArr);
			paramMap.put("input", inputArr);
			paramMap.put("i", iArr);
			HttpServletRequestWrapper requestWrapper = new TikaBodyRequestWrapper((HttpServletRequest)request, nifcontent, paramMap);
			chain.doFilter(requestWrapper,response);

		}

	}
}

