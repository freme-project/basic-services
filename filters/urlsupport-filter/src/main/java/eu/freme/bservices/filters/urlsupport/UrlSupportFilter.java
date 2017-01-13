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
package eu.freme.bservices.filters.urlsupport;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;

import com.google.common.base.Strings;

import eu.freme.common.exception.BadRequestException;

/**
 * Filter that puts the input it reads from a URL into the body of the request 
 * when the parameter intype is set to a url in a http request.  
 * 
 * @author britta grusdt 
 *
 */
@Component
@Order(79)
public class UrlSupportFilter extends GenericFilterBean {

	private Logger logger = Logger.getLogger(UrlSupportFilter.class);

	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {

		String intype = request.getParameter("intype");
		if(intype==null || intype.equals("direct") || intype.equals("file") || !(request instanceof HttpServletRequest) ||
				!(response instanceof HttpServletResponse)){
			chain.doFilter(request, response);
		} else{

			final HttpServletRequest httpRequest = (HttpServletRequest) request;
			String url="";

			if(intype.equals("url")){

				// Retrieve url from parameters, 'input' overwrites shorter version 'i'
				String[] providedUrl = request.getParameterValues("input");
				if(providedUrl!=null){
					for(String val : request.getParameterValues("input"))
						url+=val;
				}
				//from short 'i' - parameter
				if(url.isEmpty()){
					providedUrl = request.getParameterValues("i");
					
					if(providedUrl!=null){
						for(String val : providedUrl)
							url+=val;
					}else{
						String msg="No url is given. If intype is set to url, a valid url must be specified in the parameter 'input' or 'i'";
						logger.error(msg);
						throw new BadRequestException(msg);
					}
				
				}
			}

			InputStream in;
			//Get Content-Type via informat parameter
			String contentType="";
			if(request.getParameterValues("informat")!=null){
				for(String val : request.getParameterValues("informat"))
					contentType+=val;
			}
			//or via Content-Type
			if(contentType.isEmpty()){
				contentType = request.getContentType();
			}


			try{
				URL website = new URL(url);
				URLConnection connection = website.openConnection();
				//if no Content-Type is specified, the default text/turtle is used
				if(Strings.isNullOrEmpty(contentType))
					contentType = "text/turtle";
				in = connection.getInputStream();
				//in = new URL(url).openStream();
			}catch(IOException ex){
				logger.error("Content could not be retrieved from url.");
				logger.error("Error",ex);
				throw ex;
			}
			final byte[] byteContent;
			final String stringContent;

			try {
				byteContent = IOUtils.toByteArray(in);
				stringContent = new String(byteContent);

			} finally {
				IOUtils.closeQuietly(in);
			}

			HttpServletRequestWrapper requestWrapper = new ModifyRequestBodyWrapper(httpRequest, stringContent, contentType);

			chain.doFilter(requestWrapper,response);

		}


	}
}

