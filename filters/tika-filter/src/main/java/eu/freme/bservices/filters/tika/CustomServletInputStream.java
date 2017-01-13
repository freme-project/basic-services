package eu.freme.bservices.filters.tika;

import java.io.IOException;
import java.io.Reader;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;

import org.apache.commons.io.input.ReaderInputStream;

public class CustomServletInputStream extends ServletInputStream {

	  
	private boolean finished = false;
	private ReaderInputStream ris;

	public CustomServletInputStream(Reader reader) {
		 this.ris = new ReaderInputStream(reader, "utf-8");
	}
	
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
	
}
