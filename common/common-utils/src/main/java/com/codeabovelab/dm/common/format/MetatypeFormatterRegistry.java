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

package com.codeabovelab.dm.common.format;

import com.codeabovelab.dm.common.utils.Key;
import org.springframework.format.Formatter;

/**
 * The registry which handle mapping for formatters for specific metatypes.
 */
public interface MetatypeFormatterRegistry {
    <T> void addFormatter(Key<T> key, Formatter<T> formatter);
    <T> Formatter<T> getFormatter(Key<T> key);
}
