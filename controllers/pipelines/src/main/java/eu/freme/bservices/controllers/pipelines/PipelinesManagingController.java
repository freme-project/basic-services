package eu.freme.bservices.controllers.pipelines;

import com.google.common.base.Strings;
import eu.freme.common.exception.BadRequestException;
import eu.freme.common.persistence.model.Pipeline;
import eu.freme.common.rest.OwnedResourceManagingController;
import org.apache.log4j.Logger;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

/**
 * Created by Arne Binder (arne.b.binder@gmail.com) on 19.01.2016.
 */

@RestController
@RequestMapping("/pipelining/templates")
public class PipelinesManagingController extends OwnedResourceManagingController<Pipeline> {

    Logger logger = Logger.getLogger(PipelinesManagingController.class);

    public static final String labelParameterName = "label";
    public static final String persistParameterName = "persist";

    @Override
    protected Pipeline createEntity(String body, Map<String, String> parameters, Map<String, String> headers) throws BadRequestException {

        // check mandatory body content
        if(Strings.isNullOrEmpty(body)) {
            throw new BadRequestException("Can not create a new pipeline from empty body. Please provide at least one request as body content.");
        }
        // check mandatory label parameter
        if(Strings.isNullOrEmpty(parameters.get(labelParameterName))){
            throw new BadRequestException("Please provide a label for the new pipeline.");
        }

        Pipeline pipeline = new Pipeline();
        updateEntity(pipeline, body, parameters, headers);
        return pipeline;
    }

    @Override
    protected void updateEntity(Pipeline pipeline, String body, Map<String, String> parameters, Map<String, String> headers) throws BadRequestException {

        // process body
        if(!Strings.isNullOrEmpty(body)){
            try {
                pipeline.setSerializedRequests(body);

                // deserialize requests to validate them
                pipeline.deserializeRequests();
            } catch (IOException e) {
                throw new BadRequestException("Could not update pipeline requests with '"+body+"': "+e.getMessage());
            }
        }

        // process parameters
        if (parameters.containsKey(persistParameterName)) {
            boolean toPersist = Boolean.parseBoolean(parameters.get(persistParameterName));
            pipeline.setPersist(toPersist);
        }
        if (parameters.containsKey(labelParameterName)) {
            pipeline.setLabel(parameters.get(labelParameterName));
        }
    }
}
