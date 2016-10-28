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
package eu.freme.bservices.controllers.pipelines.core;

import eu.freme.common.exception.AdditionalFieldsException;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Created by Arne Binder (arne.b.binder@gmail.com) on 22.08.2016.
 */

@SuppressWarnings("serial")
@ResponseStatus(value = HttpStatus.BAD_GATEWAY)
public class PipelineFailedException extends AdditionalFieldsException {

    public PipelineFailedException(JSONObject nestedException, String msg, HttpStatus httpStatusCode){
        super(msg, httpStatusCode);
        addAdditionalField("nested-exception", nestedException);
    }
}
