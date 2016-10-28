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
package eu.freme.bservices.controllers.pipelines;

import eu.freme.bservices.controllers.pipelines.requests.RequestFactory;
import eu.freme.bservices.testhelper.api.MockupEndpoint;
import eu.freme.common.persistence.model.PipelineRequest;

/**
 * Created by Arne on 21.01.2016.
 */
public class MockupRequestFactory {

    static final String mockupEntitySpotlight = "/pipelines-mockupEntitySpotlight.ttl";
    static final String mockupEntityFremeNER = "/pipelines-mockupEntityFremeNER.ttl";
    static final String mockupLink = "/pipelines-mockupLink.ttl";
    static final String mockupTranslation = "/";
    static final String mockupTerminology = "/pipelines-mockupTerminology.ttl";

    private final String baseURL;


    public MockupRequestFactory(String baseURL){
        this.baseURL = baseURL;
    }

    @SuppressWarnings("unused")
    public PipelineRequest createEntitySpotlight(final String text, final String language) {
        PipelineRequest request = RequestFactory.createEntitySpotlight(text, language);
        request.setEndpoint(baseURL + MockupEndpoint.path +mockupEntitySpotlight);
        return request;
    }

    @SuppressWarnings("unused")
    public PipelineRequest createEntitySpotlight(final String language) {
        PipelineRequest request = RequestFactory.createEntitySpotlight(language);
        request.setEndpoint(baseURL + MockupEndpoint.path + mockupEntitySpotlight);
        return request;
    }


    @SuppressWarnings("unused")
    public PipelineRequest createEntityFremeNER(final String text, final String language, final String dataSet) {
        PipelineRequest request = RequestFactory.createEntityFremeNER(text, language, dataSet);
        request.setEndpoint(baseURL + MockupEndpoint.path + mockupEntityFremeNER);
        return request;
    }

    @SuppressWarnings("unused")
    public PipelineRequest createEntityFremeNER(final String language, final String dataSet) {
        PipelineRequest request = RequestFactory.createEntityFremeNER(language, dataSet);
        request.setEndpoint(baseURL + MockupEndpoint.path +mockupEntityFremeNER);
        return request;
    }

    @SuppressWarnings("unused")
    public PipelineRequest createLink(final String templateID) {
        PipelineRequest request = RequestFactory.createLink(templateID);
        request.setEndpoint(baseURL + MockupEndpoint.path +mockupLink);
        return request;
    }
/*
    @SuppressWarnings("unused")
    public PipelineRequest createTranslation(final String text, final String sourceLang, final String targetLang) {
        PipelineRequest request = RequestFactory.createTranslation(text, sourceLang, targetLang);
        request.setEndpoint(baseURL + MockupEndpoint.path +mockupTranslation);
        return request;
    }


    @SuppressWarnings("unused")
    public PipelineRequest createTranslation(final String sourceLang, final String targetLang) {
        PipelineRequest request = RequestFactory.createTranslation(sourceLang, targetLang);
        request.setEndpoint(baseURL + MockupEndpoint.path +mockupTranslation);
        return request;
    }
*/
    @SuppressWarnings("unused")
    public PipelineRequest createTerminology(final String sourceLang, final String targetLang) {
        PipelineRequest request = RequestFactory.createTerminology(sourceLang, targetLang);
        request.setEndpoint(baseURL + MockupEndpoint.path +mockupTerminology);
        return request;
    }



}
