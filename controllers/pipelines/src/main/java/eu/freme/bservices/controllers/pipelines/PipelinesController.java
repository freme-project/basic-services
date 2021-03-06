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
package eu.freme.bservices.controllers.pipelines;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.common.base.Strings;
import com.google.gson.JsonSyntaxException;
import com.mashape.unirest.http.exceptions.UnirestException;
import eu.freme.bservices.controllers.pipelines.core.*;
import eu.freme.bservices.controllers.pipelines.requests.RequestBuilder;
import eu.freme.bservices.controllers.pipelines.requests.RequestFactory;
import eu.freme.bservices.internationalization.api.InternationalizationAPI;
import eu.freme.common.conversion.SerializationFormatMapper;
import eu.freme.common.conversion.rdf.RDFConstants;
import eu.freme.common.exception.BadRequestException;
import eu.freme.common.exception.InternalServerErrorException;
import eu.freme.common.exception.OwnedResourceNotFoundException;
import eu.freme.common.exception.TemplateNotFoundException;
import eu.freme.common.persistence.dao.OwnedResourceDAO;
import eu.freme.common.persistence.model.Pipeline;
import eu.freme.common.persistence.model.PipelineRequest;
import eu.freme.common.rest.BaseRestController;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Created by Arne Binder (arne.b.binder@gmail.com) on 19.01.2016.
 */

@RestController
@RequestMapping("/pipelining")
public class PipelinesController extends BaseRestController {

    Logger logger = Logger.getLogger(PipelinesController.class);

    @Autowired
    PipelineService pipelineAPI;

    @Autowired
    OwnedResourceDAO<Pipeline> entityDAO;

    /*
     * <p>Calls the pipelining service.</p>
     * <p>Some predefined Requests can be formed using the class {@link RequestFactory}. It also converts request objects
     * from and to JSON.</p>
     * <p><To create custom requests, use the {@link RequestBuilder}.</p>
     * <p>Examples can be found in the unit tests in {@link eu/freme/broker/integration_tests/pipelines}.</p>
     * @param requests	The requests to send to the service.
     * @param stats		If "true": wrap the response of the last request and add timing statistics.
     * @return          The response of the last request.
     * @throws BadRequestException				The contents of the request is not valid.
     * @throws InternalServerErrorException		Something goes wrong that shouldn't go wrong.
     */
    @RequestMapping(value = "/chain",
            method = RequestMethod.POST,
            consumes = "application/json"
    )
    @Secured({"ROLE_USER", "ROLE_ADMIN"})
    public ResponseEntity<String> pipeline(
            @RequestBody String requests,
            @RequestParam(value = "stats", defaultValue = "false", required = false) String stats,
            @RequestParam (value = InternationalizationAPI.switchParameterName, defaultValue = "undefined") String useI18n,
            @RequestParam Map<String, Object> allParams
    ) {
        try {
            useI18n = useI18n.trim().toLowerCase();
            if(!useI18n.equals("true") && !useI18n.equals("false") && !useI18n.equals("undefined")){
                throw new BadRequestException("The parameter "+InternationalizationAPI.switchParameterName+" has the unknown value = '"+useI18n+"'. Use either 'true', 'false' or 'undefined'." );
            }

            boolean wrapResult = Boolean.parseBoolean(stats);
            ObjectMapper mapper = new ObjectMapper();
            List<PipelineRequest> pipelineRequests = mapper.readValue(requests,
                    TypeFactory.defaultInstance().constructCollectionType(List.class, PipelineRequest.class));
            for(PipelineRequest request: pipelineRequests){
                request.unParametrize(allParams);
            }

            WrappedPipelineResponse pipelineResult = pipelineAPI.chain(pipelineRequests, useI18n);
            MultiValueMap<String, String> headers = new HttpHeaders();

            if (wrapResult) {
                headers.add(HttpHeaders.CONTENT_TYPE, SerializationFormatMapper.JSON);
                ObjectWriter ow = new ObjectMapper().writer()
                        .withDefaultPrettyPrinter();
                String serialization = ow.writeValueAsString(pipelineResult);
                return new ResponseEntity<>(serialization, headers, HttpStatus.OK);
            } else {
                headers.add(HttpHeaders.CONTENT_TYPE, pipelineResult.getContent().getContentType());
                PipelineResponse lastResponse = pipelineResult.getContent();
                return new ResponseEntity<>(lastResponse.getBody(), headers, HttpStatus.OK);
            }

        } catch (PipelineFailedException e) {
            logger.error(e.getMessage(), e);
            throw e;
        } catch (JsonSyntaxException e) {
            logger.error(e.getMessage(), e);
            String errormsg = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            throw new BadRequestException("Error detected in the JSON body contents: " + errormsg);
        } catch (UnirestException | JsonMappingException | BadRequestException e) {
            logger.error(e.getMessage(), e);
            throw new BadRequestException(e.getMessage());
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
            // throw an Internal Server exception if anything goes really wrong...
            throw new InternalServerErrorException(t.getMessage());
        }
    }

    /*
     * Calls the pipelining service using an existing template.
     * @param body	The contents to send to the pipeline. This can be a NIF or plain text document.
     * @param id	The id of the pipeline template to use.
     * @param stats		If "true": wrap the response of the last request and add timing statistics.
     * @return		The response of the latest request defined in the template.
     * @throws AccessDeniedException			The pipeline template is not visible by the current user.
     * @throws BadRequestException				The contents of the request is not valid.
     * @throws InternalServerErrorException		Something goes wrong that shouldn't go wrong.
     * @throws TemplateNotFoundException		The pipeline template does not exist.
     */
    @RequestMapping(value = "/chain/{id}",
            method = RequestMethod.POST
    )
    public ResponseEntity<String> pipeline(
            @RequestBody String body,
            @PathVariable String id,
            @RequestParam (value = "stats", defaultValue = "false", required = false) String stats,
            @RequestHeader(value = "Accept", required = false) String acceptHeader,
            @RequestHeader(value = "Content-Type", required = false) String contentTypeHeader,
            @RequestParam(value = InternationalizationAPI.switchParameterName, defaultValue = "undefined") String useI18n,
            @RequestParam(value = "informat", required = false) String informat,
            @RequestParam(value = "outformat", required = false) String outformat,
            @RequestParam Map<String, Object> allParams
    ) throws IOException {
        try {
            Pipeline pipeline = entityDAO.findOneByIdentifier(id);
            List<PipelineRequest> pipelineRequests = pipeline.getRequests();// Serializer.fromJson(pipeline.getRequests());
            PipelineRequest firstRequest = pipelineRequests.get(0);
            PipelineRequest lastRequest = pipelineRequests.get(pipelineRequests.size()-1);

            // process parameter outformat / accept header
            if(!Strings.isNullOrEmpty(outformat)){
                lastRequest.addParameter("outformat", outformat);
            } else if(!Strings.isNullOrEmpty(acceptHeader) && !acceptHeader.equals("*/*")){
                lastRequest.addHeader("accept", acceptHeader);
            }

            // process parameter informat / content type header
            //// process content-type header (parameter informat will be added via allParams)
            if(!Strings.isNullOrEmpty(informat)){
                firstRequest.addParameter("informat", informat);
            }
            else if(!Strings.isNullOrEmpty(contentTypeHeader) && !contentTypeHeader.equals("*/*")){
                firstRequest.addHeader("content-type", contentTypeHeader);
            }

            // add request body to first pipeline request
            firstRequest.setBody(body);

            // use pipeline object to get the deserialized requests
            pipeline.setRequests(pipelineRequests);
            pipeline.serializeRequests();
            return pipeline(pipeline.getSerializedRequests(), stats, useI18n, allParams);
        } catch (org.springframework.security.access.AccessDeniedException | InsufficientAuthenticationException ex) {
            logger.error(ex.getMessage(), ex);
            throw new AccessDeniedException(ex.getMessage());
        } catch (JsonSyntaxException jsonException) {
            logger.error(jsonException.getMessage(), jsonException);
            String errormsg = jsonException.getCause() != null ? jsonException.getCause().getMessage() : jsonException.getMessage();
            throw new BadRequestException("Error detected in the JSON body contents: " + errormsg);
        } catch (OwnedResourceNotFoundException ex) {
            logger.error(ex.getMessage(), ex);
            throw new TemplateNotFoundException("Could not find the pipeline template with id " + id);
        }
    }

}
