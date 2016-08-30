package eu.freme.common.persistence.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.rdf.model.Model;
import eu.freme.common.exception.FREMEHttpException;
import org.springframework.stereotype.Component;

import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Transient;

/**
 * Created by Arne on 11.12.2015.
 */

@Entity
@Table(name = "sparqlconverter")
public class SparqlConverter extends OwnedResource {

    @JsonIgnore
    @Transient
    Query jenaQuery;

    @Lob
    String query;
    String name;

    public SparqlConverter(){super(null);}

    public SparqlConverter(String name, String queryString){
        super();
        this.name = name;
        this.query = queryString;
    }

    public QueryExecution getFilteredModel(Model model){
        if(jenaQuery==null)
            constructQuery();
        if(jenaQuery.isConstructType()) {
            return QueryExecutionFactory.create(jenaQuery, model);
        }else
            throw new FREMEHttpException("The executed query does not return a RDF graph. Current Jena query type: "+jenaQuery.getQueryType()+", see https://jena.apache.org/documentation/javadoc/arq/constant-values.html section org.apache.jena.query.Query");
    }

    public QueryExecution getFilteredResultSet(Model model){
        if(jenaQuery==null)
            constructQuery();
        if(jenaQuery.isSelectType()) {
            return QueryExecutionFactory.create(jenaQuery, model);
        }else
            throw new FREMEHttpException("The executed query does not return a set of tuples. Current Jena query type: "+jenaQuery.getQueryType()+", see https://jena.apache.org/documentation/javadoc/arq/constant-values.html section org.apache.jena.query.Query");
    }

    @JsonIgnore
    public int getQueryType(){
        if(jenaQuery==null)
            constructQuery();
        return jenaQuery.getQueryType();
    }

    @JsonIgnore
    @Override
    public String getIdentifier(){
        return getName();
    }

    public void constructQuery(){
        this.jenaQuery = QueryFactory.create(query);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String queryString) {
        this.query = queryString;
        this.jenaQuery = null;
    }
}
