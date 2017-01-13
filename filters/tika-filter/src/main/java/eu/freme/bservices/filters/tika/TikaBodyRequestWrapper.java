package eu.freme.bservices.filters.tika;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.apache.commons.io.IOUtils;

public class TikaBodyRequestWrapper extends HttpServletRequestWrapper{

		String nifBody;
		HttpServletRequest httpRequest;
		String contentType;
	    private Map<String, String[]> allParameters = null;
	    private final Map<String, String[]> modifiableParameters;


		public TikaBodyRequestWrapper(HttpServletRequest request, String nifContent, final Map<String, String[]> additionalParams){
			super(request);
			this.nifBody = nifContent;
			this.httpRequest = request;
			this.contentType = "text/turtle";
	        modifiableParameters = new TreeMap<>();
	        modifiableParameters.putAll(additionalParams);
		}

		@Override
		public ServletInputStream getInputStream() throws java.io.IOException{
			InputStream in = IOUtils.toInputStream(nifBody, "UTF-8");
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			ServletInputStream sis = new CustomServletInputStream(reader);
			
			return sis; 

		}

		@Override
		public BufferedReader getReader() throws IOException{
			InputStream in = IOUtils.toInputStream(nifBody, "UTF-8");
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			return reader;
		}
		
		/**
		 * informat must be set to turtle instead of TIKAFile and 
		 * input and i must return the converted turtle string
		 */
		@Override 
		public String getParameter(String name){
			
			String param = 	httpRequest.getParameter(name);

			if(name.equals("input") || name.equals("i")){
				param = this.nifBody;
			}else if(name.equals("informat")){
				param = "turtle";
			}else if(name.equals("intype")){
				param = "file";
			}
			return param;
		}
		
		@Override
		public String[] getParameterValues(String name){
			
			String param = "";
			String[] vals = new String[1];
			if(name.equals("input") || name.equals("i")){
				param = this.nifBody;
				vals[0] = param;
			}else if(name.equals("informat")){
				param = "turtle";
				vals[0] = param;
			}else if(name.equals("intype")){
				param = "file";
				vals[0] = param;
			}else{
				vals = httpRequest.getParameterValues(name);
			}
			return vals;
		}
		
		@Override
		public String getContentType(){
			return this.contentType;
		}
		
		
		
		@Override
		public String getHeader(String name){
			if(name.equals("Content-Type")){
				return contentType;
			}else{
				return httpRequest.getHeader(name);
			}
		}
		
		
		@Override
		public Enumeration<String> getHeaders(String name){

			if(name.equals("Content-Type")){
				List<String> wrapperList = new ArrayList<String>();
				wrapperList.add(contentType);

				return Collections.enumeration(wrapperList);

			}
			return httpRequest.getHeaders(name);
		}
		
		
	    @Override
	    public Map<String, String[]> getParameterMap()
	    {
	        if (allParameters == null)
	        {
	            allParameters = new TreeMap<>();
	            allParameters.putAll(super.getParameterMap());
	            for(String key: modifiableParameters.keySet()){
	                if(modifiableParameters.get(key)!=null)
	                    allParameters.put(key, modifiableParameters.get(key));
	                else
	                    // remove "deleted" entries
	                    allParameters.remove(key);
	            }
	            //allParameters.putAll(modifiableParameters);
	        }
	        //Return an unmodifiable collection because we need to uphold the interface contract.
	        return Collections.unmodifiableMap(allParameters);
	    }

	
}
