package eu.freme.bservices.controller.pipeliningcontroller;

import com.google.common.base.Strings;
import com.google.gson.JsonSyntaxException;
import com.mashape.unirest.http.exceptions.UnirestException;
import eu.freme.bservices.controller.pipeliningcontroller.core.PipelineResponse;
import eu.freme.bservices.controller.pipeliningcontroller.core.PipelineService;
import eu.freme.bservices.controller.pipeliningcontroller.core.ServiceException;
import eu.freme.bservices.controller.pipeliningcontroller.core.WrappedPipelineResponse;
import eu.freme.bservices.controller.pipeliningcontroller.requests.SerializedRequest;
import eu.freme.bservices.controller.pipeliningcontroller.serialization.Pipeline_local;
import eu.freme.bservices.controller.pipeliningcontroller.serialization.Serializer;
import eu.freme.common.conversion.rdf.RDFConstants;
import eu.freme.common.exception.BadRequestException;
import eu.freme.common.exception.InternalServerErrorException;
import eu.freme.common.exception.OwnedResourceNotFoundException;
import eu.freme.common.exception.TemplateNotFoundException;
import eu.freme.common.persistence.model.OwnedResource;
import eu.freme.common.persistence.model.Pipeline;
import eu.freme.common.rest.RestrictedResourceManagingController;
/*import eu.freme.eservices.pipelines.core.PipelineResponse;
import eu.freme.eservices.pipelines.core.PipelineService;
import eu.freme.eservices.pipelines.core.ServiceException;
import eu.freme.eservices.pipelines.core.WrappedPipelineResponse;
import eu.freme.eservices.pipelines.requests.SerializedRequest;
import eu.freme.eservices.pipelines.serialization.Serializer;*/
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

import java.util.List;
import java.util.Map;

/**
 * Created by Arne Binder (arne.b.binder@gmail.com) on 19.01.2016.
 */

@RestController
@RequestMapping("/pipelines")
public class PipeliningController extends RestrictedResourceManagingController<Pipeline> {

    Logger logger = Logger.getLogger(PipeliningController.class);

    @Autowired
    PipelineService pipelineAPI;

    /**
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
            consumes = "application/json",
            produces = {"text/turtle", "application/json", "application/ld+json", "application/n-triples", "application/rdf+xml", "text/n3", "text/html"}
    )
    @Secured({"ROLE_USER", "ROLE_ADMIN"})
    public ResponseEntity<String> pipeline(@RequestBody String requests, @RequestParam(value = "stats", defaultValue = "false", required = false) String stats) {
        try {
            boolean wrapResult = Boolean.parseBoolean(stats);
            List<SerializedRequest> serializedRequests = Serializer.fromJson(requests);
            WrappedPipelineResponse pipelineResult = pipelineAPI.chain(serializedRequests);
            MultiValueMap<String, String> headers = new HttpHeaders();

            if (wrapResult) {
                headers.add(HttpHeaders.CONTENT_TYPE, RDFConstants.RDFSerialization.JSON.contentType());
                return new ResponseEntity<>(Serializer.toJson(pipelineResult), headers, HttpStatus.OK);
            } else {
                headers.add(HttpHeaders.CONTENT_TYPE, pipelineResult.getContent().getContentType());
                PipelineResponse lastResponse = pipelineResult.getContent();
                return new ResponseEntity<>(lastResponse.getBody(), headers, HttpStatus.OK);
            }

        } catch (ServiceException serviceError) {
            // TODO: see if this can be replaced by excsption(s) defined in the broker.
            logger.error(serviceError.getMessage(), serviceError);
            MultiValueMap<String, String> headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_TYPE, serviceError.getResponse().getContentType());
            return new ResponseEntity<>(serviceError.getMessage(), headers, serviceError.getStatus());
        } catch (JsonSyntaxException jsonException) {
            logger.error(jsonException.getMessage(), jsonException);
            String errormsg = jsonException.getCause() != null ? jsonException.getCause().getMessage() : jsonException.getMessage();
            throw new BadRequestException("Error detected in the JSON body contents: " + errormsg);
        } catch (UnirestException unirestException) {
            logger.error(unirestException.getMessage(), unirestException);
            throw new BadRequestException(unirestException.getMessage());
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
            // throw an Internal Server exception if anything goes really wrong...
            throw new InternalServerErrorException(t.getMessage());
        }
    }

    /**
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
            method = RequestMethod.POST,
            consumes = {"text/turtle", "application/json", "application/ld+json", "application/n-triples", "application/rdf+xml", "text/n3", "text/plain"},
            produces = {"text/turtle", "application/json", "application/ld+json", "application/n-triples", "application/rdf+xml", "text/n3"}
    )
    public ResponseEntity<String> pipeline(@RequestBody String body, @PathVariable long id, @RequestParam (value = "stats", defaultValue = "false", required = false) String stats) {
        try {
            Pipeline pipeline = getEntityDAO().findOneById(id);
            List<SerializedRequest> serializedRequests = Serializer.fromJson(pipeline.getSerializedRequests());
            serializedRequests.get(0).setBody(body);
            return pipeline(Serializer.toJson(serializedRequests), stats);
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


    @Override
    protected Pipeline createEntity(String id, OwnedResource.Visibility visibility, String description, String body, Map<String, String> parameters) throws AccessDeniedException {
        // just to perform a first validation of the pipeline...
        Pipeline_local pipelineInfoObj = Serializer.templateFromJson(body);

        boolean toPersist = Boolean.parseBoolean(parameters.getOrDefault("persist","false"));
        Pipeline pipeline = new Pipeline(
                visibility,
                pipelineInfoObj.getLabel(),
                pipelineInfoObj.getDescription(),
                Serializer.toJson(pipelineInfoObj.getSerializedRequests()),
                toPersist);

        return pipeline;
    }

    @Override
    protected void updateEntity(Pipeline pipeline, String body, Map<String, String> parameters) {

        // process body
        if(!Strings.isNullOrEmpty(body) && !body.trim().isEmpty() && !body.trim().toLowerCase().equals("null") && !body.trim().toLowerCase().equals("empty")){
            Pipeline_local pipelineInfoObj = Serializer.templateFromJson(body);
            String newLabel = pipelineInfoObj.getLabel();
            if (newLabel != null && !newLabel.equals(pipeline.getLabel())) {
                pipeline.setLabel(newLabel);
            }
            List<SerializedRequest> oldRequests = Serializer.fromJson(pipeline.getSerializedRequests());
            List<SerializedRequest> newRequests = pipelineInfoObj.getSerializedRequests();
            if (newRequests != null && !newRequests.equals(oldRequests)) {
                pipeline.setSerializedRequests(Serializer.toJson(newRequests));
            }
        }

        // process parameters
        if (parameters.containsKey("persist")) {
            boolean toPersist = Boolean.parseBoolean(parameters.get("persist"));
            if (toPersist != pipeline.isPersistent()) {
                pipeline.setPersist(toPersist);
            }
        }
    }
}
