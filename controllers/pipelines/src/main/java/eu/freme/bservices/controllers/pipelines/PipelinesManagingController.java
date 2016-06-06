package eu.freme.bservices.controllers.pipelines;

import com.fasterxml.jackson.databind.ObjectMapper;
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


    @Override
    protected Pipeline createEntity(String body, Map<String, String> parameters, Map<String, String> headers) throws BadRequestException {
        // just to perform a first validation of the pipeline...
        //Pipeline pipelineInfoObj = Serializer.templateFromJson(body);

        boolean toPersist = Boolean.parseBoolean(parameters.getOrDefault("persist","false"));
        try {
            // the body contains the label, the description and the serializedRequests
            ObjectMapper mapper = new ObjectMapper();
            Pipeline pipeline = mapper.readValue(body, Pipeline.class);
            pipeline.setPersist(toPersist);
            //pipeline.setOwnerToCurrentUser();
            return pipeline;
        } catch (IOException e) {
            throw new BadRequestException("could not create pipeline template from \""+body+"\": "+e.getMessage());
        }


    }

    @Override
    protected void updateEntity(Pipeline pipeline, String body, Map<String, String> parameters, Map<String, String> headers) throws BadRequestException {

        // process body
        if(!Strings.isNullOrEmpty(body) && !body.trim().isEmpty() && !body.trim().toLowerCase().equals("null") && !body.trim().toLowerCase().equals("empty")){
            try {
                // create temp pipeline to get mapped content
                ObjectMapper mapper = new ObjectMapper();
                Pipeline newPipeline = mapper.readValue(body, Pipeline.class);
                if(!newPipeline.getLabel().equals(pipeline.getLabel()))
                    pipeline.setLabel(newPipeline.getLabel());
                if(!newPipeline.getDescription().equals(pipeline.getDescription()))
                    pipeline.setDescription(newPipeline.getDescription());
                if(!newPipeline.getSerializedRequests().equals(pipeline.getSerializedRequests()))
                    pipeline.setSerializedRequests(newPipeline.getSerializedRequests());
            } catch (IOException e) {
                throw new BadRequestException("could not update pipeline template with \""+body+"\": "+e.getMessage());
            }
        }

        // process parameters
        if (parameters.containsKey("persist")) {
            boolean toPersist = Boolean.parseBoolean(parameters.get("persist"));
            if (toPersist != pipeline.isPersist()) {
                pipeline.setPersist(toPersist);
            }
        }
    }
}
