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

import com.codeabovelab.dm.common.utils.Callback;
import com.codeabovelab.dm.common.utils.Callbacks;
import com.codeabovelab.dm.common.utils.Key;
import org.springframework.format.Formatter;
import org.springframework.util.Assert;

import java.text.ParseException;
import java.util.Locale;
import java.util.Set;

/**
 * A wrapper which allow combine formatter with validation callbacks. <p/>
 * Validation callbacks will be invoked on parse and on print.
 */
public class ValidableFormatter<T> implements Formatter<T>, SelfDescribedFormatter<T> {

    private final Formatter<T> formatter;
    private final Callback<T> objectValidator;
    private final Callback<String> textValidator;

    /**
     *
     * @param formatter
     * @param objectValidator the callback in which do object validation after parsing and before printing
     * @param textValidator the callback in which do text validation before parsing and after printing
     */
    public ValidableFormatter(Formatter<T> formatter, Callback<T> objectValidator, Callback<String> textValidator) {
        this.formatter = formatter;
        Assert.notNull(this.formatter, "formatter is null");
        this.objectValidator = objectValidator;
        this.textValidator = textValidator;
        Assert.isTrue(this.objectValidator != null || this.textValidator != null, "text and object validators is null");
    }

    @Override
    public Set<Key<?>> getHandledMetatypes() {
        return FormatterUtils.getHandledMetatypes(this.formatter);
    }

    @Override
    public T parse(String text, Locale locale) throws ParseException {
        Callbacks.call(this.textValidator, text);
        T object = formatter.parse(text, locale);
        Callbacks.call(this.objectValidator, object);
        return object;
    }

    @Override
    public String print(T object, Locale locale) {
        Callbacks.call(this.objectValidator, object);
        String text = formatter.print(object, locale);
        Callbacks.call(this.textValidator, text);
        return text;
    }
}
