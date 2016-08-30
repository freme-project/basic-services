package eu.freme.bservices.controllers.sparqlconverters;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.rdf.model.Model;

import eu.freme.common.conversion.SerializationFormatMapper;
import eu.freme.common.conversion.rdf.JenaRDFConversionService;
import eu.freme.common.exception.BadRequestException;
import eu.freme.common.exception.FREMEHttpException;
import eu.freme.common.exception.OwnedResourceNotFoundException;
import eu.freme.common.persistence.dao.OwnedResourceDAO;
import eu.freme.common.persistence.model.SparqlConverter;
import eu.freme.common.rest.BaseRestController;
import eu.freme.common.rest.NIFParameterSet;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;

import java.io.ByteArrayOutputStream;
import java.util.Map;

/**
 * Created by Arne Binder (arne.b.binder@gmail.com) on 12.01.2016.
 */
@RestController
@RequestMapping("/toolbox/convert")
public class SparqlConverterController extends BaseRestController {

	Logger logger = Logger.getLogger(SparqlConverterController.class);

	public static final String CSV = "text/comma-separated-values";
	public static final String XML = "text/xml";

	@Autowired
	JenaRDFConversionService jenaRDFConversionService;

	@Autowired
	SerializationFormatMapper serializationFormatMapper;

	@Autowired
	OwnedResourceDAO<SparqlConverter> entityDAO;

	@PostConstruct
	public void init() {
		// RDF types, plain text and json are added automatically (by
		// SerializationFormatMapper)
		serializationFormatMapper.put(CSV, CSV);
		serializationFormatMapper.put("csv", CSV);
		serializationFormatMapper.put("text/csv", CSV);
		serializationFormatMapper.put(XML, XML);
		serializationFormatMapper.put("xml", XML);
		serializationFormatMapper.put("application/xml", XML);
	}

	@RequestMapping(value = "/documents/{identifier}", method = RequestMethod.POST)
	@Secured({ "ROLE_USER", "ROLE_ADMIN" })
	public ResponseEntity<String> filter(
			@PathVariable("identifier") String identifier,
			@RequestHeader(value = "Accept", required = false) String acceptHeader,
			@RequestHeader(value = "Content-Type", required = false) String contentTypeHeader,
			@RequestBody(required = false) String postBody,
			@RequestParam Map<String, String> allParams) {
		try {
			NIFParameterSet nifParameters = this.normalizeNif(postBody,
					acceptHeader, contentTypeHeader, allParams, false);

			SparqlConverter sparqlConverter = entityDAO
					.findOneByIdentifier(identifier);

			Model model = jenaRDFConversionService.unserializeRDF(
					nifParameters.getInput(), nifParameters.getInformat());

			String serialization = null;
			switch (sparqlConverter.getQueryType()) {
			case Query.QueryTypeConstruct:
				QueryExecution qe = null;
	            try{
	            	 qe = sparqlConverter.getFilteredModel(model);
		            Model resultModel = qe.execConstruct();
					serialization = jenaRDFConversionService.serializeRDF(
							resultModel, nifParameters.getOutformat());
	            } finally{
	            	if( qe != null ){
	            		qe.close();
	            	}
	            }
				break;
			case Query.QueryTypeSelect:
				QueryExecution qe2 = sparqlConverter
						.getFilteredResultSet(model);
				ResultSet resultSet = qe2.execSelect();
				// write to a ByteArrayOutputStream
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
				try {
					switch (nifParameters.getOutformat()) {

					case CSV:
						ResultSetFormatter.outputAsCSV(outputStream, resultSet);
						break;
					case XML:
						ResultSetFormatter.outputAsXML(outputStream, resultSet);
						break;
					case JSON:
						ResultSetFormatter
								.outputAsJSON(outputStream, resultSet);
						break;
					case TURTLE:
					case JSON_LD:
					case RDF_XML:
					case N3:
					case N_TRIPLES:
						ResultSetFormatter.outputAsRDF(outputStream,
								jenaRDFConversionService
										.getJenaType(nifParameters
												.getOutformat()), resultSet);
						break;
					default:
						throw new BadRequestException(
								"Unsupported output format for resultset(SELECT) query: "
										+ nifParameters.getOutformat()
										+ ". Only JSON, CSV, XML and RDF types are supported.");
					}
					serialization = new String(outputStream.toByteArray());
				} finally {
					if( outputStream != null ){
						outputStream.close();						
					}
					if( qe2 != null ){
						qe2.close();
					}
				}
				break;
			default:
				throw new BadRequestException(
						"Unsupported sparqlConverter query. Only sparql SELECT and CONSTRUCT are allowed types.");
			}

			HttpHeaders responseHeaders = new HttpHeaders();
			responseHeaders.add("Content-Type", nifParameters.getOutformat()
					.contentType());
			return new ResponseEntity<>(serialization, responseHeaders,
					HttpStatus.OK);

		} catch (AccessDeniedException ex) {
			logger.error(ex.getMessage());
			throw new eu.freme.common.exception.AccessDeniedException(
					ex.getMessage());
		} catch (OwnedResourceNotFoundException ex) {
			logger.error(ex.getMessage());
			throw ex;
		} catch (BadRequestException ex) {
			logger.error(ex.getMessage());
			throw ex;
		} catch (Exception ex) {
			logger.error(ex.getMessage());
			throw new FREMEHttpException(ex.getMessage());
		}
	}

}
