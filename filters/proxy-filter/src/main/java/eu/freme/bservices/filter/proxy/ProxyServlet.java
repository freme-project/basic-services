package eu.freme.bservices.filter.proxy;

import java.io.IOException;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.HttpRequest;

import eu.freme.common.exception.InternalServerErrorException;


@SuppressWarnings("serial")
@Component
public class ProxyServlet extends HttpServlet {
	
	Logger logger = Logger.getLogger(ProxyServlet.class);
	
	@Autowired
	ProxyService proxyService;
	
	Map<String,String> proxies;
	
	@Override
	@PostConstruct
	public void init(){
		proxies = proxyService.getProxies();
	}
	
	private void doProxy(HttpServletRequest request, HttpServletResponse response){
		try {
			HttpRequest proxy = proxyService.createProxy(request, proxies.get(request.getRequestURI()));
			proxyService.writeProxyToResponse(response, proxy);
		} catch (IOException | UnirestException e) {
			logger.error("failed", e);
			throw new InternalServerErrorException("Proxy failed");
		}
	}

	@Override
	public void doPost( HttpServletRequest request, HttpServletResponse response){
		doProxy(request,response);
	}

	@Override
	public void doGet( HttpServletRequest request, HttpServletResponse response){
		doProxy(request,response);
	}

	@Override
	public void doPut( HttpServletRequest request, HttpServletResponse response){
		doProxy(request,response);
	}

	@Override
	public void doDelete( HttpServletRequest request, HttpServletResponse response){
		doProxy(request,response);
	}

	@Override
	public void doOptions( HttpServletRequest request, HttpServletResponse response){
		doProxy(request,response);
	}
}