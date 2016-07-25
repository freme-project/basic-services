package eu.freme.bservices.filter.proxy;

import java.io.IOException;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.HttpRequest;

import eu.freme.bservices.filter.proxy.exception.BadGatewayException;
import eu.freme.bservices.filter.proxy.exception.EndpointDoesNotExistException;
import eu.freme.common.exception.ExceptionHandlerService;
import eu.freme.common.exception.InternalServerErrorException;

@Controller("ProxyController")
public class ProxyController{

	Logger logger = Logger.getLogger(ProxyController.class);

	@Autowired
	ProxyService proxyService;

	@Autowired
	ExceptionHandlerService exceptionHandlerService;

	Map<String, String> proxies;

	@PostConstruct
	public void init() {
		proxies = proxyService.getProxies();
	}

	private ResponseEntity<String> doProxy(HttpServletRequest request) {
		try {
			String url = request.getServletPath();
			if( !url.endsWith("/")){
				url += "/";
			}
			for( String prefix : proxies.keySet() ){
				if( url.startsWith(prefix)){
					
					String pathParam = url.substring(prefix.length());
					
					String targetUrl = proxies.get(prefix);
					if( pathParam.length() > 0){
						if( !targetUrl.endsWith("/")){
							targetUrl += "/";
						}
						
						if( pathParam.startsWith("/")){
							pathParam = pathParam.substring(1);
						}
					}
					targetUrl += pathParam;

					HttpRequest proxy = proxyService.createProxy(request, targetUrl);
					//proxyService.writeProxyToResponse(response, proxy);
					return proxyService.createResponse(proxy);					
				}
			}
			
			// endpoint does not exist
			String msg = "The API endpoint \"" + request.getServletPath() + "\" does not exist.";
			throw new EndpointDoesNotExistException(msg);
			
		} catch (IOException | UnirestException e) {
			logger.error("failed", e);
			throw new BadGatewayException("Proxy failed: " + e.getMessage());
		}
	}

	@RequestMapping
	public ResponseEntity<String> doPost(HttpServletRequest request) {
		return doProxy(request);
	}

}
