package eu.freme.bservices.testhelper;

import com.hp.hpl.jena.shared.AssertionFailureException;
import com.mashape.unirest.http.HttpResponse;

import eu.freme.common.conversion.SerializationFormatMapper;
import eu.freme.common.conversion.rdf.RDFConstants;
import eu.freme.common.conversion.rdf.RDFConversionService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * Created by Arne Binder (arne.b.binder@gmail.com) on 06.01.2016.
 */
@Component
public class ValidationHelper {

    @Autowired
    public RDFConversionService converter;

    /**
     * General NIF Validation: can be used to test all NiF Responses on their validity.
     * @param response the response containing the NIF content
     * @param expectedSerializationFormat the NIF format
     * @throws IOException
     * @deprecated use {@link #validateNIFResponse(HttpResponse, String)} instead
     */
    @Deprecated
    public void validateNIFResponse(HttpResponse<String> response, RDFConstants.RDFSerialization expectedSerializationFormat) throws IOException {

        validateNIFResponse(response,expectedSerializationFormat.contentType());

    }

    /**
     * General NIF Validation: can be used to test all NiF Responses on their validity.
     * @param response the response containing the NIF content
     * @param expectedSerializationFormat the NIF format
     * @throws IOException
     */
    public void validateNIFResponse(HttpResponse<String> response, String expectedSerializationFormat) throws IOException {

        //basic tests on response
        assertEquals(HttpStatus.OK.value(), response.getStatus());
        assertTrue(response.getBody().length() > 0);
        assertTrue(!response.getHeaders().isEmpty());
        assertNotNull(response.getHeaders().get("content-type"));

        //Tests if headers are correct.
        String contentType = response.getHeaders().get("content-type").get(0).split(";")[0];
        assertEquals(expectedSerializationFormat, contentType);

        if (!expectedSerializationFormat.equals(SerializationFormatMapper.JSON)) {
            // validate RDF
            try {
                assertNotNull(converter.unserializeRDF(response.getBody(), expectedSerializationFormat));
            } catch (Exception e) {
                throw new AssertionFailureException("RDF validation failed");
            }
        }

        // validate NIF
        /* the Validate modul is available just as SNAPSHOT.
        if (nifformat == RDFConstants.RDFSerialization.TURTLE) {
            Validate.main(new String[]{"-i", response.getBody(), "--informat","turtle"});
        } else if (nifformat == RDFConstants.RDFSerialization.RDF_XML) {
            Validate.main(new String[]{"-i", response.getBody(), "--informat","rdfxml"});
        } else {
            //Not implemented yet: n3, n-triples, json-ld
            Validate.main(new String[]{"-i", response.getBody()});
        }
        */
    }

}
