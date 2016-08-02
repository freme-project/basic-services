package eu.freme.common.persistence.dao;

import eu.freme.common.persistence.model.SparqlConverter;
import eu.freme.common.persistence.repository.SparqlConverterRepository;
import org.springframework.stereotype.Component;

/**
 * Created by Arne on 11.12.2015.
 */

@Component
public class SparqlConverterDAO extends OwnedResourceDAO<SparqlConverter> {

    @Override
    public String tableName() {
        return SparqlConverter.class.getSimpleName();
    }

    @Override
    public SparqlConverter findOneByIdentifierUnsecured(String identifier){
        return ((SparqlConverterRepository)repository).findOneByName(identifier);
    }

    @Override
    public String getIdentifierName(){
        return "name";
    }
}
