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

import com.google.common.base.Strings;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.request.HttpRequestWithBody;
import eu.freme.bservices.controllers.sparqlconverters.SparqlConverterController;
import eu.freme.common.conversion.SerializationFormatMapper;
import eu.freme.common.conversion.rdf.RDFConstants;
import eu.freme.common.exception.BadRequestException;
import eu.freme.common.exception.ExceptionHandlerService;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by Arne Binder (arne.b.binder@gmail.com) on 02.12.2015.
 */
@Component
public class PostprocessingFilter implements Filter {

    private Logger logger = Logger.getLogger(PostprocessingFilter.class);

    public final String HEADER_SECURITY_TOKEN = "X-Auth-Token";

    @Autowired
    ExceptionHandlerService exceptionHandlerService;

    //@Autowired
    //RDFSerializationFormats rdfSerializationFormats;

    @Autowired
    SerializationFormatMapper serializationFormatMapper;

    //@Autowired
    //RDFConversionService rdfConversionService;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {

        if (!(req instanceof HttpServletRequest) || !(res instanceof HttpServletResponse) || req.getParameter("filter")==null) {
            chain.doFilter(req, res);
        }else{
            HttpServletRequest httpRequest = (HttpServletRequest) req;
            HttpServletResponse httpResponse = (HttpServletResponse) res;

            String responseContent = null;
            int responseStatus = HttpStatus.OK.value();
            String responseContentType = SerializationFormatMapper.JSON;

            String filterUrl = "/toolbox/convert/documents/" + req.getParameter("filter");

            try {
                // get requested format of response
                String outTypeString = httpRequest.getParameter("outformat");
                if(Strings.isNullOrEmpty(outTypeString))
                    outTypeString = httpRequest.getParameter("o");
                if(Strings.isNullOrEmpty(outTypeString) && !Strings.isNullOrEmpty(httpRequest.getHeader("Accept")) && !httpRequest.getHeader("Accept").equals("*/*"))
                    outTypeString = httpRequest.getHeader("Accept").split(";")[0];

                String outType = SparqlConverterController.CSV;
                if (!Strings.isNullOrEmpty(outTypeString)) {
                    outType = serializationFormatMapper.get(outTypeString);
                    if(outType == null)
                        throw new BadRequestException("Can not use filter: " + req.getParameter("filter") + " with outformat = \"" + httpRequest.getParameter("outformat") + "\" / accept-header = \"" + httpRequest.getHeader("Accept")+"\". Ensure, that either outformat or accept header contains a valid value!");

                }

                // set Accept header for original request to turtle
                Map<String, String[]> extraParams = new TreeMap<>();
                // delete outformat parameter
                extraParams.put("outformat", new String[]{"turtle"});
                extraParams.put("filter", null);//new String[]{"turtle"});
                Map<String, String[]> extraHeaders = new TreeMap<>();
                extraHeaders.put("Accept", new String[]{RDFConstants.TURTLE});
                HttpServletRequest wrappedRequest = new ModifiableParametersWrappedRequest(httpRequest, extraParams,extraHeaders);

                // wrap the response to allow later modification
                AccessibleHttpServletResponseWrapper wrappedResponse = new AccessibleHttpServletResponseWrapper(httpResponse);

                chain.doFilter(wrappedRequest, wrappedResponse);

                String originalResponseContent = new String(wrappedResponse.getDataStream());

                // postprocess only, if original request was successful
                if(wrappedResponse.getStatus() != HttpStatus.OK.value()){
                    responseContent = originalResponseContent;
                    responseStatus = wrappedResponse.getStatus();
                }else {

                    //// manipulate originalResponseContent here
                    String baseUrl = String.format("%s://%s:%d", httpRequest.getScheme(), httpRequest.getServerName(), httpRequest.getServerPort());

                     HttpRequestWithBody filterRequest = Unirest
                            .post(baseUrl + filterUrl)
                            .header("Content-Type", RDFConstants.TURTLE)
                            .header("Accept", outType);

                    String token = httpRequest.getHeader(HEADER_SECURITY_TOKEN);
                    if(!Strings.isNullOrEmpty(token))
                        filterRequest = filterRequest.header(HEADER_SECURITY_TOKEN, httpRequest.getHeader(HEADER_SECURITY_TOKEN));

                    HttpResponse<String> response = filterRequest
                            .body(originalResponseContent)
                            .asString();

                    if (response.getStatus() == HttpStatus.OK.value()) {
                        responseContentType = response.getHeaders().getFirst("Content-Type");
                    }

                    responseContent = response.getBody();
                    responseStatus = response.getStatus();
                }

            } catch (Exception e) {
                //exceptionHandlerService.writeExceptionToResponse((HttpServletRequest) req, (HttpServletResponse) res, e);
                ResponseEntity<String> responseEntity = exceptionHandlerService.handleError((HttpServletRequest) req, e);
                responseStatus = responseEntity.getStatusCode().value();
                responseContent = responseEntity.getBody();
                //httpResponse.flushBuffer();
            }

            byte[] responseToSend = responseContent.getBytes();

            httpResponse.setContentType(responseContentType);
            httpResponse.setContentLength(responseToSend.length);
            httpResponse.setStatus(responseStatus);

            httpResponse.getWriter().write(responseContent);

        }
    }

    @Override
    public void destroy() {

    }
}
