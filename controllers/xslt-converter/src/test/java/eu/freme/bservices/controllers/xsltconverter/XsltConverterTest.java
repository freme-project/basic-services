package eu.freme.bservices.controllers.xsltconverter;

import com.mashape.unirest.http.exceptions.UnirestException;
import eu.freme.bservices.testhelper.AuthenticatedTestHelper;
import eu.freme.bservices.testhelper.OwnedResourceManagingHelper;
import eu.freme.bservices.testhelper.SimpleEntityRequest;
import eu.freme.bservices.testhelper.api.IntegrationTestSetup;
import eu.freme.common.persistence.dao.UserDAO;
import eu.freme.common.persistence.dao.XsltConverterDAO;
import eu.freme.common.persistence.model.XsltConverter;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

import java.io.IOException;

/**
 * Created by Arne Binder (arne.b.binder@gmail.com) on 19.07.2016.
 */
public class XsltConverterTest {
    final static String serviceUrl = "/toolbox/xslt-converter/manage";

    private Logger logger = Logger.getLogger(XsltConverterTest.class);
    AuthenticatedTestHelper ath;
    OwnedResourceManagingHelper<XsltConverter> ormh;

    private XsltConverterDAO xsltConverterDAO;
    private UserDAO userDAO;

    public XsltConverterTest() throws UnirestException, IOException {
        ApplicationContext context = IntegrationTestSetup.getContext("xslt-converter-test-package.xml");
        ath = context.getBean(AuthenticatedTestHelper.class);
        ormh = new OwnedResourceManagingHelper<>(serviceUrl, XsltConverter.class, ath);
        ath.authenticateUsers();
        xsltConverterDAO = context.getBean(XsltConverterDAO.class);
        userDAO = context.getBean(UserDAO.class);
    }


    static final String xsltIdentityTransform = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\"\n" +
            "    xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"\n" +
            "    exclude-result-prefixes=\"xs\"\n" +
            "    version=\"2.0\">\n" +
            "    <xsl:template match=\"node()|@*\">\n" +
            "        <xsl:copy>\n" +
            "            <xsl:apply-templates select=\"node()|@*\"></xsl:apply-templates>\n" +
            "        </xsl:copy>\n" +
            "    </xsl:template>\n" +
            "</xsl:stylesheet>";

    static final String xsltWithParam = "<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"1.0\">\n" +
            "    <xsl:param name=\"myparam\">set internally</xsl:param>\n" +
            "    <xsl:template match=\"/\">\n" +
            "        <xsl:value-of select=\"$myparam\"/>\n" +
            "    </xsl:template>\n" +
            "</xsl:stylesheet>";

    @Test
    public void testSparqlConverterManagement() throws UnirestException, IOException {
        String createdXsltName = "identity-transform";
        SimpleEntityRequest request = new SimpleEntityRequest(xsltIdentityTransform)
                .putParameter(xsltConverterDAO.getIdentifierName(), createdXsltName);
        SimpleEntityRequest updateRequest = new SimpleEntityRequest(xsltWithParam);
        XsltConverter expectedCreatedEntity = new XsltConverter();
        expectedCreatedEntity.setName(createdXsltName);
        expectedCreatedEntity.setStylesheet(xsltIdentityTransform);
        XsltConverter expectedUpdatedEntity = new XsltConverter();
        expectedUpdatedEntity.setName(createdXsltName);
        expectedUpdatedEntity.setStylesheet(xsltWithParam);

        ormh.checkCRUDOperations(request, updateRequest, expectedCreatedEntity, expectedUpdatedEntity, "xxxx");
    }
}
