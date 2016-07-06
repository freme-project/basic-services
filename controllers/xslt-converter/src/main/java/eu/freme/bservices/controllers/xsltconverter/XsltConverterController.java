package eu.freme.bservices.controllers.xsltconverter;

import eu.freme.common.conversion.SerializationFormatMapper;
import eu.freme.common.exception.BadRequestException;
import eu.freme.common.exception.FREMEHttpException;
import eu.freme.common.exception.OwnedResourceNotFoundException;
import eu.freme.common.persistence.dao.OwnedResourceDAO;
import eu.freme.common.persistence.model.XsltConverter;
import eu.freme.common.rest.NIFParameterSet;
import eu.freme.common.rest.RestHelper;
import net.sf.saxon.s9api.*;
import nu.validator.htmlparser.sax.HtmlParser;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;
import org.xml.sax.InputSource;

import javax.annotation.PostConstruct;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.util.Map;

/**
 * Created by Arne on 05.07.2016.
 */

@RestController
@RequestMapping("/toolbox/xslt-converter/documents")
public class XsltConverterController {

    Logger logger = Logger.getLogger(XsltConverterController.class);

    public static final String XML = "text/xml";
    public static final String HTML = "text/html";

    @Autowired
    SerializationFormatMapper serializationFormatMapper;

    @Autowired
    RestHelper rest;

    @Autowired
    OwnedResourceDAO<XsltConverter> entityDAO;

    @PostConstruct
    public void init(){
        // add required mimeTypes and in-/outformat values
        serializationFormatMapper.put(XML, XML);
        serializationFormatMapper.put("xml", XML);
        serializationFormatMapper.put(HTML, HTML);
        serializationFormatMapper.put("html", HTML);
    }

    @RequestMapping(value = "/documents/{identifier}", method = RequestMethod.POST)
    @Secured({"ROLE_USER", "ROLE_ADMIN"})
    public ResponseEntity<String> filter(
            @PathVariable("identifier") String identifier,
            @RequestHeader(value = "Accept", required = false) String acceptHeader,
            @RequestHeader(value = "Content-Type", required = false) String contentTypeHeader,
            @RequestBody(required = false) String postBody,
            @RequestParam Map<String, String> allParams
    ) {
        try {
            NIFParameterSet nifParameters = rest.normalizeNif(postBody,
                    acceptHeader, contentTypeHeader, allParams, false);
            XsltConverter converter = entityDAO.findOneByIdentifier(identifier);

            Processor processor = new Processor(false);
            XsltCompiler compiler = processor.newXsltCompiler();
            // todo: set error listener
            //compiler.setErrorListener(...);

            Xslt30Transformer transformer  = compiler.compile(new StreamSource(new StringReader(converter.getStylesheet()))).load30();
            Serializer out = processor.newSerializer();
            out.setOutputProperty(Serializer.Property.METHOD, "html");
            out.setOutputProperty(Serializer.Property.INDENT, "yes");
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            out.setOutputStream(outputStream);

            //// convert html to xml
            HtmlParser parser = new HtmlParser();
            SAXSource source = new SAXSource (parser, new InputSource (new StringReader(nifParameters.getInput())));

            XdmValue transformed = transformer.applyTemplates(source);
            out.serializeXdmValue(transformed);

            String serialization = outputStream.toString("UTF-8");
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.add("Content-Type", nifParameters.getOutformat().contentType());
            return new ResponseEntity<>(serialization, responseHeaders,
                    HttpStatus.OK);
        } catch (AccessDeniedException ex){
            logger.error(ex.getMessage());
            throw new eu.freme.common.exception.AccessDeniedException(ex.getMessage());
        } catch (OwnedResourceNotFoundException | BadRequestException ex){
            logger.error(ex.getMessage());
            throw ex;
        } catch(Exception ex){
            logger.error(ex.getMessage());
            throw new FREMEHttpException(ex.getMessage());
        }
    }
}
