package com.codeabovelab.dm.security.token;

import com.codeabovelab.dm.common.security.token.*;
import com.codeabovelab.dm.security.entity.Token;
import com.codeabovelab.dm.security.repository.TokenRepository;
import org.junit.Test;
import org.mockito.Matchers;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.security.crypto.codec.Hex;

import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class TokenServiceImplTest {

    static final String DEVICE_HASH = "123456677";
    static final String USER_NAME = "test";
    static final String TYPE_MOCK = "tm";
    static final String KEY = "key";
    static final long CREATION_TIME = 5678L;
    public static final long EXPIRE_AFTER = Long.MAX_VALUE / 2000L;
    public static final long EXPIRE_LAST_ACCESS = 60_000L;

    @Test
    public void test() {

        TokenConfiguration tokenConfiguration = newTokenConfiguration();
        TokenValidatorImpl validator = null;
        TokenService tokenService = mock(TokenService.class);
        {
            TokenData tokenData = mock(TokenData.class);
            when(tokenData.getCreationTime()).thenReturn(CREATION_TIME);
            when(tokenData.getDeviceHash()).thenReturn(DEVICE_HASH);
            when(tokenData.getKey()).thenReturn(KEY);
            when(tokenData.getType()).thenReturn(TYPE_MOCK);
            when(tokenData.getUserName()).thenReturn(USER_NAME);

            when(tokenService.createToken(tokenConfiguration)).thenReturn(tokenData);
            when(tokenService.getToken(KEY)).thenReturn(tokenData);

            validator = TokenValidatorImpl.builder().tokenService(tokenService)
                    .settings(initConfigs(EXPIRE_AFTER))
                    .cache(new ConcurrentMapCache("test"))
                    .build();
        }

        TokenData token = tokenService.createToken(tokenConfiguration);

        assertEquals(token, validator.verifyToken(token.getKey(), token.getDeviceHash()));
        assertEquals(token, validator.verifyToken(token.getKey()));

        validator = TokenValidatorImpl.builder().tokenService(tokenService)
                .settings(initConfigs(1))
                .cache(new ConcurrentMapCache("test"))
                .build();

        try {
            TokenData actual = validator.verifyToken(token.getKey());
            fail("Muken must be expired");
        } catch (TokenException e) {
            //all is ok
        }

    }

    private TokenValidatorSettings initConfigs(long expiration) {
        TokenValidatorSettings settings = new TokenValidatorSettings();
        settings.setExpireAfterInSec(expiration);
        settings.setExpireIfBothExpired(false);
        settings.setExpireLastAccessInSec(EXPIRE_LAST_ACCESS);

        return settings;
    }

    private TokenConfiguration newTokenConfiguration() {
        TokenConfiguration tokenConfiguration = new TokenConfiguration();
        tokenConfiguration.setDeviceHash(DEVICE_HASH);
        tokenConfiguration.setUserName(USER_NAME);
        return tokenConfiguration;
    }


    @Test
    public void testWithSignedBacked() {
        SignedTokenServiceBackend backend = new SignedTokenServiceBackend();
        backend.setSecureRandom(new SecureRandom());
        backend.setServerSecret("s3rc3t");
        backend.setServerInteger(34281239);
        testBody(backend);
    }

    @Test
    public void testWithPersistBacked() {
        PersistedTokenServiceBackend backend = new PersistedTokenServiceBackend();
        backend.setSecureRandom(new SecureRandom());
        backend.setSecret(new SecretKeySpec(Hex.decode("73982cf50f66253392d66a303c25299576a93ca1"), "none"));
        backend.setMacAlgorithm("HmacSHA1");
        TokenRepository repository = mock(TokenRepository.class);
        final Map<String, Object> map = new HashMap<>();
        when(repository.save(Matchers.<Token>any())).then(invocationOnMock -> {
            Token token = invocationOnMock.getArgumentAt(0, Token.class);
            map.put(token.getToken(), token);
            return null;
        });
        when(repository.findByToken(anyString())).then(invocationOnMock -> {
            String token = invocationOnMock.getArgumentAt(0, String.class);
            return map.get(token);
        });
        backend.setTokenRepository(repository);
        testBody(backend);
    }

    private void testBody(TokenService backend) {
        final TokenValidatorImpl tokenService = TokenValidatorImpl.builder().tokenService(backend)
                .settings(initConfigs(EXPIRE_AFTER))
                .cache(new ConcurrentMapCache("test"))
                .build();

        TokenConfiguration tokenConfiguration = newTokenConfiguration();
        TokenData token = backend.createToken(tokenConfiguration);
        assertNotNull(token);
        assertEquals(tokenConfiguration.getUserName(), token.getUserName());
        assertEquals(tokenConfiguration.getDeviceHash(), token.getDeviceHash());

        System.out.println("token:\n" + token.getKey());

        TokenData verifiedToken = tokenService.verifyToken(token.getKey(), token.getDeviceHash());
        assertEquals(token, verifiedToken);
    }

}