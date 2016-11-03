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

package com.codeabovelab.dm.platform.security;

import org.springframework.security.crypto.encrypt.BytesEncryptor;
import org.springframework.security.crypto.encrypt.TextEncryptor;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Base64 warpper on {@link BytesEncryptor }
 */
public class Base64Encryptor implements TextEncryptor {
    private final BytesEncryptor encryptor;

    public Base64Encryptor(BytesEncryptor encryptor) {
        this.encryptor = encryptor;
    }

    @Override
    public String encrypt(String text) {
        byte[] encrypt = encryptor.encrypt(text.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encrypt);
    }

    @Override
    public String decrypt(String encryptedText) {
        byte[] decode = Base64.getDecoder().decode(encryptedText);
        return new String(encryptor.decrypt(decode), StandardCharsets.UTF_8);
    }
}
