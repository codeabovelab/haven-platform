/*
 * Copyright 2016 Code Above Lab LLC
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

package com.codeabovelab.dm.security.jpa;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.domain.Specifications;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.io.Serializable;

/**
 * a repository with custom filter
 */
public class FilteredJpaRepository<T, ID extends Serializable> extends SimpleJpaRepository<T, ID> {

    private FilterFactory filterFactory;
    
    public FilteredJpaRepository(JpaEntityInformation<T, ?> entityInformation, EntityManager entityManager) {
        super(entityInformation, entityManager);
    }

    public FilteredJpaRepository(Class<T> domainClass, EntityManager em) {
        super(domainClass, em);
    }
    
    void setFilterFactory(FilterFactory filterFactory) {
        this.filterFactory = filterFactory;
    }

    @Override
    protected TypedQuery<T> getQuery(Specification<T> spec, Sort sort) {
        return super.getQuery(appendFilter(spec), sort);
    }

    @Override
    protected TypedQuery<Long> getCountQuery(Specification<T> spec) {
        return super.getCountQuery(appendFilter(spec));
    }

    private Specification<T> appendFilter(Specification<T> spec) {
        Specification<T> filter = null;
        if(filterFactory != null) {
            filter = filterFactory.create(this);
        }
        if(filter == null) {
            return spec;
        }
        if(spec == null) {
            return filter;
        }
        return Specifications.where(spec).and(filter);
    }

}
