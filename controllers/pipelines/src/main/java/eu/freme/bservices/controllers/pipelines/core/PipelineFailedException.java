package eu.freme.bservices.controllers.pipelines.core;

import eu.freme.common.exception.AdditionalFieldsException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Created by Arne Binder (arne.b.binder@gmail.com) on 22.08.2016.
 */
//public class PipelineFailedException {
@SuppressWarnings("serial")
@ResponseStatus(value = HttpStatus.BAD_GATEWAY)
public class PipelineFailedException extends AdditionalFieldsException {

    //public ExternalServiceFailedException(){}

	/*public ExternalServiceFailedException(String msg){
		super(msg);
	}*/

    public PipelineFailedException(Object nestedException, String msg, HttpStatus httpStatusCode){
        super(msg, httpStatusCode);
        addAdditionalField("nested-exception", nestedException);
    }
}
