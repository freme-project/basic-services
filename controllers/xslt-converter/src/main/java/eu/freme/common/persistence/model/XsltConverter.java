package eu.freme.common.persistence.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import net.sf.saxon.s9api.*;

import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Transient;
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

    @Lob
    String stylesheet;

    String name;

    public XsltConverter(){super(null);}

    public XsltConverter(String name, String stylesheetString){
        super();
        this.name = name;
        this.stylesheet = stylesheetString;
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
    }
}
