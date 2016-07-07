package eu.freme.common.persistence.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import eu.freme.common.exception.BadRequestException;
import net.sf.saxon.s9api.*;
import org.apache.log4j.Logger;

import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.StringReader;

/**
 * Created by Arne on 11.12.2015.
 */

@Entity
@Table(name = "xsltconverter")
public class XsltConverter extends OwnedResource {

    @JsonIgnore
    @Transient
    Logger logger = Logger.getLogger(XsltConverter.class);

    @JsonIgnore
    @Transient
    Xslt30Transformer transformer;

    @JsonIgnore
    @Transient
    Processor processor = new Processor(false);

    @Lob
    String stylesheet;

    String name;

    public XsltConverter(){super(null);}

    public XsltConverter(String name, String stylesheetString){
        super();
        this.name = name;
        this.stylesheet = stylesheetString;
        try {
            // compile the stylesheet
            getTransformer();
        } catch (Exception e) {
            throw new BadRequestException("Can not compile the stylesheet: "+name+". "+e +": "+e.getLocalizedMessage());
        }
    }

    @JsonIgnore
    @Override
    public String getIdentifier(){
        return getName();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStylesheet() {
        return stylesheet;
    }

    public void setStylesheet(String stylesheet) {
        this.stylesheet = stylesheet;
        transformer = null;
        try {
            // compile the stylesheet
            getTransformer();
        } catch (Exception e) {
            throw new BadRequestException("Can not compile the stylesheet: "+name+". "+e +": "+e.getLocalizedMessage());
        }
    }

    @JsonIgnore
    @Transient
    private ErrorListener saxonListener = new ErrorListener() {
        public void error(TransformerException exception) throws TransformerException {
            logger.error(exception.getMessageAndLocation());
        }

        public void fatalError(TransformerException exception) throws TransformerException {
            logger.error(exception.getMessageAndLocation());
            throw exception;
        }

        public void warning(TransformerException exception) throws TransformerException {
            logger.warn(exception);
        }
    };

    @JsonIgnore
    public Xslt30Transformer getTransformer() throws SaxonApiException {
        if(transformer==null) {
            XsltCompiler compiler = processor.newXsltCompiler();
            compiler.setErrorListener(saxonListener);
            transformer = compiler.compile(new StreamSource(new StringReader(stylesheet))).load30();
            transformer.setErrorListener(saxonListener);
        }
        return transformer;
    }

    @JsonIgnore
    public Serializer newSerializer(){
        return processor.newSerializer();
    }
}
