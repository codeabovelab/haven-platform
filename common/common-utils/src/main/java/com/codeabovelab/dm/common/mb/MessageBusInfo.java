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

package com.codeabovelab.dm.common.mb;

/**
 * Info part of message bus inface.
 */
public interface MessageBusInfo<M> {
    /**
     * Id of bus. It unique string which help to identity bust in app environment. <p/>
     * Can be used in error handlers.
     * @return
     */
    String getId();

    /**
     * Type of buss messages.
     * @return
     */
    Class<M> getType();
}
