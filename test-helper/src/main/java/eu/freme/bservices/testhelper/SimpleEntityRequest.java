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
package eu.freme.bservices.testhelper;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Arne Binder (arne.b.binder@gmail.com) on 20.01.2016.
 */
public class SimpleEntityRequest {
    private final String body;
    private Map<String, Object> parameters;
    private Map<String, String> headers;

    public SimpleEntityRequest(String body, Map<String, Object> parameters, Map<String, String> headers) {
        this.body = body;
        this.parameters = parameters;
        this.headers = headers;
    }

    public SimpleEntityRequest(SimpleEntityRequest simpleEntityRequest){
        this.body = simpleEntityRequest.body;
        this.parameters = new HashMap<>();
        this.headers = new HashMap<>();
        if(simpleEntityRequest.parameters!=null) {
            this.parameters.putAll(simpleEntityRequest.getParameters());
        }
        if(simpleEntityRequest.headers!=null) {
            this.headers.putAll(simpleEntityRequest.getHeaders());
        }
    }

    public SimpleEntityRequest(String body){
        this.body = body;
    }

    public String getBody() {
        return body;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public SimpleEntityRequest putParameter(String key, Object value){
        if(parameters==null)
            parameters = new HashMap<>();
        parameters.put(key,value);
        return this;
    }

    public SimpleEntityRequest putHeader(String key, String value){
        if(headers==null)
            headers = new HashMap<>();
        headers.put(key, value);
        return this;
    }
}
