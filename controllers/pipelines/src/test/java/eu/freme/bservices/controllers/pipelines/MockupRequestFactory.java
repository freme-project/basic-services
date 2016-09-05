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
