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
package eu.freme.bservices.filters.internationalizationfilter;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import eu.freme.bservices.internationalization.api.InternationalizationAPI;

import org.apache.commons.io.input.ReaderInputStream;

import eu.freme.bservices.internationalization.okapi.nif.converter.ConversionException;

/**
 * ConversionHttpServletResponseWrapper collects the response of a normal API
 * request and uses the e-Internationalization API to convert the the response
 * back to a source format. So it converts NIF to HTML, XLIFF, ...
 * 
 * @author Jan Nehring - jan.nehring@dfki.de
 */
public class ConversionHttpServletResponseWrapper extends
		HttpServletResponseWrapper {

	int contentLength = 0;
	String contentType = "";
	BufferOutputStream bos;
	
	public ConversionHttpServletResponseWrapper(HttpServletResponse response) {
		super(response);
		this.contentLength = 0;
	}
	
	public String getHeader(String header){
		if(header.toLowerCase().equals("content-length")){
			return new Integer(contentLength).toString();
		} else if(header.toLowerCase().equals("content-type")){
			return contentType;
		} else{
			return super.getHeader(header);
		}
	}
	
	public Collection<String> getHeaders(String header){
		List<String> headers = new ArrayList<>();
		if(header.toLowerCase().equals("content-length")){
			headers.add(new Integer(contentLength).toString());
		} else if(header.toLowerCase().equals("content-type")){
			headers.add(contentType);
		} else{
			return super.getHeaders(header);
		}
		return headers;
	}

	public int getContentLength() {
		return contentLength;
	}

	public void setContentLength(int contentLength) {
		this.contentLength = contentLength;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}
	
    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if(bos == null){
        	bos = new BufferOutputStream();
        }
        return bos;
    };
    
    public class BufferOutputStream extends ServletOutputStream{

    	ByteArrayOutputStream baos = new ByteArrayOutputStream();
    	
		@Override
		public boolean isReady() {
			return true;
		}

		@Override
		public void setWriteListener(WriteListener listener) {
		}

		@Override
		public void write(int b) throws IOException {
			baos.write(b);
		}
		
		public ByteArrayOutputStream getBuffer(){
			return baos;
		}
    	
    }

}
