package eu.freme.bservices.controllers.sparqlconverters;


import com.google.common.base.Strings;
import eu.freme.common.exception.BadRequestException;
import eu.freme.common.persistence.model.SparqlConverter;
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
@RequestMapping("/toolbox/convert/manage")
public class SparqlConverterManagingController extends OwnedResourceManagingController<SparqlConverter> {

    Logger logger = Logger.getLogger(SparqlConverterManagingController.class);

    @Override
    protected SparqlConverter createEntity(String body, Map<String, String> parameters, Map<String, String> headers) throws AccessDeniedException {

        String identifier = parameters.get(getEntityDAO().getIdentifierName());
        if(Strings.isNullOrEmpty(identifier))
            throw new BadRequestException("No identifier provided! Please set the parameter '"+getEntityDAO().getIdentifierName()+"' to a valid value.");
        SparqlConverter entity = getEntityDAO().findOneByIdentifierUnsecured(identifier);
        if (entity != null)
            throw new BadRequestException("Can not add entity: Entity with identifier: " + identifier + " already exists.");
        // AccessDeniedException can be thrown, if current authentication is the anonymousUser
        return new SparqlConverter(identifier, body);
    }

    @Override
    protected void updateEntity(SparqlConverter filter, String body, Map<String, String> parameters, Map<String, String> headers) {
        if(!Strings.isNullOrEmpty(body) && !body.trim().isEmpty() && !body.trim().toLowerCase().equals("null") && !body.trim().toLowerCase().equals("empty")){
            filter.setQuery(body);
            filter.constructQuery();
        }
    }
}
