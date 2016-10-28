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
package eu.freme.bservices.filters.postprocessing;

import org.apache.http.client.utils.DateUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.util.*;

/**
 * Created by Arne Binder (arne.b.binder@gmail.com) on 15.12.2015.
 */
public class ModifiableParametersWrappedRequest extends HttpServletRequestWrapper
{
    private final Map<String, String[]> modifiableParameters;
    private Map<String, String[]> allParameters = null;

    private final Map<String, String[]> modifiableHeaders;
    private Set<String> allHeaderNames = null;

    /**
     * Create a new request wrapper that will merge additional parameters into
     * the request object without prematurely reading parameters from the
     * original request.
     *
     * @param request
     * @param additionalParams
     */
    public ModifiableParametersWrappedRequest(final HttpServletRequest request,
                                     final Map<String, String[]> additionalParams,
                                     final Map<String, String[]> additionalHeaders)
    {
        super(request);
        modifiableParameters = new TreeMap<>();
        modifiableParameters.putAll(additionalParams);

        modifiableHeaders = new TreeMap<>();
        // add headers in lowercase to provide case insensitive header access
        for(String key: additionalHeaders.keySet()){
            modifiableHeaders.put(key.toLowerCase(),additionalHeaders.get(key));
        }
    }

    @Override
    public String getParameter(final String name)
    {
        String[] strings = getParameterMap().get(name);
        if (strings != null)
        {
            return strings[0];
        }
        return super.getParameter(name);
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

    @Override
    public Enumeration<String> getParameterNames()
    {
        return Collections.enumeration(getParameterMap().keySet());
    }

    @Override
    public String[] getParameterValues(final String name)
    {
        return getParameterMap().get(name);
    }

    @Override
    public String getQueryString() {
        final List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();

        Enumeration<String> names = getParameterNames();
        while(names.hasMoreElements()){
            String name = names.nextElement();
            params.add(new BasicNameValuePair(name, getParameter(name)));

            /*result += name+"="+getParameter(name);
            if(names.hasMoreElements())
                result += "&";*/
        }
        String result = URLEncodedUtils.format(params,"UTF-8");

        return result.equals("")?null:result;
    }

    @Override
    public long getDateHeader(String name) {
        if(modifiableHeaders.containsKey(name)) {
            String[] headers = modifiableHeaders.get(name.toLowerCase());
            if(headers == null)
                return -1;
            Date date = DateUtils.parseDate(headers[0]);
            return date.getTime();
        }
        return super.getDateHeader(name);
    }

    @Override
    public int getIntHeader(String name){
        if(modifiableHeaders.containsKey(name) ) {
            String[] headers = modifiableHeaders.get(name.toLowerCase());
            if(headers==null)
                return -1;
            return Integer.parseInt(headers[0]);
        }
        return super.getIntHeader(name);
    }

    @Override
    public String getHeader(String name) {
        if(modifiableHeaders.containsKey(name)) {
            String[] headers = modifiableHeaders.get(name.toLowerCase());
            if(headers == null)
                return null;
            return headers[0];
        }
        return super.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        if(modifiableHeaders.containsKey(name)) {
            String[] headers = modifiableHeaders.get(name.toLowerCase());
            if(headers == null)
                return Collections.emptyEnumeration();
            return Collections.enumeration(new HashSet<>(Arrays.asList(headers)));
        }
        return super.getHeaders(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        if (allHeaderNames == null)
        {
            allHeaderNames = new TreeSet<>();
            allHeaderNames.addAll(Collections.list(super.getHeaderNames()));
            for(String key: modifiableHeaders.keySet()){
                if(modifiableHeaders.get(key)!=null)
                    allHeaderNames.add(key);
                else
                    allHeaderNames.remove(key);
            }
            //allHeaderNames.addAll(modifiableHeaders.keySet());
        }
        return Collections.enumeration(allHeaderNames);
    }
}