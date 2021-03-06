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
package eu.freme.bservices.cloud.loadbalancer;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import eu.freme.bservices.filter.proxy.ProxyService;

@Configuration
//@Component
public class LoadBalancerConfiguration implements EnvironmentAware{

	static final String PROPERTY_KEY = "load-balancer";

	Environment environment;

	@Autowired
	LoadBalancerServlet loadBalancerServlet;
	
	public HashMap<String,LoadBalancingProxyConfig> getProxyConfigurations(){

		RelaxedPropertyResolver propertyResolver = new RelaxedPropertyResolver(environment);
		Map<String, Object> map = propertyResolver.getSubProperties(PROPERTY_KEY+".");

		HashMap<String, LoadBalancingProxyConfig> tempConfigs = new HashMap<String, LoadBalancingProxyConfig>();


		for (String key : map.keySet()) {
			String[] parts = key.split("\\.");
			if (parts.length != 2) {
				throw new RuntimeException("bad parameter: \""+PROPERTY_KEY+"." + key
						+ "\"");
			}

			if (!tempConfigs.containsKey(parts[0])) {
				tempConfigs.put(parts[0], new LoadBalancingProxyConfig());
			}

			tempConfigs.get(parts[0]).setProperty(parts[1], (String) map.get(key));
		}
		HashMap<String,LoadBalancingProxyConfig> proxies = new HashMap<String, LoadBalancingProxyConfig>();
		for(String key: tempConfigs.keySet()){
			LoadBalancingProxyConfig config = tempConfigs.get(key);
			if (config.isValid())
				proxies.put(config.getLocalEndpoint(), config);
			else throw new RuntimeException(
					"Bad parameter configuration for load balancer proxy \"" + key + "\"");
		}

		return proxies;
	}
	
	@Bean
	public ServletRegistrationBean servletRegistrationBean(){
		
		HashMap<String,LoadBalancingProxyConfig> proxies = getProxyConfigurations();
		String[] urlMappings = new String[proxies.size()*2];
		int i=0;
		for( String url : proxies.keySet()){
			if( url.endsWith("/")){
				url = url.substring(0, url.length()-1);
			}
			urlMappings[i] = url;
			urlMappings[i*2+1] = url + "/";
		}
		
		return new ServletRegistrationBean(loadBalancerServlet, urlMappings);
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}
}
