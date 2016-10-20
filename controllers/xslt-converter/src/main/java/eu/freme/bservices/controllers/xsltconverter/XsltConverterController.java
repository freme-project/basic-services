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
package eu.freme.bservices.controllers.xsltconverter;

import eu.freme.common.conversion.SerializationFormatMapper;
import eu.freme.common.exception.BadRequestException;
import eu.freme.common.exception.FREMEHttpException;
import eu.freme.common.exception.OwnedResourceNotFoundException;
import eu.freme.common.persistence.dao.OwnedResourceDAO;
import eu.freme.common.persistence.model.XsltConverter;
import eu.freme.common.rest.BaseRestController;
import eu.freme.common.rest.RestHelper;
import net.sf.saxon.s9api.*;
import org.apache.log4j.Logger;
import org.ccil.cowan.tagsoup.Parser;
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
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Arne on 05.07.2016.
 */

@RestController
@RequestMapping("/toolbox/xslt-converter/documents")
public class XsltConverterController extends BaseRestController{

    Logger logger = Logger.getLogger(XsltConverterController.class);

    public static final String XML = "text/xml";
    public static final String HTML = "text/html";

    @Autowired
    OwnedResourceDAO<XsltConverter> entityDAO;

    @PostConstruct
    public void init(){
        // add required mimeTypes and in-/outformat values
        getSerializationFormatMapper().put(XML, XML);
        getSerializationFormatMapper().put("xml", XML);
        getSerializationFormatMapper().put("application/xml", XML);
        getSerializationFormatMapper().put(HTML, HTML);
        getSerializationFormatMapper().put("html", HTML);
    }

    @RequestMapping(value = "/{identifier}", method = RequestMethod.POST)
    @Secured({"ROLE_USER", "ROLE_ADMIN"})
    public ResponseEntity<String> filter(
            @PathVariable("identifier") String identifier,
            @RequestHeader(value = "Accept", required = false) String acceptHeader,
            @RequestHeader(value = "Content-Type", required = false) String contentTypeHeader,
            @RequestBody String postBody,
            @RequestParam Map<String, String> allParams
    ) {
        try {
            // check access rights and get plain stylesheet
            XsltConverter converter = entityDAO.findOneByIdentifier(identifier);

            // configure input
            SAXSource source;
            String inFormat = null;
            if(contentTypeHeader!=null){
                inFormat = getSerializationFormatMapper().get(contentTypeHeader.split(";")[0]);
            }
            if(inFormat == null)
                inFormat = XML;
            if(inFormat.equals(HTML)) {
                //// convert html to xml: use TagSoup parser
                Parser htmlParser = new Parser();
                source = new SAXSource(htmlParser, new InputSource(new StringReader(postBody)));
            }else{
                source = new SAXSource(new InputSource(new StringReader(postBody)));
            }

            // configure output
            String outFormat = null;
            if(acceptHeader!=null){
                outFormat = getSerializationFormatMapper().get(acceptHeader.split(";")[0]);
            }
            Serializer serializer = converter.newSerializer();

            if(outFormat == null)
                outFormat = XML;
            if(outFormat.equals(HTML)){
                serializer.setOutputProperty(Serializer.Property.METHOD, "html");
            }else{
                outFormat = XML;
                serializer.setOutputProperty(Serializer.Property.METHOD, "xml");
            }
            serializer.setOutputProperty(Serializer.Property.INDENT, "yes");
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            serializer.setOutputStream(outputStream);

            // do transformation
            Xslt30Transformer transformer = converter.getTransformer();
            XdmValue transformed;
            // see http://saxonica.com/documentation9.6/index.html#!javadoc/net.sf.saxon.s9api/Xslt30Transformer:
            // An Xslt30Transformer must not be used concurrently in multiple threads. It is safe, however, to reuse
            // the object within a single thread to run the same stylesheet several times. Running the stylesheet does
            // not change the context that has been established.
            synchronized (this) {
                // set xsl:param values
                if(allParams!= null && allParams.size() > 0){
                    Map<QName, XdmValue> stylesheetParams = new HashMap<>();
                    for(String key: allParams.keySet()){
                        stylesheetParams.put(new QName(key), new XdmAtomicValue(allParams.get(key)));
                    }
                    transformer.setStylesheetParameters(stylesheetParams);
                }

                transformed = transformer.applyTemplates(source);
            }

            // serialize the result
            serializer.serializeXdmValue(transformed);
            String serialization = outputStream.toString("UTF-8");

            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.add("Content-Type", outFormat);
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
