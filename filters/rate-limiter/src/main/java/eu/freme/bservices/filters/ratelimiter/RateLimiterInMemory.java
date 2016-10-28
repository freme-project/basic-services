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
package eu.freme.bservices.filters.ratelimiter;

import eu.freme.bservices.filters.ratelimiter.exception.TooManyRequestsException;
import eu.freme.common.exception.InternalServerErrorException;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Jonathan Sauder (jonathan.sauder@student.hpi.de) on 18.11.15.
 */

@Component
public class RateLimiterInMemory implements RateCounterInterface {



    private int max_requests;
    private long max_size;
    private int time_frame;

    Properties rateLimiterProperties;

    private ConcurrentHashMap<String,RateCounterObject> storedRequests = new ConcurrentHashMap<>();

    public RateLimiterInMemory(){
    }

    public void refresh(String rateLimiterYaml) throws IOException{
        clear();
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        URL yamlfile = getClass().getClassLoader().getResource(rateLimiterYaml);
        try {
            FileSystemResource fileSystemResource = new FileSystemResource(yamlfile.getFile());
            yaml.setResources(fileSystemResource);
            rateLimiterProperties = yaml.getObject();
            this.time_frame = (int) rateLimiterProperties.get("time-frame") * 1000;
        } catch (NullPointerException e) {
            throw new IOException(rateLimiterYaml+ "not Found in Filesystem");
        }
    }



    @Override
    public void addToStoredRequests(String identifier, long timestamp, long size, String endpointURI, String userRole) throws TooManyRequestsException {

        String finalIdentifier;
        if (rateLimiterProperties.containsKey("rate-limits." + identifier + "." + endpointURI + "[0]")) {
            max_requests=(int)rateLimiterProperties.get("rate-limits." + identifier + "." + endpointURI + "[0]");
            max_size=(int)rateLimiterProperties.get("rate-limits." + identifier + "." + endpointURI + "[1]");
            finalIdentifier="rate-limits." + identifier + "." + endpointURI;
        } else if (rateLimiterProperties.containsKey("rate-limits." + identifier + ".default[0]")) {
            max_requests=(int)rateLimiterProperties.get("rate-limits." + identifier + ".default[0]");
            max_size=(int)rateLimiterProperties.get("rate-limits." + identifier + ".default[1]");
            finalIdentifier="rate-limits."+identifier+".default";
        }
        else if (rateLimiterProperties.containsKey("rate-limits." + userRole + "." + endpointURI + "[0]")) {
            max_requests=(int)rateLimiterProperties.get("rate-limits." + userRole + "." + endpointURI + "[0]");
            max_size=(int)rateLimiterProperties.get("rate-limits." + userRole + "." + endpointURI + "[1]");
            finalIdentifier="rate-limits." + userRole + "." + endpointURI;
        } else if (rateLimiterProperties.containsKey("rate-limits." + userRole + ".default[0]")) {
            max_requests=(int)rateLimiterProperties.get("rate-limits." + userRole + ".default[0]");
            max_size=(int)rateLimiterProperties.get("rate-limits." + userRole + ".default[1]");
            finalIdentifier="rate-limits."+userRole+".default";
        } else {
            throw new InternalServerErrorException("No identifier found for "+identifier+"with role"+ userRole + "for resource" + endpointURI);
        }

        if (max_size==0 && max_requests==0) {
            return;
        }
        try {
            storedRequests.get(finalIdentifier).add_entry(timestamp, size);
        } catch (NullPointerException e) {
            storedRequests.put(finalIdentifier, new RateCounterObject(time_frame, timestamp, max_requests, size+1, max_size));
        }

    }

    public void clear() {
        storedRequests.clear();
    }


}
