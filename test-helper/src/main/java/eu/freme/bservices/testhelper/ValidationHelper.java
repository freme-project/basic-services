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
