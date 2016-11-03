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

package com.codeabovelab.dm.security.token;

import com.codeabovelab.dm.common.cache.DefineCache;
import com.codeabovelab.dm.common.security.token.*;
import com.codeabovelab.dm.security.entity.Token;
import com.codeabovelab.dm.security.repository.TokenRepository;
import org.apache.commons.codec.binary.Base32;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.transaction.Transactional;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.SecureRandom;
import java.util.Date;

/**
 * The backend which store tokens in database.
 */
@Transactional
public class PersistedTokenServiceBackend implements TokenService {

    static final String TYPE = "pt";

    private SecureRandom secureRandom;
    private TokenRepository tokenRepository;
    private String macAlgorithm;
    private java.security.Key secret;
    private final Base32 base32 = new Base32();

    public SecureRandom getSecureRandom() {
        return secureRandom;
    }

    public void setSecureRandom(SecureRandom secureRandom) {
        this.secureRandom = secureRandom;
    }

    public TokenRepository getTokenRepository() {
        return tokenRepository;
    }

    public void setTokenRepository(TokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    /**
     * Algorithm for HMAC generation. <p/>
     * See {@link javax.crypto.Mac#getInstance(String)} for info.
     * @return
     */
    public String getMacAlgorithm() {
        return macAlgorithm;
    }

    /**
     * Algorithm for HMAC generation. <p/>
     * See {@link javax.crypto.Mac#getInstance(String)} for info.
     * @param macAlgorithm
     */
    public void setMacAlgorithm(String macAlgorithm) {
        this.macAlgorithm = macAlgorithm;
    }

    public Key getSecret() {
        return secret;
    }

    public void setSecret(Key secret) {
        this.secret = secret;
    }

    @Cacheable("tokenData")
    @DefineCache(expireAfterWrite = 1000)
    @Override
    public TokenData createToken(TokenConfiguration config) {
        final String deviceHash = config.getDeviceHash();
        final String userName = config.getUserName();
        Assert.hasText(userName, "userName is nul or empty");
        final Date creationDate = new Date();

        Token token = new Token();
        token.setDeviceHash(deviceHash);
        token.setUserName(userName);
        token.setCreationDate(creationDate);
        final long time = creationDate.getTime();
        String key = createKey(time, userName, deviceHash);
        token.setToken(key);
        revokePreviouslyObtainedTokens(config);
        tokenRepository.save(token);
        return new TokenDataImpl(time, TokenUtils.getKeyWithTypeAndToken(TYPE, key), userName, deviceHash);
    }

    private void revokePreviouslyObtainedTokens(TokenConfiguration config) {
        String userName = config.getUserName();
        String deviceHash = config.getDeviceHash();
        if(!StringUtils.isEmpty(deviceHash)) {
            tokenRepository.deleteByUserNameAndDeviceHash(userName, deviceHash);
        }
    }

    private String createKey(long time, String userName, String deviceHash) {
        try {
            //use mac instead simply random data or hash
            Mac mac = Mac.getInstance(macAlgorithm);
            mac.init(secret);
            mac.update(ByteBuffer.allocate(8).putLong(time).array());
            mac.update(userName.getBytes());
            if(deviceHash != null) {
                mac.update(deviceHash.getBytes());
            }
            byte[] random = new byte[mac.getMacLength()];
            secureRandom.nextBytes(random);
            mac.update(random);
            return base32.encodeToString(mac.doFinal());
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("On alg: " + macAlgorithm, e);
        }
    }

    @Override
    @Cacheable("tokenData")
    @DefineCache(expireAfterWrite = 1000)
    public TokenData getToken(String key) {
        String tokenString = TokenUtils.getTokenFromKey(key);
        Token token = tokenRepository.findByToken(tokenString);
        if(token == null) {
            throw new TokenException("No persisted data for: " + tokenString);
        }
        return new TokenDataImpl(token.getCreationDate().getTime(),
                TokenUtils.getKeyWithTypeAndToken(TYPE, token.getToken()),
                token.getUserName(),
                token.getDeviceHash());
    }

    @Override
    @CacheEvict("tokenData")
    @DefineCache(expireAfterWrite = 1000)
    public void removeToken(String token) {
        tokenRepository.deleteByToken(TokenUtils.getTokenFromKey(token));
    }

    @Override
    @CacheEvict(value = "tokenData", allEntries = true)
    public void removeUserTokens(String userName) {
        tokenRepository.deleteByUserName(userName);
    }
}
