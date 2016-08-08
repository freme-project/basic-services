package eu.freme.bservices.controllers.nifconverter;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import eu.freme.bservices.internationalization.api.InternationalizationAPI;
import eu.freme.bservices.internationalization.okapi.nif.converter.ConversionException;
import eu.freme.common.conversion.rdf.RDFConversionService;
import eu.freme.common.exception.BadRequestException;
import eu.freme.common.exception.FREMEHttpException;
import eu.freme.common.exception.InternalServerErrorException;
import eu.freme.common.rest.NIFParameterFactory;
import eu.freme.common.rest.NIFParameterSet;
import eu.freme.common.rest.RestHelper;
import org.apache.log4j.Logger;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.apache.tika.sax.ToXMLContentHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import eu.freme.common.conversion.rdf.RDFConstants;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;

import javax.servlet.http.HttpServletRequest;

/**
 * This implements an anything to NIF converter.
 * You can send plaintext or the input formats
 * that e-Internationalisation supports to the
 * endpoint and it returns NIF.
 *
 * Created by Arne Binder (arne.b.binder@gmail.com) on 09.03.2016.
 */
@RestController
public class NifConverterController {

    Logger logger = Logger.getLogger(NifConverterController.class);

    @Autowired
    RestHelper restHelper;

    @Autowired
    RDFConversionService rdfConversionService;

    @Autowired
    NIFParameterFactory nifParameterFactory;

    @RequestMapping(value = "/toolbox/nif-converter", method = RequestMethod.POST)
    public ResponseEntity<String> convert(
			HttpServletRequest request, 
            @RequestHeader(value = "Accept") String acceptHeader,
            @RequestHeader(value = "Content-Type") String contentTypeHeader,
            @RequestBody(required = false) String postBody,
            @RequestParam Map<String, String> allParams) {

    	if( ( allParams.containsKey("informat") && allParams.get("informat").equals("TIKAFile") ) ||
    		(allParams.containsKey("i") && allParams.get("i").equals("TIKAFile"))	){
			File inputFile = null;
		    File tempDir = new File(System.getProperty("java.io.tmpdir"));
		    MultipartFile file1 = null;
		    if (request instanceof MultipartHttpServletRequest){
		    	MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
		    	file1 = multipartRequest.getFile("inputFile");
		    	if(file1==null){
		    		String msg = "There is no input file when TIKAFile informat has been specified";
		            logger.error(msg);
		            throw new BadRequestException(msg);
		    	}
		    	if (!file1.isEmpty()) {
		    		try {
		    			File tempFile = new File(tempDir + File.separator + file1.getOriginalFilename());
		    			tempFile.createNewFile(); 
		    			FileOutputStream fos = new FileOutputStream(tempFile); 
		    			fos.write(file1.getBytes());
		    			fos.close(); 
		    			inputFile = tempFile;
		    		} catch (Exception e) {
			    		String msg = "There was a problem copying the inputfile";
			            logger.error(msg);
			            throw new BadRequestException(msg);
		    		}
		    	} else {
		    		String msg = "The inputfile is empty";
		            logger.error(msg);
		            throw new BadRequestException(msg);
		    	}
		    }
		    else{
	    		String msg = "There is no file because it is not a multipart request";
	            logger.error(msg);
	            throw new BadRequestException(msg);
		    }
    		String text="";
			try{
				AutoDetectParser parser = new AutoDetectParser();
				BodyContentHandler handler = new BodyContentHandler();
				Metadata metadata = new Metadata();
				InputStream stream = new FileInputStream(inputFile);
				try{
					parser.parse(stream, handler, metadata);
					text = handler.toString();
				} finally {
					stream.close();            // close the stream
				}
			}
			catch(Exception e){
	    		String msg = "There was a problem parsing the input file";
	            logger.error(msg);
	            throw new BadRequestException(msg);
			}
			postBody = text;
    		allParams.put("input", text);
    		allParams.put("i", text);
    		allParams.put("informat", "text");
    		allParams.put("f", "text");
    	}

    	NIFParameterSet nifParameters =  restHelper.normalizeNif(postBody,
                acceptHeader, contentTypeHeader, allParams, false);
        try {
            Model model;
            if(nifParameters.getInformat().equals(RDFConstants.RDFSerialization.PLAINTEXT)){
                model = ModelFactory.createDefaultModel();
                rdfConversionService.plaintextToRDF(model, nifParameters.getInput(), null, nifParameterFactory.getDefaultPrefix());
            }else {
                model = rdfConversionService.unserializeRDF(postBody, nifParameters.getInformat());
            }
            return restHelper.createSuccessResponse(model, nifParameters.getOutformat());
        }catch (ConversionException e){
            logger.error("Error", e);
            throw new InternalServerErrorException("Conversion from \""
                    + contentTypeHeader + "\" to NIF failed");
        }catch (FREMEHttpException e){
            logger.error("Error", e);
            throw e;
        } catch (Exception e) {
            logger.error("Error", e);
            throw new BadRequestException(e.getMessage());
        }
    }
}
