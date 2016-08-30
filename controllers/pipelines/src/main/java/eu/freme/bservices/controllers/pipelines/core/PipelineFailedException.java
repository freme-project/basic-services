package eu.freme.bservices.controllers.pipelines.core;

import eu.freme.common.exception.AdditionalFieldsException;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Created by Arne Binder (arne.b.binder@gmail.com) on 22.08.2016.
 */

@SuppressWarnings("serial")
@ResponseStatus(value = HttpStatus.BAD_GATEWAY)
public class PipelineFailedException extends AdditionalFieldsException {

    public PipelineFailedException(JSONObject nestedException, String msg, HttpStatus httpStatusCode){
        super(msg, httpStatusCode);
        addAdditionalField("nested-exception", nestedException);
    }
}
