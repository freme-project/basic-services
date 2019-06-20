package eu.freme.bservices.filters.simplifylogin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
/**
 * @author Jan Nehring - jan.nehring@dfki.de
 */
public class OverwriteHeaderRequest extends HttpServletRequestWrapper {

	String token = null;
	
	public OverwriteHeaderRequest(HttpServletRequest request, String token) {
		super(request);
		this.token = token;
	}
	
	public String getHeader(String name) {
		if(name != null && name.toLowerCase().equals("x-auth-token")) {
			return token;
		} else {
			return super.getHeader(name);
		}
	}
	
	public Enumeration<String> getHeaders(){
		Enumeration<String> eOriginal = getHeaderNames();
		ArrayList<String> list = new ArrayList<String>();
		while(eOriginal.hasMoreElements()) {
			String next = eOriginal.nextElement();
			list.add(getHeader(next));
		}
		return Collections.enumeration(list);
	}
	
	public Enumeration<String> getHeaderNames(){
		Enumeration<String> eOriginal = super.getHeaderNames();
		ArrayList<String> list = new ArrayList<String>();
		boolean found = false;
		while(eOriginal.hasMoreElements()) {
			String next = eOriginal.nextElement();
			list.add(next);
			if(next.toLowerCase().contentEquals("x-auth-token")) {
				found = true;
			}
		}
		if(!found) {
			list.add("X-Auth-Token");
		}
		return Collections.enumeration(list);
	}

}
