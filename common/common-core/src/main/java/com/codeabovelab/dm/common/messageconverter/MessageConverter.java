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
 * Common interface for Command and Command.Result conversion from one version to another
 */
public interface MessageConverter<FROM, TO> {
    /**
     *
     * @param pojo - any version of command or result
     * @return newer(up casting) version of command or previous (down casting) version of result
     */
    TO convert(FROM pojo);

    /**
     * @return Class of source POJO which converter can convert
     */
    Class<FROM> getFrom();

    /**
     * @return Class of converted POJO
     */
    Class<TO> getTo();
}
