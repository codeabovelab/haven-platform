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

package com.codeabovelab.dm.security.acl;

import com.codeabovelab.dm.security.entity.ClassEntity;
import com.codeabovelab.dm.security.repository.ClassesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;

/**
 * service which prove some AclClasses code
 */
@Component 
@Transactional
class ClassesService {
    private final ClassesRepository classesRepository;

    @Autowired
    ClassesService(ClassesRepository classesRepository) {
        this.classesRepository = classesRepository;
    }
    
    ClassEntity getOrCreate(String type) {
        ClassEntity entity = classesRepository.findByClassName(type);
        if(entity == null) {
            entity = new ClassEntity();
            entity.setClassName(type);
            entity = classesRepository.save(entity);
        }
        return entity;
    }
}
