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

import com.codeabovelab.dm.common.metatype.MetatypeInfo;
import org.springframework.format.Formatter;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

/**
 * Formatter which can format phone number from common string representation, like '+199 000 00-00-000'.
 */
@FormatterInfo(metatypes = @MetatypeInfo(name = CommonMetatypeKeys.NAME_PHONE, type = Long.class))
public class PhoneFormatter implements Formatter<Long> {

    // due to http://en.wikipedia.org/wiki/E.164 we expect max 15 digits
    private static final long MAX_PHONE = 99999_99999_99999L;
    // array of chars which is allow in phone number
    private static final boolean allowedchars[] = new boolean[128];
    static {
        allowedchars[' '] =
          allowedchars['-'] =
          allowedchars['('] =
          allowedchars[')'] =
          allowedchars['+'] =
            true;
    }
    private final NumberFormat numberFormat = new DecimalFormat();

    @Override
    public Long parse(String text, Locale locale) throws ParseException {
        int pos = 0;
        char c;
        long res = 0;
        while(pos < text.length()) {
            c = text.charAt(pos);
            pos++;
            if(c < 127 && allowedchars[c]) {
                continue;
            }
            if(Character.isDigit(c)) {
                res = res * 10L + (long)(c - '0');
            } else {
                throw new FormatterException("Unexpected char at " + (pos - 1) + " in: " + text);
            }
            checkBounds(res);
        }
        return res;
    }

    @Override
    public String print(Long object, Locale locale) {
        if(object == null) {
            throw new FormatterException("Value is null.");
        }
        long number = object;
        checkBounds(number);
        return numberFormat.format(number);
    }

    private void checkBounds(long number) {
        if(number < 0L || number > MAX_PHONE) {
            throw new FormatterException("Value is out of bounds (0:" + MAX_PHONE + ").");
        }
    }
}
