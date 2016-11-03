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

package com.codeabovelab.dm.common.kv.mapping;

import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.util.StringUtils;

/**
 */
public class PropertyCipher implements PropertyInterceptor {

    private final TextEncryptor encryptor;

    public PropertyCipher(TextEncryptor encryptor) {
        this.encryptor = encryptor;
    }

    @Override
    public String save(KvPropertyContext prop, String value) {
        if (StringUtils.hasText(value)) {
            return encryptor.encrypt(value);
        } else {
            return value;
        }
    }

    @Override
    public String read(KvPropertyContext prop, String value) {
        if (StringUtils.hasText(value)) {
            return encryptor.decrypt(value);
        } else {
            return value;
        }
    }
}
