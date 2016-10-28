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

public class LoadBalancingProxyConfig {

	public enum PropertyName {
		LOCAL_ENDPOINT("local-endpoint"),
		TARGET_ENDPOINT("target-endpoint"),
		SERVICE_NAME("service-name");
		private String value;
		PropertyName(String key){
			this.value = key;
		}
		String getValue(){return value;}
	}

	String localEndpoint;
	String targetEndpoint;
	String serviceName;

	public LoadBalancingProxyConfig(){}

	public void setProperty(String propertyName, String value){
		if(PropertyName.LOCAL_ENDPOINT.getValue().equals(propertyName))
			localEndpoint = value;
		else if(PropertyName.TARGET_ENDPOINT.getValue().equals(propertyName))
			targetEndpoint = value;
		else if(PropertyName.SERVICE_NAME.getValue().equals(propertyName))
			serviceName = value;
		else{
			throw new RuntimeException("unknown parameter: "+ propertyName);
		}
	}

	public boolean isValid(){
		return localEndpoint!=null && targetEndpoint!=null && serviceName!=null;
	}

	public LoadBalancingProxyConfig(String localEndpoint,
			String targetEndpoint, String serviceName) {
		super();
		this.localEndpoint = localEndpoint;
		this.targetEndpoint = targetEndpoint;
		this.serviceName = serviceName;
	}
	public String getLocalEndpoint() {
		return localEndpoint;
	}
	public String getTargetEndpoint() {
		return targetEndpoint;
	}
	public String getServiceName() {
		return serviceName;
	}
}
