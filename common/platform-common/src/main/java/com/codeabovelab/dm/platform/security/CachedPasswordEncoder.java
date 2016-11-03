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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.codeabovelab.dm.common.utils.Throwables;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.concurrent.ExecutionException;


/**
 * Attention. Usage of this class is insecure, because it holding 'passwords->hash' pairs in memory. But in some
 * cases we need very often calculate password hashes, therefore we use this wrapper.
 */
public class CachedPasswordEncoder implements PasswordEncoder {
    private final LoadingCache<String, String> encoderCache;
    private final LoadingCache<MatcherKey, Boolean> matcherCache;


    public CachedPasswordEncoder(final PasswordEncoder passwordEncoder, final CacheBuilder<Object, Object> cacheBuilder) {
        CacheLoader<String, String> encoderLoader = new CacheLoader<String, String>() {
            @Override
            public String load(String key) throws Exception {
                return passwordEncoder.encode(key);
            }
        };
        this.encoderCache = cacheBuilder.build(encoderLoader);

        CacheLoader<MatcherKey, Boolean> matcherLoader = new CacheLoader<MatcherKey, Boolean>() {
            @Override
            public Boolean load(MatcherKey key) throws Exception {
                return passwordEncoder.matches(key.pass, key.encoded);
            }
        };

        this.matcherCache = cacheBuilder.build(matcherLoader);
    }

    @Override
    public String encode(CharSequence rawPassword) {
        try {
            return encoderCache.get(rawPassword.toString());
        } catch (ExecutionException e) {
            throw Throwables.asRuntime(e);
        }
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        try {
            return matcherCache.get(new MatcherKey(rawPassword.toString(), encodedPassword));
        } catch (ExecutionException e) {
            throw Throwables.asRuntime(e);
        }
    }

    static final class MatcherKey {
        final String pass;
        final String encoded;

        MatcherKey(String pass, String encoded) {
            this.pass = pass;
            this.encoded = encoded;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof MatcherKey)) {
                return false;
            }

            MatcherKey that = (MatcherKey) o;

            if (encoded != null ? !encoded.equals(that.encoded) : that.encoded != null) {
                return false;
            }
            if (pass != null ? !pass.equals(that.pass) : that.pass != null) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = pass != null ? pass.hashCode() : 0;
            result = 31 * result + (encoded != null ? encoded.hashCode() : 0);
            return result;
        }
    }
}
