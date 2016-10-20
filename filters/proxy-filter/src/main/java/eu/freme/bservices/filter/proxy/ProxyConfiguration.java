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
package eu.freme.bservices.filter.proxy;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;

@Configuration
public class ProxyConfiguration {

	@Autowired
	ProxyService proxyService;

	@Bean
	public SimpleUrlHandlerMapping registerProxyController() {
		SimpleUrlHandlerMapping suhm = new SimpleUrlHandlerMapping();
		Map<String, String> map = new HashMap<String, String>();
		Map<String, String> proxies = proxyService.getProxies();
		for (String url : proxies.keySet()) {
			map.put(url, "ProxyController");
			map.put(url.substring(0, url.length()) + "*", "ProxyController");
			map.put(url.substring(0, url.length()-1), "ProxyController");
		}
		suhm.setUrlMap(map);
		return suhm;
	}
}
