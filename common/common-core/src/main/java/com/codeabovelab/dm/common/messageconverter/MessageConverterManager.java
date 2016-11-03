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

package com.codeabovelab.dm.common.messageconverter;

/**
 * Provides ability to convert any version of pojo to a latest or any requested version.
 * It composes chains of MessageConverter's. Used for up casting of commands and down casting of results.
 */
public interface MessageConverterManager {
    /**
     * Convert a message till the end of chain
     * @param message to convert
     * @return converted message
     */
    <T> T convertToNewest(Object message, Class<T> resultType);

    /**
     * Convert a message to a specified version
     * @param message to convert
     * @return converted message
     * @throws RuntimeException if a chain of converter for conversion does not exists
     */
    <T> T convert(Object message, Class<T> toType);
}
