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

import org.apache.log4j.Appender;
import org.apache.log4j.Logger;
import org.apache.log4j.filter.ExpressionFilter;
import org.springframework.stereotype.Component;

import java.util.Enumeration;

/**
 * Created by Arne Binder (arne.b.binder@gmail.com) on 06.01.2016.
 */
@Component
public class LoggingHelper {

    public static final String accessDeniedExceptions = "eu.freme.common.exception.AccessDeniedException || EXCEPTION ~=org.springframework.security.access.AccessDeniedException";
    public static final String ownedResourceNotFoundException = "eu.freme.common.exception.OwnedResourceNotFoundException";

    /**
     * Sets specific LoggingFilters and initiates suppression of specified Exceptions in Log4j.
     * @param x Class of Exception
     **/
    public static void loggerIgnore(Class<Throwable> x){
        loggerIgnore(x.getName());
    }

    /**
     * Sets specific LoggingFilters and initiates suppression of specified Exceptions in Log4j.
     * @param x String name of Exception
     **/
    public static void loggerIgnore(String x) {

        String newExpression="EXCEPTION ~="+x;
        Enumeration<Appender> allAppenders= Logger.getRootLogger().getAllAppenders();
        Appender appender;

        while (allAppenders.hasMoreElements()) {
            appender=allAppenders.nextElement();
            String oldExpression;
            ExpressionFilter exp;
            try {
                exp = ((ExpressionFilter) appender.getFilter());
                oldExpression = exp.getExpression();
                if (!oldExpression.contains(newExpression)) {
                    exp.setExpression(oldExpression + " || " + newExpression);
                    exp.activateOptions();
                }
            } catch (NullPointerException e) {
                exp = new ExpressionFilter();
                exp.setExpression(newExpression);
                exp.setAcceptOnMatch(false);
                exp.activateOptions();
                appender.clearFilters();
                appender.addFilter(exp);
            }
        }
    }

    /**
     * Clears specific LoggingFilters and stops their suppression of Exceptions in Log4j.
     * @param x Class of Exception
     **/
    public static void loggerUnignore(Class<Throwable> x) {
        loggerUnignore(x.getName());
    }

    /**
     * Clears specific LoggingFilters and stops their suppression of Exceptions in Log4j.
     * @param x String name of Exception
     **/
    public static void loggerUnignore(String x) {
        Enumeration<Appender> allAppenders= Logger.getRootLogger().getAllAppenders();
        Appender appender;

        while (allAppenders.hasMoreElements()) {
            appender=allAppenders.nextElement();
            ExpressionFilter exp = ((ExpressionFilter) appender.getFilter());
            exp.setExpression(exp.getExpression().replace("|| EXCEPTION ~=" + x, "").replace("EXCEPTION ~=" + x + "||", ""));
            exp.activateOptions();
            appender.clearFilters();
            appender.addFilter(exp);
        }
    }

    /**
     * Clears all LoggingFilters for all Appenders. Stops suppression of Exceptions in Log4j.
     **/
    public static void loggerClearFilters() {
        Enumeration<Appender> allAppenders = Logger.getRootLogger().getAllAppenders();
        Appender appender;

        while (allAppenders.hasMoreElements()) {
            appender = allAppenders.nextElement();
            appender.clearFilters();
        }
    }
}
