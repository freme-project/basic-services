package eu.freme.bservices.filters.tika;

import java.io.IOException;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;

public class CustomServletInputStream extends ServletInputStream {

	private byte[] data;
	private boolean finished = false;
	private int idx = 0;
//	private ReaderInputStream ris;

	public CustomServletInputStream(byte[] data) {
//		 this.ris = new ReaderInputStream(reader);
		if(data == null)
			data = new byte[0];
		this.data = data;
	}
	
	@Override
	public int read() throws IOException {
		if(idx == data.length)
			return -1;
		return data[idx++];
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
