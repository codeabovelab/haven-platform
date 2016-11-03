package com.codeabovelab.dm.gateway.proxy.common;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 * Created by pronto on 1/18/16.
 */
public class HttpProxyTest {

    @Test
    public void testCookies() {

        String realCookie = HttpProxy.getRealCookie("" +
                "!Proxy!JSESSIONID=AE94F00CA87DB8576DBC00F824819F93;" +
                "!Proxy!JSESSIONID=AE94F00CA87DB8576DBC00F824819F93;" +
                "!Proxy!JSESSIONID=400C1F87D505AD2C04541B72F1DEA57E;");
        assertNotNull(realCookie);
    }

}