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

package com.codeabovelab.dm.cluman.ui.model;

import com.codeabovelab.dm.cluman.model.ContainerBaseIface;

import java.util.Date;

/**
 * Contract for any UI container representation
 */
public interface UiContainerIface extends ContainerBaseIface {
    void setId(String id);

    void setName(String name);

    void setImage(String image);

    void setImageId(String imageId);

    void setRun(boolean lock);
    void setLock(boolean lock);

    void setLockCause(String lockCause);

    String getStatus();
    void setStatus(String status);

    Date getCreated();
    void setCreated(Date date);
}
