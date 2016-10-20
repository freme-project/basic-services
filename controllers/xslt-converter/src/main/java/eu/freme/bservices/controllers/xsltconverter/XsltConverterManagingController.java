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
package eu.freme.bservices.controllers.xsltconverter;


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
