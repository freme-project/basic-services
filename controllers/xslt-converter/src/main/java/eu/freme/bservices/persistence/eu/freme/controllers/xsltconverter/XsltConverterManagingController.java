package eu.freme.bservices.persistence.eu.freme.controllers.xsltconverter;


import com.google.common.base.Strings;
import eu.freme.common.exception.BadRequestException;
import eu.freme.common.persistence.model.XsltConverter;
import eu.freme.common.rest.OwnedResourceManagingController;
import org.apache.log4j.Logger;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Created by Arne Binder (arne.b.binder@gmail.com) on 12.01.2016.
 */
@RestController
@RequestMapping("/toolbox/xslt-converter/manage")
public class XsltConverterManagingController extends OwnedResourceManagingController<XsltConverter> {

    Logger logger = Logger.getLogger(XsltConverterManagingController.class);

    public static final String identifierParameterName = "name";
    public static final String identifierName = "name"; // depends on SparqlConverter Model class

    @Override
    protected XsltConverter createEntity(String body, Map<String, String> parameters, Map<String, String> headers) throws AccessDeniedException {

        String identifier = parameters.get(identifierParameterName);
        if(Strings.isNullOrEmpty(identifier))
            throw new BadRequestException("No identifier provided! Please set the parameter \""+identifierParameterName+"\" to a valid value.");
        XsltConverter entity = getEntityDAO().findOneByIdentifierUnsecured(identifier);
        if (entity != null)
            throw new BadRequestException("Can not add entity: Entity with identifier: " + identifier + " already exists.");
        // AccessDeniedException can be thrown, if current authentication is the anonymousUser
        return new XsltConverter(identifier, body);
    }

    @Override
    protected void updateEntity(XsltConverter filter, String body, Map<String, String> parameters, Map<String, String> headers) {
        if(body != null && !body.trim().isEmpty()){
            filter.setStylesheet(body);
        }
    }
}
