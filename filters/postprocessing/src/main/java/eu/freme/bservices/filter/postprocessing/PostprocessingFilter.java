package eu.freme.bservices.filter.postprocessing;

import com.google.common.base.Strings;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import eu.freme.common.conversion.rdf.RDFConstants;
import eu.freme.common.conversion.rdf.RDFConversionService;
import eu.freme.common.conversion.rdf.RDFSerializationFormats;
import eu.freme.common.exception.BadRequestException;
import eu.freme.common.exception.ExceptionHandlerService;
import eu.freme.common.exception.FREMEHttpException;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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

    @Autowired
    ExceptionHandlerService exceptionHandlerService;

    @Autowired
    RDFSerializationFormats rdfSerializationFormats;

    @Autowired
    RDFConversionService rdfConversionService;

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

            // get requested format of response
            String outTypeString = httpRequest.getParameter("outformat");
            if(Strings.isNullOrEmpty(outTypeString))
                outTypeString = httpRequest.getParameter("o");
            if(Strings.isNullOrEmpty(outTypeString))
                outTypeString = httpRequest.getHeader("Accept").split(";")[0];

            RDFConstants.RDFSerialization outType = RDFConstants.RDFSerialization.CSV;
            if (!Strings.isNullOrEmpty(outTypeString)) {
                outType = rdfSerializationFormats.get(outTypeString);
                if(outType == null)
                    throw new BadRequestException("Can not use filter: " + req.getParameter("filter") + " with outformat/Accept-header: " + httpRequest.getParameter("outformat") + "/" + httpRequest.getHeader("Accept"));

            }

            // set Accept header for original request to turtle
            Map<String, String[]> extraParams = new TreeMap<>();
            // delete outformat parameter
            extraParams.put("outformat", null);//new String[]{"turtle"});
            Map<String, String[]> extraHeaders = new TreeMap<>();
            extraHeaders.put("Accept", new String[]{RDFConstants.RDFSerialization.TURTLE.contentType()});
            HttpServletRequest wrappedRequest = new ModifiableParametersWrappedRequest(httpRequest, extraParams,extraHeaders);

            // wrap the response to allow later modification
            AccessibleHttpServletResponseWrapper wrappedResponse = new AccessibleHttpServletResponseWrapper(httpResponse);

            chain.doFilter(wrappedRequest, wrappedResponse);


            String responseContent = new String(wrappedResponse.getDataStream());
            String responseContentType;

            //// manipulate responseContent here
            try {
                String baseUrl = String.format("%s://%s:%d", httpRequest.getScheme(), httpRequest.getServerName(), httpRequest.getServerPort());

                HttpResponse<String> response = Unirest
                        .post(baseUrl + "/toolbox/convert/documents/"+req.getParameter("filter"))
                        .header("Content-Type", RDFConstants.RDFSerialization.TURTLE.contentType())
                        .header("Accept", outType.contentType())
                        .body(responseContent)
                        .asString();

                if (response.getStatus() != HttpStatus.OK.value()) {
                    throw new FREMEHttpException(
                            "Postprocessing filter failed with status code: "
                                    + response.getStatus() + " (" + response.getStatusText() + ")",
                            HttpStatus.valueOf(response.getStatus()));
                }

                responseContent = response.getBody();
                responseContentType = response.getHeaders().getFirst("Content-Type");

            } catch (UnirestException e) {
                throw new FREMEHttpException(e.getMessage());
            }
            ////

            byte[] responseToSend = responseContent.getBytes();

            httpResponse.setContentType(responseContentType);
            httpResponse.setContentLength(responseToSend.length);

            ServletOutputStream outputStream = res.getOutputStream();
            outputStream.write(responseToSend);
            outputStream.flush();
            outputStream.close();

        }
    }

    @Override
    public void destroy() {

    }
}
