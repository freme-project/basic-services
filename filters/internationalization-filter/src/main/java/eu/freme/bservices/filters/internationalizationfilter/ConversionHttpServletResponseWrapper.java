package eu.freme.bservices.filters.internationalizationfilter;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

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

	/*
	 * this reader holds the input of the original request represented in turtle
	 */
	InputStream markupInTurtle;

	/*
	 * this stream takes original file and
	 */
	DummyOutputStream conversionStream;

	InternationalizationAPI api;

	public ConversionHttpServletResponseWrapper(HttpServletResponse response,
												InternationalizationAPI api, InputStream originalRequest,
												String informat, String outformat, String nifVersion) throws ConversionException,
			IOException {
		super(response);

		this.api = api;
		markupInTurtle = new ReaderInputStream(api.convertToTurtleWithMarkups(
				originalRequest, informat, nifVersion));
		//originalOutputStream = response.getOutputStream();
		conversionStream = new DummyOutputStream();
	}

	public ServletOutputStream getOutputStream() {
		return conversionStream;
	}

	public byte[] writeBackToClient(Boolean convertBack) throws IOException {
		byte[] buffer = new byte[1024];
		int read = 0;
		long length = 0;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		BufferedInputStream is;
		if(convertBack){
			InputStream enrichedData = conversionStream.getInputStream();
			Reader reader = api.convertBack(markupInTurtle, enrichedData);
			is = new BufferedInputStream(new ReaderInputStream(
					reader));
		}else {
			is = new BufferedInputStream(conversionStream.getInputStream());
		}
		while ((read = is.read(buffer)) != -1) {
			length += read;
			baos.write(buffer, 0, read);
		}
		is.close();
		if(convertBack)
			markupInTurtle.close();
		setContentLengthLong(length);
		
		return baos.toByteArray();
	}

	@Override
	public void flushBuffer() throws IOException {
		super.flushBuffer();
	}

	class DummyOutputStream extends ServletOutputStream{
		
		private ByteArrayOutputStream buffer = new ByteArrayOutputStream();

		@Override
		public boolean isReady() {
			return true;
		}

		@Override
		public void setWriteListener(WriteListener listener) {
		}

		@Override
		public void write(int b) throws IOException {
			buffer.write(b);
		}
		
		public InputStream getInputStream(){
			return new ByteArrayInputStream(buffer.toByteArray());
		}
		
		public void close() throws IOException{
			buffer.close();
		}
	}
}
