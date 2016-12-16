package com.codeabovelab.dm.common.utils;

import com.sun.xml.internal.messaging.saaj.packaging.mime.internet.MimeUtility;

import java.io.UnsupportedEncodingException;

public class FileUtils {

    /**
     * Encode specified value as RFC 2047
     * @param name
     * @return
     * @throws Exception
     */
    public static String encode(String name) {
        try {
            return MimeUtility.encodeText(name, "UTF-8", null);
        } catch (UnsupportedEncodingException e) {
            throw Throwables.asRuntime(e);
        }
    }
}
