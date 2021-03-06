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
package eu.freme.bservices.controllers.users;

import eu.freme.common.persistence.model.User;

import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Collection;

/**
 * @author Jonathan Sauder jsauder@campus.tu-berlin.de
 */
@Component
public class UserAccessDecisionVoter implements AccessDecisionVoter<Object> {

    @Override
    public boolean supports(ConfigAttribute attribute) {
        return true;
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return clazz == User.class;
    }

    @Override
    public int vote(Authentication authentication, Object object,
                    Collection<ConfigAttribute> attributes) {
        try {
            User user = (User) object;

            //temporary
            //System.out.println("Successfully casted from Object to User");

            if (authentication.getPrincipal().equals("anonymousUser")) {
                return ACCESS_DENIED;
            }

            User authenticatedUser = (User) authentication.getPrincipal();

            if (authenticatedUser.getRole().equals(User.roleAdmin)) {
                return ACCESS_GRANTED;
            } else if (authenticatedUser.getName().equals(user.getName())) {
                return ACCESS_GRANTED;
            } else {
                return ACCESS_DENIED;
            }
        } catch (ClassCastException e) {
            //temporary
            //System.out.println("Handled ClassCastException from some Object to User");
            return ACCESS_ABSTAIN;
        }
    }
}