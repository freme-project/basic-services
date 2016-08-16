package eu.freme.bservices.filters.urlsupport;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.ReaderInputStream;

public class ModifyRequestBodyWrapper extends HttpServletRequestWrapper {

	String urlContent;
	HttpServletRequest httpRequest;
	String contentType;
	
	public ModifyRequestBodyWrapper(HttpServletRequest request, String urlContent, String contentType) {
		super(request);
		this.urlContent = urlContent;
		this.httpRequest = request;
		this.contentType = contentType;
	}

	@Override
	public ServletInputStream getInputStream() throws java.io.IOException{
		InputStream in = IOUtils.toInputStream(urlContent, "UTF-8");
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		ServletInputStream sis = new ServletInputStream(){
		   
			private boolean finished = false;
			private ReaderInputStream ris = new ReaderInputStream(reader);

			@Override
			public int read() throws IOException {
				int i = ris.read();
				if (i == -1) {
					finished = true;
					ris.close();
				}
				return i;
			}

			@Override
			public boolean isFinished() {
				return finished;
			}

			@Override
			public boolean isReady() {
				return !finished;
			}

			@Override
			public void setReadListener(ReadListener listener) {
			}
		};
		
		return sis; 

	}

	@Override
	public BufferedReader getReader() throws IOException{
		InputStream in = IOUtils.toInputStream(urlContent, "UTF-8");
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		return reader;
	}

	@Override
	public Enumeration<String> getHeaders(String name){

		if(name.equals("Content-Type")){
			List<String> wrapperList = new ArrayList<String>();
			if(contentType!=null)
				wrapperList.add(contentType);
			else //default value is html
				wrapperList.add("text/html");

			return Collections.enumeration(wrapperList);

		}
		return httpRequest.getHeaders(name);
	}
	
	@Override
	public String getContentType(){
		return this.contentType==null ? "text/html" : this.contentType;
	}
	
	/**
	 * makes sure that the content is written to the input parameter
	 * otherwise it is just the url-string
	 */
	@Override 
	public String getParameter(String name){
		String param = 	httpRequest.getParameter(name);

		if(name.equals("input") || name.equals("i")){
			param = this.urlContent;
		}else if(name.equals("informat")){
			param = this.contentType==null ? "text/html" : this.contentType;
		}
		return param;
	}
	
	
	


	
}
