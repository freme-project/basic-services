package eu.freme.common.persistence.repository;

import eu.freme.common.persistence.model.XsltConverter;

/**
 * Created by Arne on 11.12.2015.
 */

public interface XsltConverterRepository extends OwnedResourceRepository<XsltConverter> {
    XsltConverter findOneByName(String name);
}
