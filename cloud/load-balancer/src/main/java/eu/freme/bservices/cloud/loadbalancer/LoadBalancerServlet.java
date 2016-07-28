package eu.freme.bservices.cloud.loadbalancer;

import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.HttpRequest;
import eu.freme.bservices.filter.proxy.ProxyService;
import eu.freme.bservices.filter.proxy.exception.BadGatewayException;
import eu.freme.common.exception.ExceptionHandlerService;
import eu.freme.common.exception.InternalServerErrorException;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@SuppressWarnings("serial")
@Component
public class LoadBalancerServlet extends HttpServlet {

	Logger logger = Logger.getLogger(LoadBalancerServlet.class);

	@Autowired
	LoadBalancerConfiguration loadBalancerConfiguration;

	@Autowired
	ExceptionHandlerService exceptionHandlerService;
	
	@Autowired
	ProxyService proxyService;

	// map from endpoint to service uri
	Map<String, LoadBalancingProxyConfig> proxies;
	
    @Autowired
    private DiscoveryClient discoveryClient;

    // for round robin load balancing
    Integer index = 0;

	@Override
	@PostConstruct
	public void init() {
		proxies = loadBalancerConfiguration.getProxyConfigurations();
	}

	private void doProxy(HttpServletRequest request,
			HttpServletResponse response) {
		try {
			LoadBalancingProxyConfig proxyConfig = proxies.get(request.getRequestURI());
			List<ServiceInstance> instances = discoveryClient.getInstances(proxyConfig.getServiceName());
			
			if( instances.size() == 0 ){
				throw new BadGatewayException("Could not find an instance of service \"" + proxyConfig.getServiceName() + "\"");
			}
			
			int thisIndex;
			synchronized(index){
				if(index >= instances.size()){
					index=0;
				}
				thisIndex = index++;
			}
			
			ServiceInstance si = instances.get(thisIndex);
			String targetUri = "http://" + si.getHost() + ":" + si.getPort() + proxyConfig.getTargetEndpoint();
			logger.info("redirect request from " + request.getRequestURI() + " to " + targetUri);
			
			HttpRequest proxy = proxyService.createProxy(request,targetUri);
			proxyService.writeProxyToResponse(response, proxy);
		} catch (IOException | UnirestException e) {
			logger.error("failed", e);
			throw new BadGatewayException("Proxy failed: " + e.getMessage());
		}
	}

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) {
		try {
			doProxy(request, response);
		} catch (Exception e) {
			handleError(request, e, response);
		}
	}

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) {
		try {
			doProxy(request, response);
		} catch (Exception e) {
			handleError(request, e, response);
		}
	}

	@Override
	public void doPut(HttpServletRequest request, HttpServletResponse response) {
		try {
			doProxy(request, response);
		} catch (Exception e) {
			handleError(request, e, response);
		}
	}

	@Override
	public void doDelete(HttpServletRequest request,
			HttpServletResponse response) {
		try {
			doProxy(request, response);
		} catch (Exception e) {
			handleError(request, e, response);
		}
	}

	@Override
	public void doOptions(HttpServletRequest request,
			HttpServletResponse response) {
		try {
			doProxy(request, response);
		} catch (Exception e) {
			handleError(request, e, response);
		}
	}

	public void handleError(HttpServletRequest request, Exception e,
			HttpServletResponse response) {
		ResponseEntity<String> error = exceptionHandlerService.handleError(
				request, e);

		try {
			response.getWriter().write(error.getBody());
			response.setStatus(error.getStatusCode().value());
		} catch (IOException e1) {
			logger.error(e1);
			throw new InternalServerErrorException();
		}
	}
}
