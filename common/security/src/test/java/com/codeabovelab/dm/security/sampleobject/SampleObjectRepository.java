package com.codeabovelab.dm.security.sampleobject;

import org.springframework.data.repository.CrudRepository;
import org.springframework.security.access.method.P;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;

public interface SampleObjectRepository extends CrudRepository<SampleObject, Long> {

    @Override
    @PostFilter("hasPermission(filterObject, 'READ')")
    Iterable<SampleObject> findAll();

    @Override
    @PreAuthorize("hasPermission(#id, 'com.codeabovelab.dm.security.sampleobject.SampleObject', 'DELETE')")
    void delete(@P("id") Long id);
    
    @Override
    @PreAuthorize("hasRole('ROLE_USER')")
    <S extends SampleObject> S save(S object);
}
