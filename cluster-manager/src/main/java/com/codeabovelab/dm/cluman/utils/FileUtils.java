package com.codeabovelab.dm.cluman.utils;

import com.codeabovelab.dm.common.utils.Throwables;

import javax.mail.internet.MimeUtility;
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
