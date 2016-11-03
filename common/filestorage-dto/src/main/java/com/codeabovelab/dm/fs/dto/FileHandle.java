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

package com.codeabovelab.dm.fs.dto;

import java.io.InputStream;
import java.util.Map;

/**
 * Handler for files. Hold file data and any file attributes.
 */
public interface FileHandle {
    /**
     * Name of file.
     */
    String ATTR_NAME  = "name";
    /**
     * MIME type.
     */
    String ATTR_MIME_TYPE = "mimeType";
    /**
     * Creation date, UTC milliseconds from epoch.
     */
    String ATTR_DATE_CREATION = "dateCreation";
    /**
     * Id of user which create this file.
     */
    String ATTR_CREATOR_ID = "creatorId";

    /**
     * UUID of file
     * @return
     */
    String getId();

    /**
     * Stream with data of file.
     * @return stream or null in cases when current object represent patch for updating of attributes
     */
    InputStream getData();

    /**
     * Attributes of file. Attribute keys case insensitive.
     * @see #ATTR_NAME
     * @see #ATTR_DATE_CREATION
     * @see #ATTR_CREATOR_ID
     * @return
     */
    Map<String, String> getAttributes();
}
