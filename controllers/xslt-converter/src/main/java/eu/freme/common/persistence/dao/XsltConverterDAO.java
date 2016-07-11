package eu.freme.common.persistence.dao;

import eu.freme.common.persistence.repository.XsltConverterRepository;
import eu.freme.common.persistence.model.XsltConverter;
import org.springframework.stereotype.Component;

/**
 * Created by Arne on 11.12.2015.
 */

@Component
public class XsltConverterDAO extends OwnedResourceDAO<XsltConverter> {

    @Override
    public String tableName() {
        return XsltConverter.class.getSimpleName();
    }

    @Override
    public XsltConverter findOneByIdentifierUnsecured(String identifier){
        return ((XsltConverterRepository)repository).findOneByName(identifier);
    }

}
