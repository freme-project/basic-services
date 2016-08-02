package eu.freme.bservices.filter.proxy.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import eu.freme.common.exception.FREMEHttpException;

@SuppressWarnings("serial")
@ResponseStatus(value=HttpStatus.NOT_FOUND, reason="The requested API endpoint does not exist.")
public class EndpointDoesNotExistException extends FREMEHttpException{

	public EndpointDoesNotExistException(){
		super();
	}
	
	public EndpointDoesNotExistException(String msg){
		super(msg);
	}
}
